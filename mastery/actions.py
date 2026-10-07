"""What users can change: assignments, activities, ratings, boosts, abuse reports, subscriptions.

Every function takes the viewer first and raises ActionError if the viewer may not
do it or the input is not valid.
"""
import datetime

from django.conf import settings
from django.db import transaction
from django.db.models import F
from django.utils import timezone

from mastery import balancing, media, notifications, presenter, sanitizer, search
from mastery.models import (
    AbuseReport, Achievement, Activity, Assignment, ContentBlock, LicenseType, Person, Rating, Resource,
    Reward, Skill, Subscription)

# Reward of assignments that the system user creates
SYSTEM_REWARD = 25

DEFAULT_PICTURE = "static/default-skill.png"
SITE_NAME = "busyhumans.com"


class ActionError(Exception):
    def __init__(self, message="An error occured. Please try again later."):
        super().__init__(message)
        self.message = message


def require_person(viewer):
    if viewer.person is None or viewer.needs_tos_confirmation:
        raise ActionError("Please login.")
    return viewer.person


def require_admin(viewer):
    if not viewer.is_admin or viewer.needs_tos_confirmation:
        raise ActionError("Please login as admin.")


def find(model, id, viewer=None, filtered=True):
    """Loads an entity that is not deleted and, if filtered, not hidden from the viewer."""
    entity = model.objects.filter(id=id, deleted=False).first() if id else None
    if entity is None or (filtered and presenter.hidden(entity, viewer)):
        raise ActionError()
    return entity


def default_picture():
    return Resource(
        resource=DEFAULT_PICTURE, small=DEFAULT_PICTURE, medium=DEFAULT_PICTURE, large=DEFAULT_PICTURE,
        author_name=SITE_NAME, author_url=settings.BASE_URL, license=LicenseType.INTERNAL_PICTURE)


def uploaded_picture(picture_id, person):
    """Resource for a picture that the person uploaded. It is published under CC BY."""
    path = "mastery/res/dynamic/%s" % picture_id
    return Resource(
        resource=path, small=path + "/small", medium=path + "/medium", large=path + "/large", hires=path + "/hires",
        author_name=person.name, author_url="#person=%s" % person.id, license=LicenseType.CC_BY_30)


def check_upload(picture_id, uploads):
    """Returns the id if it is a picture that this session uploaded."""
    if not picture_id or str(picture_id) not in uploads or not media.upload_exists(picture_id):
        raise ActionError("The picture was not found. Please upload it again.")
    return picture_id


#
# abuse reports
#

def _set_abuse_report(viewer, entity, field, abuse):
    person = require_person(viewer)
    if getattr(entity, "author_id", None) == person.id:
        raise ActionError()
    report = AbuseReport.objects.filter(deleted=False, author=person, **{field: entity}).first()
    if abuse and report is None:
        AbuseReport.objects.create(author=person, timestamp=timezone.now(), **{field: entity})
        type(entity).objects.filter(id=entity.id).update(abuse_reports_count=F("abuse_reports_count") + 1)
    elif not abuse and report is not None:
        AbuseReport.objects.filter(id=report.id).update(deleted=True)
        type(entity).objects.filter(id=entity.id).update(abuse_reports_count=F("abuse_reports_count") - 1)


def _clear_abuse_reports(viewer, entity, field):
    require_admin(viewer)
    AbuseReport.objects.filter(deleted=False, **{field: entity}).update(deleted=True)
    type(entity).objects.filter(id=entity.id).update(abuse_reports_count=0)


@transaction.atomic
def set_activity_abuse(viewer, activity_id, abuse):
    _set_abuse_report(viewer, find(Activity, activity_id, filtered=False), "activity", abuse)


@transaction.atomic
def clear_activity_abuse(viewer, activity_id):
    _clear_abuse_reports(viewer, find(Activity, activity_id, filtered=False), "activity")


