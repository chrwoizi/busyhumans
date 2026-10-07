"""The pictures in the media directory.

Resources refer to them by the path under which they are served:

    mastery/res/dynamic/<id>/<size>   uploaded pictures, in the sizes small, medium, large and hires
    mastery/res/mirrored/<name>       copies of pictures that were hotlinked from other sites
    mastery/res/youtube/<id>.jpg      preview pictures of embedded YouTube videos
"""
import io
import re
import urllib.request
import uuid
from pathlib import Path

from django.conf import settings
from PIL import Image, ImageOps

SIZES = ("small", "medium", "large", "hires")

# Width and height that a size may have at most
MAX_EDGE = {"hires": 2000, "large": 400, "medium": 120, "small": 50}

MAX_UPLOAD_BYTES = 10 * 1024 * 1024
# Refuse pictures that would need too much memory when they are decoded
MAX_PIXELS = 40_000_000

YOUTUBE_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")
MIRRORED_NAME = re.compile(r"^[0-9a-f]{40}\.(jpg|png|gif|webp)$")

CONTENT_TYPES = {".jpg": "image/jpeg", ".png": "image/png", ".gif": "image/gif", ".webp": "image/webp"}


class InvalidPicture(Exception):
    pass


def root():
    return Path(settings.MEDIA_ROOT)


def dynamic_file(picture_id, size):
    """File of an uploaded picture, or None if the id or the size is not valid."""
    try:
        picture_id = uuid.UUID(str(picture_id))
    except ValueError:
        return None
    if size not in SIZES:
        return None
    return root() / "dynamic" / str(picture_id) / (size + ".jpg")


def mirrored_file(name):
    return root() / "mirrored" / name if MIRRORED_NAME.match(name) else None


def youtube_file(video_id):
    return root() / "youtube" / (video_id + ".jpg") if YOUTUBE_ID.match(video_id or "") else None


def youtube_thumbnail_path(video_id):
    """Path of the local preview picture of a video, or None if there is none."""
    file = youtube_file(video_id)
    if file is not None and file.exists():
        return "mastery/res/youtube/%s.jpg" % video_id
    return None


def fetch_youtube_thumbnail(video_id):
    """Copies the preview picture of a video to disk. Returns False if that is not possible."""
    file = youtube_file(video_id)
    if file is None:
        return False
    try:
        with urllib.request.urlopen("https://i.ytimg.com/vi/%s/hqdefault.jpg" % video_id, timeout=10) as response:
            data = response.read(MAX_UPLOAD_BYTES + 1)
        image = open_image(data)
    except (OSError, InvalidPicture):
        return False
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_bytes(to_jpeg(image))
    return True


def open_image(data):
    """Decodes an uploaded file. What it claims to be does not matter, only what it is."""
    if len(data) > MAX_UPLOAD_BYTES:
        raise InvalidPicture("The file is too large.")
    try:
        image = Image.open(io.BytesIO(data))
        if image.format not in ("JPEG", "PNG", "GIF", "BMP", "WEBP"):
            raise InvalidPicture("This kind of picture is not supported.")
        if image.width * image.height > MAX_PIXELS:
            raise InvalidPicture("The picture is too large.")
        image.load()
        # Turn the picture the way the camera held it, then drop that and all other metadata
        image = ImageOps.exif_transpose(image)
    except InvalidPicture:
        raise
    except Exception:
        raise InvalidPicture("The file is not a picture.")
    if image.mode != "RGB":
        background = Image.new("RGB", image.size, (255, 255, 255))
        rgba = image.convert("RGBA")
        background.paste(rgba, mask=rgba.split()[3])
        image = background
    return image


def to_jpeg(image):
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=90)
    return buffer.getvalue()


def resize(image, max_edge, scale_largest_axis):
    """Scales the picture down.

    scale_largest_axis: the longer side becomes max_edge, so the picture fits into the square.
    Otherwise the shorter side becomes max_edge, so the picture fills the square.
    """
    exceeds_one = image.width > max_edge or image.height > max_edge
    exceeds_both = image.width > max_edge and image.height > max_edge
    if not ((scale_largest_axis and exceeds_one) or (not scale_largest_axis and exceeds_both)):
        return image
    landscape = image.width >= image.height
    if landscape == scale_largest_axis:
        size = (max_edge, max(1, image.height * max_edge // image.width))
    else:
        size = (max(1, image.width * max_edge // image.height), max_edge)
    return image.resize(size, Image.LANCZOS)


def save_upload(data):
    """Stores an uploaded picture in all sizes. Returns its id."""
    image = open_image(data)
    picture_id = uuid.uuid4()
    previous = None
    for size in ("hires", "large", "medium", "small"):
        previous = resize(previous or image, MAX_EDGE[size], scale_largest_axis=size == "hires")
        file = dynamic_file(picture_id, size)
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(to_jpeg(previous))
    return picture_id


def delete_upload(picture_id):
    for size in SIZES:
        file = dynamic_file(picture_id, size)
        if file is not None and file.exists():
            file.unlink()
    directory = dynamic_file(picture_id, "small")
    if directory is not None and directory.parent.exists():
        directory.parent.rmdir()


def upload_exists(picture_id):
    file = dynamic_file(picture_id, "small")
    return file is not None and file.exists()
