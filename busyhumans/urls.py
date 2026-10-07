from django.urls import path
from django.views.generic import RedirectView

from mastery.views import actions, pages, resources, uploads

urlpatterns = [
    path("", pages.index),
    path("index.html", RedirectView.as_view(url="/")),
    path("favicon.png", RedirectView.as_view(url="/static/favicon.png", permanent=True)),

    # Pieces of HTML for the client script
    path("page", pages.page),
    path("list/assignments", pages.list_assignments),
    path("list/achievements", pages.list_achievements),
    path("sidebar", pages.sidebar),
    path("search", pages.search_view),
    path("sync", pages.sync),

    # What users do
    path("action/<slug:name>", actions.dispatch),
    path("mastery/upload", uploads.upload),

    # Pictures. Stored data refers to these paths.
    path("mastery/res/dynamic/<uuid:picture_id>", resources.dynamic),
    path("mastery/res/dynamic/<uuid:picture_id>/<slug:size>", resources.dynamic),
    path("mastery/res/mirrored/<str:name>", resources.mirrored),
    path("mastery/res/youtube/<str:video_id>.jpg", resources.youtube),

    # Preview of a shared assignment
    path("meta", resources.meta),
    path("fb_meta", resources.meta),
]
