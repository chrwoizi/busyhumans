"""The page shell and the pieces of HTML that the client script puts into it."""
import uuid

from django.conf import settings
from django.http import HttpResponse, HttpResponseBadRequest, JsonResponse
from django.shortcuts import render
from django.utils import timezone
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.http import require_GET

from mastery import presenter, search
from mastery.models import Announcement
from mastery.viewer import get_viewer

TABS = ("PARTICIPATIONS", "CREATED", "ACHIEVEMENTS")


def to_uuid(value):
    try:
        return uuid.UUID(value)
    except (ValueError, TypeError, AttributeError):
        return None


def to_offset(request):
    try:
        return max(0, int(request.GET.get("offset", "0")))
    except ValueError:
        return 0


def status_of(viewer):
    if viewer.banned:
        return "ACCESS_DENIED"
    if not viewer.logged_in:
        return "NOT_AUTHORIZED"
    return "NEEDS_TOS_CONFIRMATION" if viewer.needs_tos_confirmation else "AUTHORIZED"


def sidebar_context(viewer):
    return {"tags": presenter.get_tag_cloud(), "rankings": presenter.get_rankings(viewer)}


@require_GET
@ensure_csrf_cookie
def index(request):
    viewer = get_viewer(request)
    context = {
        "viewer": viewer,
        "status": status_of(viewer),
        "enable_login": settings.ENABLE_LOGIN,
        "enable_search": settings.ENABLE_SEARCH,
        "base_url": settings.BASE_URL,
        "contact": settings.CONTACT_EMAIL,
        "myself": presenter.present_person(viewer.person) if viewer.person else None,
    }
    context.update(sidebar_context(viewer))
    return render(request, "mastery/index.html", context)


@require_GET
def sidebar(request):
    return render(request, "mastery/sidebar/content.html", sidebar_context(get_viewer(request)))


def boost_context(assignment, viewer):
    """State and tooltips of the two buttons that raise and lower the reward."""
    boosted = assignment["boosted"] if viewer.person else None
    by = " by %d XP" % presenter.present_person(viewer.person)["new_assignment_reward"] if viewer.person else ""
    up = boosted is not None and boosted > 0
    down = boosted is not None and boosted < 0
    return {
        "up": up,
        "down": down,
        "up_title": "You have increased the reward by %d XP." % boosted if up else (
            "Click this button to increase the reward of this assignment%s."
            " This will make it more likely for someone to complete the assignment."
            " Therefore you should activate this button if you like the assignment and want to see"
            " other people's performances." % by),
        "down_title": "You have decreased the reward by %d XP." % -boosted if down else (
            "Click this button to decrease the reward of this assignment%s."
            " This will make it less likely for someone to complete the assignment."
            " Therefore you should activate this button only if you think that the reward is too high." % by),
    }


@require_GET
def page(request):
    """Returns the page for the part of the address after the #.

    The header X-Menu names the entry of the top bar that the page belongs to.
    """
    viewer = get_viewer(request)
    token = request.GET.get("token", "")
    context = {
        "viewer": viewer,
        "enable_login": settings.ENABLE_LOGIN,
        "default_picture": {"url": presenter.PLACEHOLDER_PICTURE, "show_license": False},
        "imprint_street": settings.IMPRINT_STREET,
    }
    menu = ""

    if viewer.logged_in and viewer.needs_tos_confirmation:
        template = "agreement"
    elif token.startswith("about"):
        template, menu = "about", "about"
    elif token.startswith("person="):
        template = "person"
        person_token = token[len("person="):]
        person_id = viewer.person_id if person_token == "me" else to_uuid(person_token)
        context["person"] = presenter.get_person(person_id) if person_id else None
        if context["person"]:
            tab = request.GET.get("tab")
            tab = tab if tab in TABS else TABS[0]
            if tab == "ACHIEVEMENTS":
                list_url = "/list/achievements?person=%s" % person_id
            else:
                kind = "created" if tab == "CREATED" else "completed"
                list_url = "/list/assignments?kind=%s&person=%s" % (kind, person_id)
            context.update(tab=tab, list_url=list_url, anti_progress=100 - context["person"]["level_progress"])
            if viewer.person_id == person_id:
                menu = "myself"
    elif token.startswith("discover"):
        template, menu = discover(request, context)
    elif token.startswith("create"):
        template, menu = "create", "create"
        if viewer.person:
            context["reward"] = presenter.present_person(viewer.person)["new_assignment_reward"]
    elif token.startswith("assignment="):
        template = "assignment"
        assignment_id = to_uuid(token[len("assignment="):])
        context["assignment"] = presenter.get_assignment(assignment_id, viewer) if assignment_id else None
        if context["assignment"]:
            context["boost"] = boost_context(context["assignment"], viewer)
            if viewer.person:
                context["myself"] = presenter.present_person(viewer.person)
    elif token.startswith("category="):
        template = "skill"
        skill_id = to_uuid(token[len("category="):])
        context["skill"] = presenter.get_skill(skill_id, viewer) if skill_id else None
        context["show_abuse"] = not viewer.is_admin
        context["list_url"] = "/list/assignments?kind=skill&skill=%s" % skill_id
    elif token.startswith("imprint"):
        template = "imprint"
    elif token.startswith("legal"):
        template = "legal"
    elif token.startswith("admin"):
        from mastery.views import admin
        template, menu = "admin", "admin"
        context.update(admin.context(viewer))
    elif token.startswith("register") and not viewer.logged_in:
        template = "register"
    elif token.startswith("preferences"):
        template = "preferences"
    elif token.startswith("verify=") or token.startswith("reset") or token.startswith("email="):
        template = "token"
        context["token"] = token
    else:
        template, menu = discover(request, context)

    response = render(request, "mastery/pages/%s.html" % template, context)
    response["X-Menu"] = menu
    response["Cache-Control"] = "no-store"
    return response


