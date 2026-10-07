"""Data of the admin page."""
from django.utils import timezone

from mastery import presenter
from mastery.models import Account, Activity, Assignment, Person, Skill


def context(viewer):
    if not viewer.is_admin:
        return {}
    reported = {"deleted": False, "abuse_reports_count__gt": 0}

    skill_ids = Skill.objects.filter(**reported).order_by("-abuse_reports_count").values_list("id", flat=True)[:100]
    skills = [skill for skill in (presenter.get_skill(skill_id, viewer) for skill_id in skill_ids) if skill]

    assignment_ids = Assignment.objects.filter(**reported).order_by("-abuse_reports_count").values_list(
        "id", flat=True)[:100]

    now = timezone.now()
    activities = []
    for activity in presenter.activity_queryset().filter(**reported).order_by("-abuse_reports_count")[:100]:
        presented = presenter.present_activity(activity, viewer, now)
        presented["author"] = presenter.present_person(activity.author)
        activities.append(presented)

    users = []
    for account in Account.objects.filter(deleted=False, person__isnull=False, person__deleted=False).select_related(
            "person__picture", "user"):
        person = presenter.present_person(account.person)
        users.append({
            "person": person,
            "login": "legacy" if account.legacy else "password",
            "has_email": bool(account.email),
            "last_login": presenter.natural_duration(now - account.last_login) if account.last_login else None,
        })
    users.sort(key=lambda user: user["person"]["join_date"] or now, reverse=True)

    return {
        # Persons without an account: the test persons and the system user
        "cloaks": Person.objects.filter(deleted=False, account__isnull=True).order_by("name"),
        "reported_skills": skills,
        "reported_assignments": presenter.load_assignments(assignment_ids, viewer),
        "reported_activities": activities,
        "users": users,
    }