@transaction.atomic
def set_assignment_abuse(viewer, assignment_id, abuse):
    _set_abuse_report(viewer, find(Assignment, assignment_id, filtered=False), "assignment", abuse)


@transaction.atomic
def clear_assignment_abuse(viewer, assignment_id):
    _clear_abuse_reports(viewer, find(Assignment, assignment_id, filtered=False), "assignment")


@transaction.atomic
def set_skill_abuse(viewer, skill_id, abuse):
    _set_abuse_report(viewer, find(Skill, skill_id, filtered=False), "skill", abuse)


@transaction.atomic
def clear_skill_abuse(viewer, skill_id):
    _clear_abuse_reports(viewer, find(Skill, skill_id, filtered=False), "skill")


#
# activities
#

@transaction.atomic
def rate(viewer, activity_id, value):
    """Likes (1) or dislikes (-1) an activity, or takes the rating back (0)."""
    person = require_person(viewer)
    activity = find(Activity, activity_id, viewer)
    Rating.objects.filter(activity=activity, author=person, deleted=False).update(deleted=True)
    if value != 0:
        Rating.objects.create(activity=activity, author=person, timestamp=timezone.now(), positive=value > 0)


def _delete_activity(activity):
    Activity.objects.filter(id=activity.id).update(deleted=True)
    remaining = Activity.objects.filter(assignment_id=activity.assignment_id, deleted=False)
    latest = remaining.order_by("-timestamp").values_list("timestamp", flat=True).first()
    Assignment.objects.filter(id=activity.assignment_id, deleted=False).update(last_activity=latest)
    if not Activity.objects.filter(achievement_id=activity.achievement_id, deleted=False).exists():
        Achievement.objects.filter(id=activity.achievement_id).update(deleted=True)


@transaction.atomic
def delete_activity(viewer, activity_id):
    """Authors may delete their activity until it is rewarded."""
    person = require_person(viewer)
    activity = find(Activity, activity_id, filtered=False)
    if not viewer.is_admin and (activity.author_id != person.id or activity.rewarded is not None):
        raise ActionError()
    _delete_activity(activity)


def video_block_resource(video_id):
    if not media.YOUTUBE_ID.match(video_id or ""):
        raise ActionError("This is not a YouTube video.")
    return Resource(resource=video_id, license=LicenseType.YOUTUBE)


@transaction.atomic
def create_activity(viewer, assignment_id, text, blocks, uploads):
    """Posts the proof that the viewer did what the assignment asks for.

    blocks: [{"typ": 1, "value": id of an uploaded picture} | {"typ": 2, "value": id of a YouTube video}]
    uploads: ids of the pictures that the session uploaded
    """
    person = require_person(viewer)
    text = sanitizer.activity_text(text or "")
    if text is None:
        raise ActionError("The text is too long.")

    values = []
    if text:
        values.append((ContentBlock.Type.TEXT, True, Resource(
            resource=text, author_name=person.name, author_url="#person=%s" % person.id,
            license=LicenseType.INTERNAL_TEXT)))
    for block in (blocks or [])[:20]:
        if block.get("typ") == ContentBlock.Type.IMAGE:
            picture_id = check_upload(block.get("value"), uploads)
            values.append((ContentBlock.Type.IMAGE, True, uploaded_picture(picture_id, person)))
        elif block.get("typ") == ContentBlock.Type.VIDEO:
            values.append((ContentBlock.Type.VIDEO, False, video_block_resource(block.get("value"))))
        else:
            raise ActionError()
    if not values:
        raise ActionError("Please describe your activity with text, pictures or videos.")

    assignment = find(Assignment, assignment_id, viewer)
    if settings.FOUNDER_ASSIGNMENT_ID and str(assignment.id) == settings.FOUNDER_ASSIGNMENT_ID:
        # The founder assignment is completed by creating an own assignment
        raise ActionError()
    if Activity.objects.filter(assignment=assignment, author=person, deleted=False).exists():
        raise ActionError("You have already completed this assignment.")

    achievement = Achievement.objects.filter(deleted=False, owner=person, skill_id=assignment.skill_id).first()
    if achievement is None:
        if not Skill.objects.filter(id=assignment.skill_id, deleted=False).exists():
            raise ActionError()
        achievement = Achievement.objects.create(owner=person, skill_id=assignment.skill_id)

    now = timezone.now()
    activity = Activity.objects.create(
        author=person, timestamp=now, assignment=assignment, achievement=achievement,
        reward_due_date=now + datetime.timedelta(days=3))
    for position, (typ, authentic, resource) in enumerate(values):
        resource.save()
        ContentBlock.objects.create(
            activity=activity, position=position, typ=typ, value=resource, authentic=authentic)
        if typ == ContentBlock.Type.VIDEO:
            media.fetch_youtube_thumbnail(resource.resource)
    Assignment.objects.filter(id=assignment.id).update(last_activity=now)

    notifications.create_other_activity(person, assignment)
    return activity


