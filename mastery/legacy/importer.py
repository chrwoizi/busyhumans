"""Imports the data of the old MongoDB database.

What changes on the way:

- Accounts become legacy accounts. Their passwords, the tokens of Facebook,
  Twitter and Google and the token of the login cookie are not imported.
- The uploaded pictures move from the database to the media directory.
- Pictures that were hotlinked from other sites point to the local copy. If the
  picture no longer exists, the placeholder takes its place.
- The lists of activities inside assignments and achievements are not imported,
  because every activity already names its assignment and achievement.
- An activity that was removed from the list of its assignment but not deleted
  itself is imported as deleted, and so is an achievement that is left without
  activities. The old site never rewarded such an activity; as a live activity
  it would be rewarded now.
"""
import json
import re
import shutil
from collections import Counter
from pathlib import Path

from django.db import transaction

from mastery import media, models
from mastery.legacy.archive import to_uuid

PLACEHOLDER = "static/default-skill.png"
DYNAMIC_PREFIX = "mastery/res/dynamic/"
MIRRORED_PREFIX = "mastery/res/mirrored/"

# From the smallest to the largest. "resource" is the picture in its default size.
PICTURE_FIELDS = ("small", "medium", "resource", "large", "hires")

FILE_EXTENSIONS = {"image/jpeg": ".jpg"}

DYNAMIC_ID = re.compile(re.escape(DYNAMIC_PREFIX) + r"([0-9a-f-]{36})")


class ImportFailed(Exception):
    pass


