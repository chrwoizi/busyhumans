"""Search for persons, categories and assignments by name or title.

Everything is kept in memory: the index is built from the database on first use
and follows every change of a person, skill or assignment (see apps.py). That
is fine for some ten thousand entries in one server process. With more data or
more than one process, this module is the place to put something bigger.

How it matches: the text is split into lowercase words at everything that is
not a letter. An entry is found if every word of the query is part of one of
its words. Words that match from their beginning rank higher, and so do
shorter names.
"""
import threading
from dataclasses import dataclass
from uuid import UUID

PERSON = 1
SKILL = 2
ASSIGNMENT = 3

# Longer words in a query never match
MAX_WORD_LENGTH = 15

SYSTEM_ID = UUID(int=0)


def words(text):
    """Splits a text into its lowercase words. Digits and punctuation separate words."""
    result = []
    current = []
    for char in (text or "").lower():
        if char.isalpha():
            current.append(char)
        elif current:
            result.append("".join(current))
            current = []
    if current:
        result.append("".join(current))
    return result


@dataclass(frozen=True)
class Entry:
    typ: int
    id: UUID
    value: str
    words: tuple


class Index:

    def __init__(self):
        self._entries = {}
        self._lock = threading.Lock()
        self._built = False

    def update(self, typ, id, value, deleted=False):
        """Adds, replaces or (if deleted) removes an entry."""
        with self._lock:
            if deleted or not value:
                self._entries.pop((typ, id), None)
            else:
                self._entries[(typ, id)] = Entry(typ, id, value, tuple(words(value)))

    def clear(self):
        with self._lock:
            self._entries.clear()
            self._built = False

    def rebuild(self):
        from mastery.models import Assignment, Person, Skill
        entries = {}
        for typ, queryset, field in ((PERSON, Person.objects, "name"), (SKILL, Skill.objects, "title"),
                                     (ASSIGNMENT, Assignment.objects, "title")):
            for id, value in queryset.filter(deleted=False).values_list("id", field):
                if value:
                    entries[(typ, id)] = Entry(typ, id, value, tuple(words(value)))
        with self._lock:
            self._entries = entries
            self._built = True

    def query(self, text, max_results, typ=None):
        """Returns [(type, id)] of the best matches. An empty query matches everything."""
        if not self._built:
            self.rebuild()
        query_words = words(text.replace('"', "").replace("'", ""))
        if any(len(word) > MAX_WORD_LENGTH for word in query_words):
            return []
        with self._lock:
            entries = list(self._entries.values())
        found = []
        for entry in entries:
            if typ is not None and entry.typ != typ:
                continue
            score = self._score(entry, query_words)
            if score is not None:
                found.append((score, len(entry.value), entry.value.lower(), str(entry.id), entry))
        found.sort(key=lambda item: item[:4])
        return [(item[4].typ, item[4].id) for item in found[:max_results]]

    @staticmethod
    def _score(entry, query_words):
        """Lower is better. None if the entry does not match."""
        score = 0
        for query_word in query_words:
            best = None
            for word in entry.words:
                if word == query_word:
                    rank = 0
                elif word.startswith(query_word):
                    rank = 1
                elif query_word in word:
                    rank = 2
                else:
                    continue
                best = rank if best is None else min(best, rank)
            if best is None:
                return None
            score += best
        return score


index = Index()


def search(text, viewer, max_results=5):
    """Returns ([results], has_more) for the search box. A result is ("person" | "skill" | "assignment", data)."""
    from mastery import presenter

    found = index.query(text, max_results + 1)
    results = []
    for typ, id in found[:max_results]:
        if typ == PERSON:
            # The system user is not to be found
            data = presenter.get_person(id, with_counts=False) if id != SYSTEM_ID else None
            kind = "person"
        elif typ == ASSIGNMENT:
            data = presenter.get_assignment(id, viewer)
            kind = "assignment"
        else:
            data = presenter.get_skill(id, viewer)
            kind = "skill"
        if data:
            results.append((kind, data))
    return results, len(found) > max_results


def suggest_skills(title, viewer, max_results=3):
    """Existing categories that are similar to a title."""
    from mastery import presenter

    skills = [presenter.get_skill(id, viewer) for _, id in index.query(title, max_results, typ=SKILL)]
    return [skill for skill in skills if skill]
