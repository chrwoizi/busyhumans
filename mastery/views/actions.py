"""Receives what users do. Every action is a POST of JSON to /action/<name>."""
import datetime
import json

from django.conf import settings
from django.http import JsonResponse
from django.template.loader import render_to_string
from django.utils import timezone
from django.views.decorators.http import require_POST

from mastery import accounts, actions, notifications, presenter, sanitizer, search
from mastery.actions import ActionError
from mastery.models import Account, Announcement, Assignment, ContentBlock, Person, Resource
from mastery.viewer import get_viewer
from mastery.views.pages import to_uuid


def uploads_of(request):
    return request.session.get("uploads", [])


#
# accounts
#

def login(request, viewer, data):
    accounts.login(request, data.get("email"), data.get("password"))


def logout(request, viewer, data):
    accounts.logout(request)


def register(request, viewer, data):
    if viewer.logged_in:
        raise ActionError()
    return {"message": accounts.register(
        request, data.get("email"), data.get("password"), data.get("name"), to_uuid(data.get("picture")),
        data.get("website"))}


def verify(request, viewer, data):
    accounts.verify(request, data.get("token"))


def request_reset(request, viewer, data):
    return {"message": accounts.request_reset(request, data.get("email"))}


def reset(request, viewer, data):
    accounts.reset(request, data.get("token"), data.get("password"))


def confirm_email(request, viewer, data):
    accounts.confirm_email_change(request, data.get("token"))


def confirm_tos(request, viewer, data):
    accounts.confirm_tos(viewer)


def preferences(request, viewer, data):
    accounts.set_preferences(
        viewer, data.get("subscribeOwnAssignments"), data.get("notifyOtherActivity"),
        data.get("notifyActivityReward"), data.get("notifyOtherActivityReward"))


def change_email(request, viewer, data):
    return {"message": accounts.request_email_change(request, viewer, data.get("email"), data.get("password"))}


def announcement_closed(request, viewer, data):
    if viewer.account and isinstance(data.get("showTime"), (int, float)):
        show_time = datetime.datetime.fromtimestamp(data["showTime"] / 1000, datetime.UTC)
        Account.objects.filter(id=viewer.account.id).update(last_announcement=show_time)


#
# activities
#

def rate(request, viewer, data):
    value = data.get("value")
    if value not in (-1, 0, 1):
        raise ActionError()
    actions.rate(viewer, to_uuid(data.get("activity")), value)


def activity_abuse(request, viewer, data):
    actions.set_activity_abuse(viewer, to_uuid(data.get("activity")), bool(data.get("abuse")))


def clear_activity_abuse(request, viewer, data):
    actions.clear_activity_abuse(viewer, to_uuid(data.get("activity")))


def delete_activity(request, viewer, data):
    actions.delete_activity(viewer, to_uuid(data.get("activity")))


def video_block(request, viewer, data):
    """Preview of a video for an activity that is being written."""
    actions.require_person(viewer)
    resource = actions.video_block_resource(data.get("video"))
    block = {"typ": ContentBlock.Type.VIDEO, "authentic": False, "value": resource}
    return {"html": render_to_string("mastery/components/new_block.html", {"block": block, "value": resource.resource})}


def create_activity(request, viewer, data):
    blocks = data.get("blocks")
    if not isinstance(blocks, list) or not all(isinstance(block, dict) for block in blocks):
        raise ActionError()
    activity = actions.create_activity(
        viewer, to_uuid(data.get("assignment")), data.get("text"), blocks, uploads_of(request))
    return {"id": str(activity.id)}


#
# assignments
#

def boost(request, viewer, data):
    value = data.get("value")
    if value not in (-1, 0, 1):
        raise ActionError()
    actions.boost(viewer, to_uuid(data.get("assignment")), value)


def assignment_abuse(request, viewer, data):
    actions.set_assignment_abuse(viewer, to_uuid(data.get("assignment")), bool(data.get("abuse")))


def clear_assignment_abuse(request, viewer, data):
    actions.clear_assignment_abuse(viewer, to_uuid(data.get("assignment")))


def delete_assignment(request, viewer, data):
    actions.delete_assignment(viewer, to_uuid(data.get("assignment")))


def speedup(request, viewer, data):
    if data.get("days") not in (-1, 1):
        raise ActionError()
    actions.speedup(viewer, to_uuid(data.get("assignment")), data["days"])


def subscribe(request, viewer, data):
    actions.set_subscribed(viewer, to_uuid(data.get("assignment")), bool(data.get("subscribed")))


def assignment_exists(request, viewer, data):
    actions.require_person(viewer)
    title = sanitizer.assignment_title(data.get("title"))
    return {"exists": title is not None and actions.assignment_exists(title)}


