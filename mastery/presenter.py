"""Turns the stored data into what the pages show."""
import math

from django.db.models import Count, Min, Prefetch, Q
from django.utils import timezone

from mastery import balancing, media, rewards
from mastery.models import (
    Achievement, Activity, Assignment, ContentBlock, LicenseType, Person, Skill, Subscription)

PAGE_SIZE = 10
SYSTEM_ID = "00000000-0000-0000-0000-000000000000"

# Content with this many abuse reports is only shown to admins
HIDE_AT_ABUSE_REPORTS = 5

SORT_NEWEST = "NEWEST"
SORT_ACTIVITY = "ACTIVITY"
SORT_REWARD = "REWARD"
SORT_FIELDS = {SORT_NEWEST: "creation_timestamp", SORT_ACTIVITY: "last_activity", SORT_REWARD: "rewards_sum"}

PLACEHOLDER_PICTURE = "static/default-skill.png"
VIDEO_PLACEHOLDER_PICTURE = "static/default-video.png"

LICENSES = {
    LicenseType.NONE: ("None", ""),
    LicenseType.CC_BY_30: ("CC BY", "https://creativecommons.org/licenses/by/3.0/"),
    LicenseType.CC_BY_SA_30: ("CC BY-SA", "https://creativecommons.org/licenses/by-sa/3.0/"),
    LicenseType.CC_BY_ND_30: ("CC BY-ND", "https://creativecommons.org/licenses/by-nd/3.0/"),
}

# Pictures with these licenses do not show the license button
NO_LICENSE_BUTTON = (
    LicenseType.NONE, LicenseType.INTERNAL_PICTURE, LicenseType.YOUTUBE, LicenseType.FACEBOOK,
    LicenseType.TWITTER, LicenseType.UNKNOWN)


def hidden(entity, viewer):
    """True if the content is deleted or was reported so often that only admins may see it."""
    return entity is None or entity.deleted or (
        not viewer.is_admin and entity.abuse_reports_count >= HIDE_AT_ABUSE_REPORTS)


def valid(items):
    return [item for item in items if not item.deleted]


#
# pictures
#

def picture(resource, size=None):
    """Returns what the picture component needs. Size is small, medium, large, hires or None."""
    license_name, license_url = LICENSES.get(resource.license, ("Unknown", ""))
    return {
        "url": (getattr(resource, size) if size else None) or resource.resource,
        "hires": resource.hires or resource.resource,
        "show_license": resource.license not in NO_LICENSE_BUTTON,
        "author_name": resource.author_name,
        "author_url": resource.author_url,
        "license_name": license_name,
        "license_url": license_url,
    }


#
# persons
#

def present_person(person, with_counts=False):
    level = balancing.get_level(person.xp)
    result = {
        "id": str(person.id),
        "name": person.name,
        "picture_small": picture(person.picture, "small"),
        "picture_large": picture(person.picture, "large"),
        "xp": person.xp,
        "level": level,
        "level_progress": int(balancing.get_level_progress(person.xp) * 100),
        "level_xp": balancing.get_min_xp_for_level(level),
        "next_level_xp": balancing.get_min_xp_for_level(level + 1),
        "new_assignment_reward": balancing.get_initial_assignment_reward(level),
        "join_date": person.registration_timestamp,
        "founder": person.founder is True,
        "is_system": str(person.id) == SYSTEM_ID,
    }
    if person.registration_timestamp:
        result["joined"] = natural_duration(timezone.now() - person.registration_timestamp) + " ago"
    if with_counts:
        result["created_assignments"] = Assignment.objects.filter(deleted=False, author=person).count()
        result["completed_assignments"] = completed_assignments(person.id).count()
    return result


def get_person(person_id, with_counts=True):
    person = Person.objects.filter(id=person_id, deleted=False).select_related("picture").first()
    return present_person(person, with_counts) if person else None