class Importer:
    """
    collections: {collection name: [documents]} of the old database
    media_root: directory that receives the pictures
    external_dir: directory with the copies of the hotlinked pictures and their manifest.json
    """

    def __init__(self, collections, media_root, external_dir=None):
        self.collections = collections
        self.media_root = Path(media_root)
        self.external_dir = Path(external_dir) if external_dir else None
        self.manifest = {}
        if self.external_dir:
            with open(self.external_dir / "manifest.json", encoding="utf-8") as f:
                self.manifest = json.load(f)

        self.objects = {}
        self.mirrored = {}
        # Ids of activities that were removed from the list of their assignment
        self.removed_from_assignment = set()
        self.notes = Counter()

    def docs(self, collection):
        return self.collections.get(collection, [])

    def add(self, obj):
        self.objects.setdefault(type(obj), []).append(obj)
        return obj

    #
    # resources
    #

    def base_resource(self, doc):
        return models.Resource(
            id=to_uuid(doc["id"]),
            deleted=doc["deleted"],
            resource=doc["resource"],
            small=doc.get("small"),
            medium=doc.get("medium"),
            large=doc.get("large"),
            hires=doc.get("hires"),
            author_name=doc.get("authorName"),
            author_url=doc.get("authorUrl"),
            license=doc["license"],
        )

    def text(self, doc):
        return self.add(self.base_resource(doc))

    def picture(self, doc):
        resource = self.base_resource(doc)
        original = {field: getattr(resource, field) for field in PICTURE_FIELDS}
        if not any(is_external(value) for value in original.values()):
            return self.add(resource)

        local = {field: self.local_copy(value) if is_external(value) else value
                 for field, value in original.items()}
        if not any(local.values()):
            # Nothing of the picture is left. The author of the placeholder is the site itself.
            self.notes["pictures replaced by the placeholder"] += 1
            resource.resource = PLACEHOLDER
            resource.small = resource.medium = resource.large = resource.hires = None
            resource.author_name = "busyhumans.com"
            resource.author_url = "http://busyhumans.com"
            resource.license = models.LicenseType.INTERNAL_PICTURE
            return self.add(resource)

        self.notes["pictures that point to a local copy"] += 1
        for index, field in enumerate(PICTURE_FIELDS):
            if original[field] and not local[field]:
                # This size is gone: use the size that is closest to it
                self.notes["picture sizes replaced by another size"] += 1
                nearest = min((i for i, f in enumerate(PICTURE_FIELDS) if local[f]), key=lambda i: abs(i - index))
                setattr(resource, field, local[PICTURE_FIELDS[nearest]])
            else:
                setattr(resource, field, local[field])
        return self.add(resource)

    def local_copy(self, url):
        """Returns the local path of a hotlinked picture, or None if there is no copy."""
        if url not in self.mirrored:
            entry = self.manifest.get(url)
            path = None
            if entry is None:
                status = "not-copied"
                self.notes["hotlinked pictures that the manifest does not know"] += 1
            else:
                status = entry["status"]
                if status == "ok":
                    path = "mirrored/" + entry["file"]
                    target = self.media_root / path
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(self.external_dir / entry["file"], target)
            self.mirrored[url] = self.add(models.MirroredFile(url=url, path=path, status=status))
        path = self.mirrored[url].path
        return MIRRORED_PREFIX + path.removeprefix("mirrored/") if path else None

    def copy_video_thumbnail(self, video_id):
        """Puts the copy of the preview picture of an embedded YouTube video into the media directory."""
        entry = self.manifest.get("http://i2.ytimg.com/vi/%s/hqdefault.jpg" % video_id)
        target = media.youtube_file(video_id)
        if entry is None or entry["status"] != "ok" or target is None:
            self.notes["videos without a copy of their preview picture"] += 1
            return
        target = self.media_root / "youtube" / target.name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(self.external_dir / entry["file"], target)
        self.notes["preview pictures of videos copied"] += 1

    #
    # collections
    #

    def import_cities(self):
        for doc in self.docs("CITY"):
            self.add(models.City(
                id=to_uuid(doc["id"]), deleted=doc["deleted"],
                facebook_id=doc.get("facebookId"), name=doc["name"]))

    def import_accounts(self):
        for doc in self.docs("ACCOUNT"):
            preferences = doc.get("preferences") or {}
            self.add(models.Account(
                id=to_uuid(doc["id"]),
                deleted=doc["deleted"],
                legacy=True,
                email=doc.get("email"),
                join_date=doc.get("joindate"),
                last_login=doc.get("lastLogin"),
                confirmed_tos_version=doc.get("confirmedTosVersion") or 0,
                person_id=to_uuid(doc.get("person")),
                cloak_id=to_uuid(doc.get("cloak")),
                admin=doc.get("admin", False),
                banned=doc.get("banned", False),
                last_announcement=doc.get("lastAnnouncement"),
                subscribe_own_assignments=preferences.get("subscribeOwnAssignments", True),
                notify_other_activity=preferences.get("notifyOtherActivity", True),
                notify_activity_reward=preferences.get("notifyActivityReward", True),
                notify_other_activity_reward=preferences.get("notifyOtherActivityReward", True),
            ))

    def import_persons(self):
        for doc in self.docs("PERSON"):
            birthday = doc.get("birthday")
            person = self.add(models.Person(
                id=to_uuid(doc["id"]),
                deleted=doc["deleted"],
                account_id=to_uuid(doc.get("account")),
                registration_timestamp=doc.get("registrationTimestamp"),
                name=doc["name"],
                picture=self.picture(doc["picture"]),
                gender=doc.get("gender"),
                birthday=birthday.date() if birthday else None,
                city_id=to_uuid(doc.get("city")),
                locale=doc.get("locale"),
                timezone=doc.get("timezone") or 0,
                xp=doc.get("xp") or 0,
                founder=doc.get("founder"),
            ))
            for contact in doc.get("contacts", []):
                self.add(models.Contact(
                    id=to_uuid(contact["id"]), deleted=contact["deleted"], person=person,
                    timestamp=contact["timestamp"], typ=contact["typ"], value=contact["value"]))

    def import_abuse_reports(self, doc, **target):
        reports = doc.get("abuseReports", [])
        for report in reports:
            self.add(models.AbuseReport(
                id=to_uuid(report["id"]), deleted=report["deleted"],
                author_id=to_uuid(report["author"]), timestamp=report["timestamp"], **target))
        if doc["abuseReportsCount"] != sum(not report["deleted"] for report in reports):
            self.notes["abuse report counters that differ from the reports"] += 1

    def import_skills(self):
        for doc in self.docs("SKILL"):
            skill = self.add(models.Skill(
                id=to_uuid(doc["id"]),
                deleted=doc["deleted"],
                author_id=to_uuid(doc["author"]),
                creation_timestamp=doc["creationTimestamp"],
                title=doc["title"],
                description=self.text(doc["description"]),
                picture=self.picture(doc["picture"]),
                abuse_reports_count=doc["abuseReportsCount"],
            ))
            self.import_abuse_reports(doc, skill=skill)

    def import_assignments(self):
        activities = {to_uuid(doc["id"]): doc for doc in self.docs("ACTIVITY")}
        for doc in self.docs("ASSIGNMENT"):
            assignment = self.add(models.Assignment(
                id=to_uuid(doc["id"]),
                deleted=doc["deleted"],
                creation_timestamp=doc["creationTimestamp"],
                author_id=to_uuid(doc["author"]),
                skill_id=to_uuid(doc["skill"]),
                title=doc["title"],
                description=doc.get("description") or "",
                # Imported as stored: this is the value that the old site showed and sorted by
                rewards_sum=doc.get("rewardsSum") or 0,
                last_activity=doc.get("lastActivity"),
                abuse_reports_count=doc["abuseReportsCount"],
            ))
            rewards = doc.get("rewards", [])
            for reward in rewards:
                self.add(models.Reward(
                    id=to_uuid(reward["id"]), deleted=reward["deleted"], assignment=assignment,
                    timestamp=reward["timestamp"], donating_person_id=to_uuid(reward["donatingPerson"]),
                    amount=reward["amount"]))
            if assignment.rewards_sum != sum(r["amount"] for r in rewards if not r["deleted"]):
                self.notes["assignments whose reward differs from the sum of its boosts"] += 1
            self.import_abuse_reports(doc, assignment=assignment)

            # The list of activities is not imported. Check that it says the same as the activities.
            for ref in doc.get("activities", []):
                activity = activities.get(to_uuid(ref["activity"]))
                if (activity is None
                        or to_uuid(activity["assignment"]) != assignment.id
                        or to_uuid(activity["author"]) != to_uuid(ref["person"])):
                    raise ImportFailed("Assignment %s lists an activity that does not belong to it." % assignment.id)
                if ref["deleted"] and not activity["deleted"]:
                    self.removed_from_assignment.add(to_uuid(ref["activity"]))

    def import_achievements(self):
        activities = {to_uuid(doc["id"]): doc for doc in self.docs("ACTIVITY")}
        for doc in self.docs("ACHIEVEMENT"):
            achievement = self.add(models.Achievement(
                id=to_uuid(doc["id"]), deleted=doc["deleted"],
                owner_id=to_uuid(doc["owner"]), skill_id=to_uuid(doc["skill"])))
            for item in doc.get("activities", []):
                activity = activities.get(to_uuid(item["id"]))
                if activity is None or to_uuid(activity["achievement"]) != achievement.id:
                    raise ImportFailed("Achievement %s lists an activity that does not belong to it." % achievement.id)

    def import_activities(self):
        live = Counter()
        for doc in self.docs("ACTIVITY"):
            half_deleted = to_uuid(doc["id"]) in self.removed_from_assignment
            if half_deleted:
                self.notes["half deleted activities imported as deleted"] += 1
            activity = self.add(models.Activity(
                id=to_uuid(doc["id"]),
                deleted=doc["deleted"] or half_deleted,
                author_id=to_uuid(doc["author"]),
                timestamp=doc["timestamp"],
                assignment_id=to_uuid(doc["assignment"]),
                achievement_id=to_uuid(doc["achievement"]),
                reward_due_date=doc["rewardDueDate"],
                rewarded=doc.get("rewarded"),
                abuse_reports_count=doc["abuseReportsCount"],
            ))
            for position, block in enumerate(doc.get("contentBlocks", [])):
                is_image = block["typ"] == models.ContentBlock.Type.IMAGE
                if block["typ"] == models.ContentBlock.Type.VIDEO:
                    self.copy_video_thumbnail(block["value"]["resource"])
                self.add(models.ContentBlock(
                    id=to_uuid(block["id"]), deleted=block["deleted"], activity=activity, position=position,
                    typ=block["typ"],
                    value=self.picture(block["value"]) if is_image else self.text(block["value"]),
                    authentic=block.get("authentic")))
            for rating in doc.get("ratings", []):
                self.add(models.Rating(
                    id=to_uuid(rating["id"]), deleted=rating["deleted"], activity=activity,
                    author_id=to_uuid(rating["author"]), timestamp=rating["timestamp"],
                    positive=rating["positive"]))
            self.import_abuse_reports(doc, activity=activity)
            live[activity.achievement_id] += not activity.deleted

        for achievement in self.objects.get(models.Achievement, []):
            if not achievement.deleted and not live[achievement.id]:
                self.notes["achievements without activities imported as deleted"] += 1
                achievement.deleted = True

    def import_subscriptions(self):
        for doc in self.docs("SUBSCRIPTION"):
            self.add(models.Subscription(
                id=to_uuid(doc["id"]), deleted=doc["deleted"], timestamp=doc["timestamp"],
                account_id=to_uuid(doc["account"]), assignment_id=to_uuid(doc["assignment"])))

    def import_notifications(self):
        for doc in self.docs("NOTIFICATION"):
            self.add(models.Notification(
                id=to_uuid(doc["id"]), deleted=doc["deleted"], typ=doc["typ"],
                account_id=to_uuid(doc["account"]), timestamp=doc["timestamp"],
                emailed=doc.get("email"), html=doc["html"], values=list(doc.get("values") or [])))

    def import_announcements(self):
        for doc in self.docs("ANNOUNCEMENT"):
            self.add(models.Announcement(
                id=to_uuid(doc["id"]), deleted=doc["deleted"], show_time=doc["showTime"],
                hide_time=doc.get("hideTime"), text=doc["text"], maintenance=doc.get("maintenance", False)))

    #
    # uploaded pictures
    #

    def import_files(self):
        """Writes the uploaded pictures that a resource refers to into the media directory."""
        referenced = set()
        for resource in self.objects.get(models.Resource, []):
            for field in PICTURE_FIELDS:
                match = DYNAMIC_ID.match(getattr(resource, field) or "")
                if match:
                    referenced.add(match.group(1))

        chunks = {}
        for chunk in self.docs("fs.chunks"):
            chunks.setdefault(chunk["files_id"], {})[chunk["n"]] = chunk["data"].data

        found = set()
        for file in self.docs("fs.files"):
            # The name is "<id of the picture>-<size>"
            picture_id, part = file["filename"][:36], file["filename"][37:]
            if picture_id not in referenced:
                self.notes["uploaded files that nothing refers to (not imported)"] += 1
                continue
            parts = chunks.get(file["_id"], {})
            data = b"".join(parts[n] for n in sorted(parts))
            if sorted(parts) != list(range(len(parts))) or len(data) != file["length"]:
                raise ImportFailed("The uploaded file %s is incomplete." % file["filename"])
            content_type = file["metadata"]["type"]
            if content_type not in FILE_EXTENSIONS or not part:
                raise ImportFailed("The uploaded file %s has an unexpected type or name." % file["filename"])
            target = self.media_root / "dynamic" / picture_id / (part + FILE_EXTENSIONS[content_type])
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            found.add(picture_id)
            self.notes["uploaded files written"] += 1

        if referenced - found:
            raise ImportFailed("%d uploaded pictures are referenced but missing." % len(referenced - found))

    #
    # run
    #

    def run(self):
        """Imports everything. Returns {model name: number of rows} and the notes."""
        if models.Person.objects.exists():
            raise ImportFailed("The database already contains data. The import needs an empty database.")

        self.import_cities()
        self.import_accounts()
        self.import_persons()
        self.import_skills()
        self.import_assignments()
        self.import_achievements()
        self.import_activities()
        self.import_subscriptions()
        self.import_notifications()
        self.import_announcements()
        self.import_files()

        # One transaction: the references between the rows are checked when it commits
        with transaction.atomic():
            for model, objects in self.objects.items():
                check_lengths(objects)
                model.objects.bulk_create(objects)

        counts = {model.__name__: model.objects.count() for model in self.objects}
        return counts, dict(self.notes)


def is_external(value):
    return bool(value) and value.startswith(("http://", "https://"))


def check_lengths(objects):
    for obj in objects:
        for field in obj._meta.concrete_fields:
            value = getattr(obj, field.attname)
            if field.max_length and isinstance(value, str) and len(value) > field.max_length:
                raise ImportFailed("%s.%s of %s is longer than %d characters." % (
                    type(obj).__name__, field.name, obj.pk, field.max_length))