def suggest_skills(request, viewer, data):
    """Categories that are similar to what the user typed, and whether one has exactly that title."""
    actions.require_person(viewer)
    title = sanitizer.skill_title(data.get("title"))
    if title is None:
        return {"html": "", "exact": True}
    skills = search.suggest_skills(title, viewer)
    return {
        "html": render_to_string("mastery/components/existing_skills.html", {"skills": skills}),
        "exact": any(skill["title"].lower() == title.lower() for skill in skills),
    }


def create_assignment(request, viewer, data):
    new_skill = data.get("newSkill")
    if new_skill is not None and not isinstance(new_skill, dict):
        raise ActionError()
    if new_skill and new_skill.get("picture"):
        new_skill["picture"] = to_uuid(new_skill["picture"])
    assignment = actions.create_assignment(
        viewer, data.get("title"), data.get("description"), to_uuid(data.get("skill")), new_skill,
        uploads_of(request))
    return {"id": str(assignment.id)}


#
# skills
#

def skill_abuse(request, viewer, data):
    actions.set_skill_abuse(viewer, to_uuid(data.get("skill")), bool(data.get("abuse")))


def clear_skill_abuse(request, viewer, data):
    actions.clear_skill_abuse(viewer, to_uuid(data.get("skill")))


def delete_skill(request, viewer, data):
    actions.delete_skill(viewer, to_uuid(data.get("skill")))


#
# admin
#

def set_cloak(request, viewer, data):
    """Lets the admin act as one of the test persons, or as him- or herself again."""
    actions.require_admin(viewer)
    person_id = to_uuid(data.get("person"))
    if person_id and not Person.objects.filter(id=person_id, deleted=False, account__isnull=True).exists():
        raise ActionError()
    Account.objects.filter(id=viewer.account.id).update(cloak_id=person_id)


def create_test_person(request, viewer, data):
    actions.require_admin(viewer)
    name = sanitizer.person_name(data.get("name"))
    if name is None:
        raise ActionError("The name is invalid")
    picture_id = actions.check_upload(to_uuid(data.get("picture")), uploads_of(request))
    person = Person(name=name, registration_timestamp=timezone.now(), locale="en_US", timezone=2)
    picture = actions.uploaded_picture(picture_id, person)
    picture.save()
    person.picture = picture
    person.save()
    return {"id": str(person.id)}


def create_announcement(request, viewer, data):
    actions.require_admin(viewer)
    text, show, hide = data.get("text"), data.get("show"), data.get("hide")
    if not isinstance(text, str) or not text or not isinstance(show, int) or show < 0 or (
            hide is not None and (not isinstance(hide, int) or hide <= 0)):
        raise ActionError("Missing text or due time.")
    now = timezone.now()
    Announcement.objects.create(
        text=text[:2000], show_time=now + datetime.timedelta(seconds=show),
        hide_time=now + datetime.timedelta(seconds=hide) if hide is not None else None,
        maintenance=bool(data.get("maintenance")))


def subscribe_all(request, viewer, data):
    actions.require_admin(viewer)
    for assignment_id in Assignment.objects.filter(deleted=False).values_list("id", flat=True):
        actions.subscribe_account(viewer.account, assignment_id)


def send_notifications(request, viewer, data):
    actions.require_admin(viewer)
    notifications.send_emails()


HANDLERS = {
    "login": login, "logout": logout, "register": register, "verify": verify, "request-reset": request_reset,
    "reset": reset, "confirm-email": confirm_email, "confirm-tos": confirm_tos, "preferences": preferences,
    "change-email": change_email, "announcement-closed": announcement_closed,
    "rate": rate, "activity-abuse": activity_abuse, "clear-activity-abuse": clear_activity_abuse,
    "delete-activity": delete_activity, "video-block": video_block, "create-activity": create_activity,
    "boost": boost, "assignment-abuse": assignment_abuse, "clear-assignment-abuse": clear_assignment_abuse,
    "delete-assignment": delete_assignment, "speedup": speedup, "subscribe": subscribe,
    "assignment-exists": assignment_exists, "suggest-skills": suggest_skills,
    "create-assignment": create_assignment,
    "skill-abuse": skill_abuse, "clear-skill-abuse": clear_skill_abuse, "delete-skill": delete_skill,
    "set-cloak": set_cloak, "create-test-person": create_test_person,
    "create-announcement": create_announcement, "subscribe-all": subscribe_all,
    "send-notifications": send_notifications,
}


@require_POST
def dispatch(request, name):
    # Everything here needs an account, so nothing of it works while login is switched off
    if not settings.ENABLE_LOGIN:
        return JsonResponse({"error": "Login is disabled"}, status=403)
    handler = HANDLERS.get(name)
    if handler is None:
        return JsonResponse({"error": "Unknown action"}, status=404)
    try:
        data = json.loads(request.body or b"{}")
    except ValueError:
        data = None
    if not isinstance(data, dict):
        return JsonResponse({"error": "Invalid request"}, status=400)
    try:
        result = handler(request, get_viewer(request), data) or {}
    except ActionError as error:
        return JsonResponse({"error": error.message}, status=400)
    result.setdefault("ok", True)
    return JsonResponse(result)
