"""Serves the pictures of the media directory and the page that sharing services read."""
from django.conf import settings
from django.http import FileResponse, Http404
from django.shortcuts import render
from django.views.decorators.http import require_GET

from mastery import media, presenter
from mastery.viewer import ANONYMOUS
from mastery.views.pages import to_uuid

# The files never change: an upload gets a new id
CACHE = "public, max-age=31536000, immutable"


def send(file):
    if file is None or not file.is_file():
        raise Http404()
    response = FileResponse(open(file, "rb"), content_type=media.CONTENT_TYPES[file.suffix])
    response["Cache-Control"] = CACHE
    response["X-Content-Type-Options"] = "nosniff"
    return response


@require_GET
def dynamic(request, picture_id, size="large"):
    return send(media.dynamic_file(picture_id, size))


@require_GET
def mirrored(request, name):
    return send(media.mirrored_file(name))


@require_GET
def youtube(request, video_id):
    return send(media.youtube_file(video_id))


def absolute(url):
    return url if url.startswith(("http://", "https://")) else settings.BASE_URL + url


@require_GET
def meta(request):
    """A page with the title and picture of an assignment for the preview of a shared link.

    Browsers are sent on to the assignment: /meta?token=assignment%3D<id>
    """
    token = request.GET.get("token", "")
    context = {"url": settings.BASE_URL}
    if token.startswith("assignment="):
        assignment_id = to_uuid(token[len("assignment="):])
        assignment = presenter.get_assignment(assignment_id, ANONYMOUS) if assignment_id else None
        if assignment:
            context = {
                "url": "%s#assignment=%s" % (settings.BASE_URL, assignment["id"]),
                "meta_url": "%smeta?token=assignment%%3D%s" % (settings.BASE_URL, assignment["id"]),
                "title": assignment["title"],
                "description": assignment["description"],
                "image": absolute(assignment["picture"]["url"]),
            }
    return render(request, "mastery/meta.html", context)