def get_rankings(viewer):
    """The five persons with the most XP, and the viewer if not among them."""
    top = list(Person.objects.filter(deleted=False, xp__gt=0).exclude(id=SYSTEM_ID)
               .select_related("picture").order_by("-xp")[:5])
    rankings = []
    rank = 0
    last_xp = None
    for person in top:
        if last_xp is None or person.xp < last_xp:
            rank += 1
        last_xp = person.xp
        rankings.append({"rank": rank, "person": present_person(person)})
    myself = None
    if viewer.person and viewer.person.xp > 0 and viewer.person not in top:
        myself = {
            "rank": Person.objects.filter(deleted=False, xp__gt=viewer.person.xp).count() + 1,
            "person": present_person(viewer.person),
        }
        # Like every entry, the own one goes below the dots only from the sixth place on
        if myself["rank"] <= 5:
            rankings.append(myself)
            myself = None
    return {"rankings": rankings, "myself": myself}


#
# skills
#

def present_skill(skill, viewer):
    return {
        "id": str(skill.id),
        "title": skill.title,
        "picture": picture(skill.picture, "medium"),
        "description": skill.description,
        "show_source": skill.description.license != LicenseType.INTERNAL_TEXT,
        "has_reported_abuse": viewer.person is not None and any(
            report.author_id == viewer.person_id for report in valid(skill.abuse_reports.all())),
        "abuse_reports": skill.abuse_reports_count,
    }


def get_skill(skill_id, viewer):
    skill = (Skill.objects.filter(id=skill_id).select_related("picture", "description")
             .prefetch_related("abuse_reports").first())
    if hidden(skill, viewer):
        return None
    return present_skill(skill, viewer)


def get_tag_cloud(min_font_size=7, max_font_size=15):
    """The 30 categories with the most assignments, by name, sized by the number of assignments."""
    # Oldest first: among categories with the same number of assignments, the older ones are kept
    skills = Skill.objects.filter(deleted=False).annotate(
        weight=Count("assignments", filter=Q(assignments__deleted=False))).order_by("creation_timestamp")
    tags = sorted(sorted(skills, key=lambda skill: -skill.weight)[:30], key=lambda skill: skill.title)
    if not tags:
        return []
    low = min(skill.weight for skill in tags)
    high = max(skill.weight for skill in tags)
    result = []
    for skill in tags:
        weight = 0.5 if len(tags) == 1 or low == high else (skill.weight - low) / (high - low)
        result.append({
            "id": str(skill.id),
            "label": skill.title,
            "font_size": "%.6g" % (min_font_size + weight * (max_font_size - min_font_size)),
        })
    return result


#
# activities
#

def java_round(value):
    """Rounds half up."""
    return math.floor(value + 0.5)


def natural_duration(delta):
    """Words like "3 days" for a time span."""
    minute = 60
    hour = 60 * minute
    day = 24 * hour
    week = 7 * day
    month = 30 * day
    year = 365 * day
    seconds = delta.total_seconds()
    for limit, unit, name in ((2 * minute, 1, "seconds"), (2 * hour, minute, "minutes"),
                              (2 * day, hour, "hours"), (2 * week, day, "days"),
                              (2 * month, week, "weeks"), (2 * year, month, "months")):
        if seconds < limit:
            return "%d %s" % (java_round(seconds / unit), name)
    return "%d years" % java_round(seconds / year)


def present_block(block):
    result = {"typ": block.typ, "authentic": block.authentic is True, "value": block.value}
    if block.typ == ContentBlock.Type.IMAGE:
        result["picture"] = picture(block.value, "large")
    return result


def present_activity(activity, viewer, now=None):
    now = now or timezone.now()
    ratings = valid(activity.ratings.all())
    likes = sum(rating.positive for rating in ratings)
    dislikes = len(ratings) - likes
    my_rating = 0
    if viewer.person:
        for rating in ratings:
            if rating.author_id == viewer.person_id:
                my_rating = 1 if rating.positive else -1
    is_author = viewer.person is not None and viewer.person_id == activity.author_id
    can_delete = (
        is_author and activity.rewarded is None
        and (viewer.is_admin or activity.reward_due_date > now))

    if activity.rewarded is not None:
        if activity.rewarded >= 0:
            reward_text = "Gained %d XP for this activity." % activity.rewarded
        else:
            reward_text = "Lost %d XP for this activity." % -activity.rewarded
    elif activity.reward_due_date > now:
        reward_text = "Rewards will be issued in " + natural_duration(activity.reward_due_date - now)
    else:
        reward_text = ""

    like_percent = java_round(100 * likes / (likes + dislikes)) if likes + dislikes else 50
    return {
        "id": str(activity.id),
        "author_id": str(activity.author_id),
        "assignment_id": str(activity.assignment_id),
        "achievement_id": str(activity.achievement_id),
        "blocks": [present_block(block) for block in valid(activity.content_blocks.all())],
        "likes": likes,
        "dislikes": dislikes,
        "net_rating": likes - dislikes,
        "like_percent": like_percent,
        "dislike_percent": 100 - like_percent,
        "my_rating": my_rating,
        "liked": my_rating == 1,
        "disliked": my_rating == -1,
        "rateable": now < activity.reward_due_date,
        "rewarded": activity.rewarded,
        "reward_text": reward_text,
        "show_delete": can_delete or viewer.is_admin,
        "show_abuse": not viewer.logged_in or (not can_delete and not is_author),
        "has_reported_abuse": viewer.person is not None and any(
            report.author_id == viewer.person_id for report in valid(activity.abuse_reports.all())),
        "abuse_reports": activity.abuse_reports_count,
        "show_clear_abuse": viewer.is_admin and activity.abuse_reports_count > 0,
    }


