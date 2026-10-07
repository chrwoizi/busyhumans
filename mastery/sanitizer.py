"""Cleans up and checks the texts that users enter."""
import re

MIN_SKILL_TITLE_LENGTH = 3
MAX_SKILL_TITLE_LENGTH = 40
MAX_SKILL_DESCRIPTION_LENGTH = 500

MIN_ASSIGNMENT_TITLE_LENGTH = 3
MAX_ASSIGNMENT_TITLE_LENGTH = 40
MAX_ASSIGNMENT_DESCRIPTION_LENGTH = 500

MAX_ACTIVITY_TEXT_LENGTH = 500

NAME_LENGTH_MIN = 2
NAME_LENGTH_MAX = 48


def _common(text, min_length, max_length):
    """Returns the text on one line with single spaces, or None if its length is not allowed."""
    if text is None or not min_length <= len(text) <= max_length:
        return None
    result = re.sub(r"\s+", " ", text.replace("\r", "").replace("\n", " ").strip())
    return capitalize(result)


def capitalize(text):
    if text.startswith("http"):
        return text
    return text[:1].upper() + text[1:]


def skill_title(text):
    return _common(text, MIN_SKILL_TITLE_LENGTH, MAX_SKILL_TITLE_LENGTH)


def skill_description(text):
    return _common(text, 0, MAX_SKILL_DESCRIPTION_LENGTH)


def assignment_title(text):
    return _common(text, MIN_ASSIGNMENT_TITLE_LENGTH, MAX_ASSIGNMENT_TITLE_LENGTH)


def assignment_description(text):
    return _common(text, 0, MAX_ASSIGNMENT_DESCRIPTION_LENGTH)


def activity_text(text):
    return _common(text, 0, MAX_ACTIVITY_TEXT_LENGTH)


def person_name(text):
    """Returns the trimmed name, or None if it is too short or too long."""
    if text is None or not NAME_LENGTH_MIN <= len(text.strip()) <= NAME_LENGTH_MAX:
        return None
    return text.strip()