def discover(request, context):
    sort = request.GET.get("sort")
    sort = sort if sort in presenter.SORT_FIELDS else presenter.SORT_ACTIVITY
    context.update(sort=sort, list_url="/list/assignments?kind=active&sort=%s" % sort)
    return "discover", "discover"


def list_response(request, template, context, count):
    """One page of a list. The header X-Count says how many entries the page covers, shown or not."""
    response = render(request, template, context)
    response["X-Count"] = str(count)
    response["Cache-Control"] = "no-store"
    return response


@require_GET
def list_assignments(request):
    viewer = get_viewer(request)
    kind = request.GET.get("kind")
    person_id = to_uuid(request.GET.get("person"))
    skill_id = to_uuid(request.GET.get("skill"))
    if kind not in ("active", "completed", "created", "skill") or (
            kind in ("completed", "created") and not person_id) or (kind == "skill" and not skill_id):
        return HttpResponseBadRequest()
    ids = presenter.assignment_ids(kind, to_offset(request), request.GET.get("sort"), person_id, skill_id)
    context = {"assignments": presenter.load_assignments(ids, viewer), "viewer": viewer}
    return list_response(request, "mastery/lists/assignments.html", context, len(ids))


@require_GET
def list_achievements(request):
    viewer = get_viewer(request)
    person_id = to_uuid(request.GET.get("person"))
    if not person_id:
        return HttpResponseBadRequest()
    ids = presenter.achievement_ids(person_id, to_offset(request))
    context = {"achievements": presenter.load_achievements(ids, viewer), "viewer": viewer}
    return list_response(request, "mastery/lists/achievements.html", context, len(ids))


@require_GET
def search_view(request):
    if not settings.ENABLE_SEARCH:
        return HttpResponse(status=404)
    viewer = get_viewer(request)
    results, has_more = search.search(request.GET.get("q", "")[:100], viewer)
    response = render(request, "mastery/sidebar/search_results.html", {"results": results})
    response["X-More"] = "true" if has_more else "false"
    return response


@require_GET
def sync(request):
    """The announcements that the viewer has not closed yet."""
    viewer = get_viewer(request)
    now = timezone.now()
    announcements = Announcement.objects.filter(deleted=False, show_time__lt=now)
    if viewer.account:
        announcements = announcements.filter(hide_time__isnull=True) | announcements.filter(hide_time__gt=now)
        if viewer.account.last_announcement:
            announcements = announcements.filter(show_time__gt=viewer.account.last_announcement)
        announcements = announcements.order_by("show_time")[:100]
    else:
        announcements = announcements.filter(hide_time__gt=now).order_by("-show_time")[:1]
    return JsonResponse({
        "serverTime": int(now.timestamp() * 1000),
        "announcements": [{
            "id": str(announcement.id),
            "showTime": int(announcement.show_time.timestamp() * 1000),
            "hideTime": int(announcement.hide_time.timestamp() * 1000) if announcement.hide_time else None,
            "text": announcement.text,
            "maintenance": announcement.maintenance,
        } for announcement in announcements],
    })


def healthz(request):
    return HttpResponse("ok", content_type="text/plain")