def activity_queryset():
    return (Activity.objects.filter(deleted=False).select_related("author__picture")
            .prefetch_related("content_blocks__value", "ratings", "abuse_reports").order_by("timestamp"))


#
# assignments
#

def assignment_picture(activities, skill):
    """The first picture of the best rated activity, or the picture of the category."""
    candidates = []
    for activity in activities:
        for block in activity["blocks"]:
            if block["typ"] == ContentBlock.Type.IMAGE:
                found = picture(block["value"], "medium")
            elif block["typ"] == ContentBlock.Type.VIDEO:
                thumbnail = media.youtube_thumbnail_path(block["value"].resource)
                found = {"url": thumbnail or VIDEO_PLACEHOLDER_PICTURE, "show_license": False}
            else:
                continue
            rating = 0.0
            if activity["likes"] + activity["dislikes"] > 0:
                rating = activity["likes"] / (activity["likes"] + activity["dislikes"])
            if activity["abuse_reports"] > 0:
                rating *= 0.01 / activity["abuse_reports"]
            candidates.append((found, rating))
            break
    if candidates:
        return max(candidates, key=lambda candidate: candidate[1])[0]
    return picture(skill.picture, "medium")


def present_assignment(assignment, viewer, subscribed=False, now=None):
    """Returns the assignment with its activities, or None if the viewer may not see it."""
    now = now or timezone.now()
    if hidden(assignment, viewer) or hidden(assignment.skill, viewer):
        return None

    activities = []
    for activity in assignment.visible_activities:
        if not hidden(activity, viewer):
            presented = present_activity(activity, viewer, now)
            presented["author"] = present_person(activity.author)
            activities.append(presented)
    # Stable: activities with the same number of likes stay in the order in which they were added
    activities.sort(key=lambda activity: -activity["likes"])

    boosts = valid(assignment.rewards.all())
    boosted = None
    if viewer.person:
        for boost in boosts:
            if boost.donating_person_id == viewer.person_id:
                boosted = boost.amount
                break

    count = len(activities)
    not_rated = sum(1 for activity in activities if activity["rateable"] and activity["my_rating"] == 0)
    noun = " activity" if count == 1 else " activities"
    activities_text = new_activities_text = None
    if count == 0:
        activities_text = "no activity"
    elif not_rated > 0:
        new_activities_text = "%d%s%s to rate" % (
            not_rated, " / %d" % count if not_rated != count else "", noun)
    else:
        activities_text = "%d%s" % (count, noun)

    is_author = viewer.person is not None and viewer.person_id == assignment.author_id
    show_delete = viewer.logged_in and (viewer.is_admin or (is_author and not activities))
    return {
        "id": str(assignment.id),
        "title": assignment.title,
        "description": assignment.description,
        "reward": max(0, sum(boost.amount for boost in boosts)),
        "picture": assignment_picture(activities, assignment.skill),
        "skill": {"id": str(assignment.skill_id), "title": assignment.skill.title},
        "author": present_person(assignment.author),
        "is_author": is_author,
        "activities": activities,
        "activities_text": activities_text,
        "new_activities_text": new_activities_text,
        "has_completed": viewer.person is not None and any(
            activity.author_id == viewer.person_id for activity in assignment.visible_activities),
        "boosted": boosted,
        "has_reported_abuse": viewer.person is not None and any(
            report.author_id == viewer.person_id for report in valid(assignment.abuse_reports.all())),
        "abuse_reports": assignment.abuse_reports_count,
        "show_delete": show_delete,
        "show_abuse": not viewer.logged_in or (not show_delete and not is_author),
        "show_clear_abuse": viewer.is_admin and assignment.abuse_reports_count > 0,
        "subscribed": subscribed,
    }


