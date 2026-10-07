from django.http import JsonResponse
from django.template.loader import render_to_string
from django.views.decorators.http import require_POST

from mastery import accounts, actions, media, presenter
from mastery.actions import ActionError
from mastery.models import ContentBlock
from mastery.viewer import get_viewer


@require_POST
def upload(request):
    """Stores an uploaded picture. Answers with its id and a preview for a new activity."""
    viewer = get_viewer(request)
    for_registration = request.POST.get("register") == "true"
    if not accounts.may_upload(viewer, for_registration):
        return JsonResponse({"error": "Please login."}, status=403)
    try:
        accounts.throttle("upload", request.session.session_key or accounts.client_address(request), 60, 3600)
        if for_registration:
            accounts.throttle("upload-register", accounts.client_address(request), 10, 3600)
        file = request.FILES.get("file")
        if file is None or file.size > media.MAX_UPLOAD_BYTES:
            raise ActionError("The file is too large.")
        try:
            picture_id = media.save_upload(file.read())
        except media.InvalidPicture as error:
            raise ActionError(str(error))
    except ActionError as error:
        return JsonResponse({"error": error.message}, status=400)

    accounts.remember_upload(request, picture_id, for_registration)
    path = "mastery/res/dynamic/%s" % picture_id
    result = {"id": str(picture_id), "medium": path + "/medium"}
    if viewer.person is not None:
        resource = actions.uploaded_picture(picture_id, viewer.person)
        block = {"typ": ContentBlock.Type.IMAGE, "authentic": True, "value": resource,
                 "picture": presenter.picture(resource, "large")}
        result["html"] = render_to_string(
            "mastery/components/new_block.html", {"block": block, "value": str(picture_id)})
    return JsonResponse(result)