#
# assignments
#

@transaction.atomic
def boost(viewer, assignment_id, value):
    """Raises (1) or lowers (-1) the reward of an assignment, or takes that back (0).

    The amount depends on the level of the person. Everybody has one boost per
    assignment; for the author it is the reward that the assignment started with.
    """
    person = require_person(viewer)
    assignment = find(Assignment, assignment_id, viewer)
    for existing in Reward.objects.filter(assignment=assignment, donating_person=person, deleted=False):
        Reward.objects.filter(id=existing.id).update(deleted=True)
        Assignment.objects.filter(id=assignment.id).update(rewards_sum=F("rewards_sum") - existing.amount)
    if value != 0:
        amount = balancing.get_initial_assignment_reward(balancing.get_level(person.xp))
        if amount <= 0:
            raise ActionError()
        amount = amount if value > 0 else -amount
        Reward.objects.create(assignment=assignment, donating_person=person, timestamp=timezone.now(), amount=amount)
        Assignment.objects.filter(id=assignment.id).update(rewards_sum=F("rewards_sum") + amount)


def _delete_assignment(assignment):
    assignment.deleted = True
    assignment.save(update_fields=["deleted"])
    Subscription.objects.filter(assignment=assignment, deleted=False).update(deleted=True)


@transaction.atomic
def delete_assignment(viewer, assignment_id):
    """Authors may delete their assignment as long as nobody completed it."""
    person = require_person(viewer)
    assignment = find(Assignment, assignment_id, filtered=False)
    if not viewer.is_admin:
        if assignment.author_id != person.id:
            raise ActionError()
        if Activity.objects.filter(assignment=assignment, deleted=False).exists():
            raise ActionError()
    _delete_assignment(assignment)


@transaction.atomic
def speedup(viewer, assignment_id, days):
    """Moves an assignment and its activities into the past, so that rewards are due earlier."""
    require_admin(viewer)
    assignment = find(Assignment, assignment_id, viewer)
    delta = datetime.timedelta(days=days)
    Assignment.objects.filter(id=assignment.id).update(creation_timestamp=F("creation_timestamp") - delta)
    Activity.objects.filter(assignment=assignment, deleted=False).update(
        timestamp=F("timestamp") - delta, reward_due_date=F("reward_due_date") - delta)


def subscribe_account(account, assignment_id):
    if not Subscription.objects.filter(deleted=False, account=account, assignment_id=assignment_id).exists():
        Subscription.objects.create(account=account, assignment_id=assignment_id, timestamp=timezone.now())


@transaction.atomic
def set_subscribed(viewer, assignment_id, subscribed):
    """Subscribers get an email when somebody completes the assignment."""
    require_person(viewer)
    assignment = find(Assignment, assignment_id, filtered=False)
    if subscribed:
        subscribe_account(viewer.account, assignment.id)
    else:
        Subscription.objects.filter(deleted=False, account=viewer.account, assignment=assignment).update(deleted=True)