def load_assignments(ids, viewer):
    """Returns the presented assignments for the ids, in their order, without the hidden ones.

    Looking at an assignment also pays out the rewards of its activities that are due.
    """
    ids = list(ids)
    rewards.distribute(ids)
    assignments = {
        assignment.id: assignment
        for assignment in Assignment.objects.filter(id__in=ids)
        .select_related("skill__picture", "author__picture")
        .prefetch_related(
            Prefetch("activities", queryset=activity_queryset(), to_attr="visible_activities"),
            "rewards", "abuse_reports")
    }
    subscribed = set()
    if viewer.account:
        subscribed = set(Subscription.objects.filter(
            deleted=False, account=viewer.account, assignment_id__in=ids).values_list("assignment_id", flat=True))
    now = timezone.now()
    result = []
    for assignment_id in ids:
        assignment = assignments.get(assignment_id)
        presented = present_assignment(assignment, viewer, assignment_id in subscribed, now) if assignment else None
        if presented:
            result.append(presented)
    return result


def get_assignment(assignment_id, viewer):
    found = load_assignments([assignment_id], viewer)
    return found[0] if found else None


def completed_assignments(person_id):
    return Assignment.objects.filter(
        deleted=False, activities__deleted=False, activities__author_id=person_id).distinct()


def assignment_ids(kind, offset, sort=None, person_id=None, skill_id=None):
    """Returns the ids of one page of a list of assignments."""
    if kind == "active":
        field = SORT_FIELDS.get(sort, "creation_timestamp")
        query = Assignment.objects.filter(deleted=False).order_by("-" + field, "-creation_timestamp")
    elif kind == "completed":
        query = completed_assignments(person_id).order_by("-rewards_sum", "creation_timestamp")
    elif kind == "created":
        query = Assignment.objects.filter(deleted=False, author_id=person_id).order_by(
            "-rewards_sum", "creation_timestamp")
    elif kind == "skill":
        query = Assignment.objects.filter(deleted=False, skill_id=skill_id).order_by("creation_timestamp")
    else:
        raise ValueError(kind)
    return list(query.values_list("id", flat=True)[offset:offset + PAGE_SIZE])


#
# achievements
#

def achievement_ids(person_id, offset):
    """Ids of one page of the achievements of a person, oldest first."""
    query = (Achievement.objects.filter(deleted=False, owner_id=person_id)
             .annotate(first_activity=Min("activities__timestamp")).order_by("first_activity", "id"))
    return list(query.values_list("id", flat=True)[offset:offset + PAGE_SIZE])


def load_achievements(ids, viewer):
    """Returns the presented achievements with their activities and the assignment of each."""
    achievements = {
        achievement.id: achievement
        for achievement in Achievement.objects.filter(id__in=ids, deleted=False)
        .select_related("skill__picture", "skill__description")
        .prefetch_related(Prefetch("activities", queryset=activity_queryset(), to_attr="visible_activities"),
                          "skill__abuse_reports")
    }
    now = timezone.now()
    result = []
    for achievement_id in ids:
        achievement = achievements.get(achievement_id)
        if achievement is None or hidden(achievement.skill, viewer):
            continue
        shown = [activity for activity in achievement.visible_activities if not hidden(activity, viewer)]
        # Highest reward first
        shown.sort(key=lambda activity: -activity.rewarded if activity.rewarded is not None else 0)
        assignments = {
            assignment["id"]: assignment
            for assignment in load_assignments([activity.assignment_id for activity in shown], viewer)}
        activities = []
        for activity in shown:
            assignment = assignments.get(str(activity.assignment_id))
            if assignment is None:
                continue
            presented = present_activity(activity, viewer, now)
            presented["author"] = present_person(activity.author)
            presented["assignment"] = assignment
            activities.append(presented)
        result.append({
            "id": str(achievement.id),
            "skill": present_skill(achievement.skill, viewer),
            "activities": activities,
        })
    return result
