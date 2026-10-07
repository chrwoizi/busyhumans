import re

from django import template
from django.utils.html import escape
from django.utils.safestring import mark_safe

register = template.Library()

# Whatever starts with a scheme or with "www." and goes on with the characters of a url
LINK = re.compile(r"(\b(?:https?://|ftp://|www\.|ftp\.)[-a-zA-Z0-9+&@#/%=~_|$?!:,.]*[a-zA-Z0-9+&@#/%=~_|$])")


@register.filter
def linkify(text, style=""):
    """Escapes the text and turns the urls in it into links that open in a new tab."""
    attributes = ' target="_blank" rel="noopener noreferrer"'
    if style:
        attributes += ' style="%s"' % escape(style)
    parts = []
    for index, part in enumerate(LINK.split(text or "")):
        if index % 2:
            href = part
            if part.startswith("www."):
                href = "http://" + part
            elif part.startswith("ftp."):
                href = "ftp://" + part
            parts.append('<a href="%s"%s>%s</a>' % (escape(href), attributes, escape(part)))
        else:
            parts.append(escape(part))
    return mark_safe("".join(parts))


@register.inclusion_tag("mastery/components/picture.html")
def picture(pic, width=None, height=None, cls="PictureV-outer", link=None, target=None, max_width=None,
            license=True):
    """Shows a picture in a frame.

    With width and height the picture fills the frame and is cropped around its center.
    With max_width it keeps its own size up to that width.
    """
    return {
        "pic": pic, "width": width, "height": height, "cls": cls, "link": link, "target": target,
        "max_width": max_width, "license": license and pic.get("show_license"),
        "outer_width": width + 2 if width else None, "outer_height": height + 2 if height else None,
    }


@register.inclusion_tag("mastery/components/toggle.html")
def toggle(name, down, cls, up_image=None, down_image=None, title="", alt_up="", alt_down="", visible=True):
    """A button that stays pressed, with one picture for each state."""
    return {
        "name": name, "down": down, "cls": cls, "up_image": up_image, "down_image": down_image,
        "title": title, "alt_up": alt_up, "alt_down": alt_down, "visible": visible,
    }