def assignment_exists(title):
    return Assignment.objects.filter(deleted=False, title__iexact=title).exists()


def _create_skill(person, params, uploads):
    title = sanitizer.skill_title(params.get("title"))
    if title is None or Skill.objects.filter(deleted=False, title__iexact=title).exists():
        raise ActionError()
    description = sanitizer.skill_description(params.get("description") or "")
    if description is None:
        raise ActionError()
    if params.get("wikipedia"):
        text = Resource(resource=description, author_name="Wikipedia", author_url="https://en.wikipedia.org",
                        license=LicenseType.WIKIPEDIA)
    else:
        text = Resource(resource=description, author_name=SITE_NAME, author_url=settings.BASE_URL,
                        license=LicenseType.INTERNAL_TEXT)
    if params.get("picture"):
        picture = uploaded_picture(check_upload(params["picture"], uploads), person)
    else:
        picture = default_picture()
    text.save()
    picture.save()
    return Skill.objects.create(
        author=person, creation_timestamp=timezone.now(), title=title, description=text, picture=picture)


@transaction.atomic
def create_assignment(viewer, title, description, skill_id=None, new_skill=None, uploads=()):
    """Creates an assignment in an existing category or together with a new one."""
    person = require_person(viewer)
    title = sanitizer.assignment_title(title)
    if title is None:
        raise ActionError("Please enter a short title for the assignment.")
    if assignment_exists(title):
        raise ActionError("An assigment with this title already exists.")
    description = sanitizer.assignment_description(description or "")
    if description is None:
        raise ActionError("Please describe the assignment with up to 500 characters.")

    if skill_id:
        skill = find(Skill, skill_id, viewer)
    elif new_skill:
        skill = _create_skill(person, new_skill, uploads)
    else:
        raise ActionError("Please select or create a category.")

    now = timezone.now()
    if str(person.id) == presenter.SYSTEM_ID:
        amount = SYSTEM_REWARD
    else:
        amount = balancing.get_initial_assignment_reward(balancing.get_level(person.xp))
    assignment = Assignment.objects.create(
        creation_timestamp=now, author=person, skill=skill, title=title, description=description,
        rewards_sum=amount)
    Reward.objects.create(assignment=assignment, donating_person=person, timestamp=now, amount=amount)

    make_founder(person)
    if viewer.account.subscribe_own_assignments:
        subscribe_account(viewer.account, assignment.id)
    return assignment


def make_founder(person):
    """While the founder assignment exists, everybody who creates an assignment becomes a founder."""
    if not settings.FOUNDER_ASSIGNMENT_ID or person.founder is True:
        return
    founder_assignment = Assignment.objects.filter(id=settings.FOUNDER_ASSIGNMENT_ID, deleted=False).first()
    if founder_assignment is None:
        return
    first = founder_assignment.rewards.filter(deleted=False).order_by("timestamp").first()
    reward = max(0, first.amount) if first else 0
    if Person.objects.filter(id=person.id, deleted=False).exclude(founder=True).update(
            founder=True, xp=F("xp") + reward):
        person.founder = True


#
# skills
#

@transaction.atomic
def delete_skill(viewer, skill_id):
    """Deletes a category with all its assignments, activities and achievements."""
    require_admin(viewer)
    skill = find(Skill, skill_id, filtered=False)
    for assignment in Assignment.objects.filter(skill=skill, deleted=False):
        _delete_assignment(assignment)
    Activity.objects.filter(assignment__skill=skill, deleted=False).update(deleted=True)
    Achievement.objects.filter(skill=skill, deleted=False).update(deleted=True)
    skill.deleted = True
    skill.save(update_fields=["deleted"])
    search.index.update(search.SKILL, skill.id, skill.title, deleted=True)
