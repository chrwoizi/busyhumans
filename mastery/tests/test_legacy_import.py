import datetime
import gzip
import json
import struct
import tempfile
import uuid
from pathlib import Path

from django.test import TestCase

from mastery import models
from mastery.legacy.archive import (
    MAGIC, TERMINATOR, UUID_LEGACY, UUID_STANDARD, Binary, ObjectId, load_collections, to_uuid)
from mastery.legacy.importer import PLACEHOLDER, Importer, ImportFailed

NOW = datetime.datetime(2012, 7, 14, 12, 0, tzinfo=datetime.UTC)


def encode(doc):
    """Encodes a dict (or a list, as a BSON array) the way MongoDB stores it."""
    items = enumerate(doc) if isinstance(doc, list) else doc.items()
    body = b""
    for key, value in items:
        name = str(key).encode("utf-8") + b"\x00"
        if value is None:
            body += b"\x0a" + name
        elif isinstance(value, bool):
            body += b"\x08" + name + (b"\x01" if value else b"\x00")
        elif isinstance(value, int):
            body += b"\x10" + name + struct.pack("<i", value)
        elif isinstance(value, float):
            body += b"\x01" + name + struct.pack("<d", value)
        elif isinstance(value, str):
            data = value.encode("utf-8") + b"\x00"
            body += b"\x02" + name + struct.pack("<i", len(data)) + data
        elif isinstance(value, datetime.datetime):
            body += b"\x09" + name + struct.pack("<q", int(value.timestamp() * 1000))
        elif isinstance(value, Binary):
            body += b"\x05" + name + struct.pack("<i", len(value.data)) + bytes([value.subtype]) + value.data
        elif isinstance(value, ObjectId):
            body += b"\x07" + name + value.data
        elif isinstance(value, dict):
            body += b"\x03" + name + encode(value)
        elif isinstance(value, list):
            body += b"\x04" + name + encode(value)
        else:
            raise TypeError(type(value))
    return struct.pack("<i", len(body) + 5) + body + b"\x00"


def write_archive(path, collections):
    data = struct.pack("<I", MAGIC) + encode({"version": "0.1"}) + TERMINATOR
    for name, docs in collections.items():
        data += encode({"db": "mastery", "collection": name, "EOF": False})
        data += b"".join(encode(doc) for doc in docs) + TERMINATOR
        data += encode({"db": "mastery", "collection": name, "EOF": True}) + TERMINATOR
    Path(path).write_bytes(gzip.compress(data))


def new_id():
    return Binary(UUID_STANDARD, uuid.uuid4().bytes)


def legacy_pair():
    """An id in the legacy format and the standard id that the old application read it as."""
    legacy = Binary(UUID_LEGACY, uuid.uuid4().bytes)
    return legacy, Binary(UUID_STANDARD, to_uuid(legacy).bytes)


def resource(value, **extra):
    doc = {"id": new_id(), "deleted": False, "resource": value, "small": None, "medium": None, "large": None,
           "hires": None, "authorName": "Somebody", "authorUrl": "http://example.com/somebody", "license": 10}
    doc.update(extra)
    return doc


class ArchiveTest(TestCase):

    def test_standard_id_is_read_as_it_is(self):
        value = uuid.uuid4()
        self.assertEqual(to_uuid(Binary(UUID_STANDARD, value.bytes)), value)

    def test_legacy_id_is_hashed_like_the_old_application_did(self):
        # java.util.UUID.nameUUIDFromBytes("hello".getBytes())
        self.assertEqual(to_uuid(Binary(UUID_LEGACY, b"hello")), uuid.UUID("5d41402a-bc4b-3a76-b971-9d911017c592"))

    def test_archive_round_trip(self):
        doc = {"id": new_id(), "name": "Jörg", "xp": 12, "timezone": 2.0, "founder": None, "deleted": False,
               "timestamp": NOW, "picture": {"resource": "static/x.png"}, "values": ["a", "b"],
               "_id": ObjectId(b"123456789012")}
        with tempfile.TemporaryDirectory() as tmp:
            write_archive(Path(tmp) / "dump.gz", {"PERSON": [doc, doc], "CITY": []})
            collections = load_collections(Path(tmp) / "dump.gz")
        self.assertEqual(collections, {"PERSON": [doc, doc]})


class ImporterTest(TestCase):

    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.media = Path(tmp.name) / "media"
        self.external = Path(tmp.name) / "external"
        self.external.mkdir()

        self.flickr = "http://farm1.static.flickr.com/1/photo.jpg"
        self.flickr_small = "http://farm1.static.flickr.com/1/photo_s.jpg"
        self.flickr_gone = "http://farm2.static.flickr.com/2/gone.jpg"
        self.facebook = "https://graph.facebook.com/123/picture"
        (self.external / "abc.jpg").write_bytes(b"\xff\xd8picture\xff\xd9")
        (self.external / "manifest.json").write_text(json.dumps({
            self.flickr: {"status": "ok", "file": "abc.jpg"},
            self.flickr_small: {"status": "http-404"},
            self.flickr_gone: {"status": "http-410"},
            self.facebook: {"status": "placeholder"},
            "http://i2.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg": {"status": "ok", "file": "abc.jpg"},
        }))

        # The old application wrote the achievements in the legacy format and everything else in the
        # standard format. Whatever an achievement refers to has the hashed id.
        person_legacy, self.person_id = legacy_pair()
        skill_legacy, self.skill_id = legacy_pair()
        activity_legacy, self.activity_id = legacy_pair()
        achievement_legacy, self.achievement_id = legacy_pair()
        self.account_id, self.assignment_id = new_id(), new_id()
        self.person, self.skill, self.activity = (
            to_uuid(self.person_id), to_uuid(self.skill_id), to_uuid(self.activity_id))
        self.upload_id = str(uuid.uuid4())
        upload = "mastery/res/dynamic/" + self.upload_id
        file_id, orphan_id = ObjectId(b"a" * 12), ObjectId(b"b" * 12)

        self.collections = {
            "ACCOUNT": [{
                "id": self.account_id, "deleted": False, "email": "old@example.com", "psid": "cookie-token",
                "anonCredential": {"id": new_id(), "deleted": False, "username": "old@example.com",
                                   "accessToken": "password-hash", "accessTokenSecret": "salt"},
                "facebookCredential": {"id": new_id(), "deleted": False, "username": "123", "accessToken": "token"},
                "joindate": NOW, "lastLogin": NOW, "confirmedTosVersion": 1342275766000, "person": self.person_id,
                "cloak": None, "admin": True, "banned": False, "lastAnnouncement": None,
                "preferences": {"id": new_id(), "deleted": False, "subscribeOwnAssignments": True,
                                "notifyOtherActivity": False, "notifyActivityReward": True,
                                "notifyOtherActivityReward": True},
            }],
            "PERSON": [{
                "id": self.person_id, "deleted": False, "account": self.account_id, "registrationTimestamp": NOW,
                "name": "Old User", "picture": resource(self.facebook, small=self.facebook, license=30),
                "gender": "female", "birthday": None, "city": None, "locale": "en_US", "timezone": 2.0, "xp": 250,
                "founder": True,
                "contacts": [{"id": new_id(), "deleted": False, "timestamp": NOW, "typ": "facebook",
                              "value": "http://facebook.com/old.user"}],
            }],
            "SKILL": [
                {"id": self.skill_id, "deleted": False, "author": self.person_id, "creationTimestamp": NOW,
                 "title": "Juggling", "description": resource("http://example.com is where I learned it", license=40),
                 "picture": resource(self.flickr, small=self.flickr_small, large=self.flickr),
                 "abuseReports": [], "abuseReportsCount": 0},
                {"id": new_id(), "deleted": False, "author": self.person_id, "creationTimestamp": NOW,
                 "title": "Knitting", "description": resource("Loops of yarn", license=40),
                 "picture": resource(self.flickr_gone, small=self.flickr_gone),
                 "abuseReports": [{"id": new_id(), "deleted": False, "author": self.person_id, "timestamp": NOW}],
                 "abuseReportsCount": 1},
            ],
            "ASSIGNMENT": [{
                "id": self.assignment_id, "deleted": False, "creationTimestamp": NOW, "author": self.person_id,
                "skill": self.skill_id, "title": "Juggle three balls", "description": "For ten seconds",
                "rewards": [{"id": new_id(), "deleted": False, "timestamp": NOW, "donatingPerson": self.person_id,
                             "amount": 50}],
                "rewardsSum": 50, "lastActivity": NOW, "abuseReports": [], "abuseReportsCount": 0,
                "activities": [{"id": new_id(), "deleted": False, "person": self.person_id,
                                "activity": self.activity_id}],
            }],
            "ACHIEVEMENT": [{
                "id": achievement_legacy, "deleted": False, "owner": person_legacy, "skill": skill_legacy,
                "activities": [{"id": activity_legacy, "deleted": False}],
            }],
            "ACTIVITY": [{
                "id": self.activity_id, "deleted": False, "author": self.person_id, "timestamp": NOW,
                "assignment": self.assignment_id, "achievement": self.achievement_id,
                "rewardDueDate": NOW, "rewarded": 55, "abuseReports": [], "abuseReportsCount": 0,
                "contentBlocks": [
                    {"id": new_id(), "deleted": False, "typ": 0, "authentic": None,
                     "value": resource("I did it", license=40)},
                    {"id": new_id(), "deleted": False, "typ": 1, "authentic": True,
                     "value": resource(upload, small=upload + "/small")},
                    {"id": new_id(), "deleted": False, "typ": 2, "authentic": False,
                     "value": resource("dQw4w9WgXcQ", license=20)},
                ],
                "ratings": [{"id": new_id(), "deleted": False, "author": self.person_id, "timestamp": NOW,
                             "positive": True}],
            }],
            "SUBSCRIPTION": [{"id": new_id(), "deleted": False, "timestamp": NOW, "account": self.account_id,
                              "assignment": self.assignment_id}],
            "NOTIFICATION": [{"id": new_id(), "deleted": False, "typ": 20, "account": self.account_id,
                              "timestamp": NOW, "email": NOW, "html": "55 XP", "values": ["55", "x", "y"]}],
            "ANNOUNCEMENT": [{"id": new_id(), "deleted": True, "showTime": NOW, "hideTime": NOW, "text": "Hello",
                              "maintenance": False}],
            "fs.files": [
                {"_id": file_id, "filename": self.upload_id + "-small", "length": 6, "metadata": {"type": "image/jpeg"}},
                {"_id": orphan_id, "filename": str(uuid.uuid4()) + "-small", "length": 2,
                 "metadata": {"type": "image/jpeg"}},
            ],
            "fs.chunks": [
                {"files_id": file_id, "n": 1, "data": Binary(0, b"def")},
                {"files_id": file_id, "n": 0, "data": Binary(0, b"abc")},
                {"files_id": orphan_id, "n": 0, "data": Binary(0, b"zz")},
            ],
        }

    def run_import(self):
        return Importer(self.collections, self.media, self.external).run()

    def test_accounts_become_legacy_without_any_login(self):
        self.run_import()
        account = models.Account.objects.get()
        self.assertTrue(account.legacy)
        self.assertIsNone(account.user)
        self.assertEqual(models.User.objects.count(), 0)
        self.assertEqual(account.email, "old@example.com")
        self.assertEqual(account.person_id, self.person)
        self.assertFalse(account.notify_other_activity)
        self.assertEqual(account.confirmed_tos_version, 1342275766000)
        # Nothing of the credentials is stored anywhere
        stored = json.dumps(list(models.Account.objects.values()), default=str)
        for secret in ("password-hash", "salt", "cookie-token", "token"):
            self.assertNotIn(secret, stored)

    def test_personal_data_that_is_not_about_login_is_kept(self):
        self.run_import()
        person = models.Person.objects.get()
        self.assertEqual((person.name, person.gender, person.locale, person.timezone, person.xp, person.founder),
                         ("Old User", "female", "en_US", 2.0, 250, True))
        self.assertEqual(person.account, models.Account.objects.get())
        self.assertEqual(person.contacts.get().value, "http://facebook.com/old.user")

    def test_profile_picture_of_facebook_gets_the_placeholder(self):
        self.run_import()
        picture = models.Person.objects.get().picture
        self.assertEqual(picture.resource, PLACEHOLDER)
        self.assertIsNone(picture.small)
        self.assertEqual(picture.license, models.LicenseType.INTERNAL_PICTURE)
        self.assertEqual(models.MirroredFile.objects.get(url=self.facebook).status, "placeholder")

    def test_hotlinked_picture_points_to_the_local_copy(self):
        self.run_import()
        skill = models.Skill.objects.get(title="Juggling")
        self.assertEqual(skill.picture.resource, "mastery/res/mirrored/abc.jpg")
        self.assertEqual(skill.picture.large, "mastery/res/mirrored/abc.jpg")
        # The small size no longer exists, so the nearest existing size takes its place
        self.assertEqual(skill.picture.small, "mastery/res/mirrored/abc.jpg")
        self.assertIsNone(skill.picture.hires)
        # Author and license of the copy stay
        self.assertEqual((skill.picture.author_name, skill.picture.license), ("Somebody", 10))
        self.assertEqual((self.media / "mirrored" / "abc.jpg").read_bytes(), b"\xff\xd8picture\xff\xd9")
        self.assertEqual(models.MirroredFile.objects.get(url=self.flickr).path, "mirrored/abc.jpg")

    def test_hotlinked_picture_that_is_gone_gets_the_placeholder(self):
        self.run_import()
        skill = models.Skill.objects.get(title="Knitting")
        self.assertEqual(skill.picture.resource, PLACEHOLDER)
        self.assertEqual(skill.picture.author_name, "busyhumans.com")
        self.assertEqual(models.MirroredFile.objects.get(url=self.flickr_gone).status, "http-410")
        self.assertEqual(skill.abuse_reports.count(), 1)

    def test_text_that_starts_with_a_url_is_not_treated_as_a_picture(self):
        self.run_import()
        skill = models.Skill.objects.get(title="Juggling")
        self.assertEqual(skill.description.resource, "http://example.com is where I learned it")

    def test_uploaded_pictures_are_written_to_the_media_directory(self):
        counts, notes = self.run_import()
        self.assertEqual((self.media / "dynamic" / self.upload_id / "small.jpg").read_bytes(), b"abcdef")
        self.assertEqual(notes["uploaded files written"], 1)
        self.assertEqual(notes["uploaded files that nothing refers to (not imported)"], 1)
        self.assertEqual(len(list((self.media / "dynamic").iterdir())), 1)

    def test_activity_with_its_parts(self):
        counts, notes = self.run_import()
        activity = models.Activity.objects.get()
        self.assertEqual(activity.assignment.title, "Juggle three balls")
        self.assertEqual(activity.rewarded, 55)
        self.assertEqual([block.typ for block in activity.content_blocks.all()], [0, 1, 2])
        # The preview picture of the video is a local file. The video itself stays at YouTube.
        self.assertEqual(activity.content_blocks.last().value.resource, "dQw4w9WgXcQ")
        self.assertTrue((self.media / "youtube" / "dQw4w9WgXcQ.jpg").exists())
        self.assertEqual(activity.content_blocks.first().value.resource, "I did it")
        self.assertTrue(activity.ratings.get().positive)
        self.assertEqual(activity.assignment.rewards.get().amount, 50)
        self.assertEqual(counts["Announcement"], 1)
        self.assertTrue(models.Announcement.objects.get().deleted)
        self.assertEqual(models.Notification.objects.get().values, ["55", "x", "y"])
        self.assertEqual(models.Subscription.objects.get().account, models.Account.objects.get())

    def test_achievement_in_the_legacy_format_finds_its_owner_skill_and_activities(self):
        self.run_import()
        achievement = models.Achievement.objects.get()
        self.assertEqual(achievement.id, to_uuid(self.achievement_id))
        self.assertEqual(achievement.owner_id, self.person)
        self.assertEqual(achievement.skill_id, self.skill)
        self.assertEqual([activity.id for activity in achievement.activities.all()], [self.activity])
        self.assertFalse(achievement.deleted)

    def test_activity_that_was_only_removed_from_its_assignment_is_deleted_with_its_achievement(self):
        self.collections["ASSIGNMENT"][0]["activities"][0]["deleted"] = True
        counts, notes = self.run_import()
        self.assertTrue(models.Activity.objects.get().deleted)
        self.assertTrue(models.Achievement.objects.get().deleted)
        self.assertEqual(notes["half deleted activities imported as deleted"], 1)
        self.assertEqual(notes["achievements without activities imported as deleted"], 1)

    def test_import_needs_an_empty_database(self):
        self.run_import()
        with self.assertRaises(ImportFailed):
            Importer(self.collections, self.media, self.external).run()

    def test_assignment_that_lists_a_foreign_activity_fails(self):
        self.collections["ASSIGNMENT"][0]["activities"][0]["person"] = new_id()
        with self.assertRaises(ImportFailed):
            self.run_import()
        self.assertEqual(models.Person.objects.count(), 0)

    def test_incomplete_uploaded_file_fails(self):
        del self.collections["fs.chunks"][0]
        with self.assertRaises(ImportFailed):
            self.run_import()
        self.assertEqual(models.Person.objects.count(), 0)

    def test_without_copies_every_hotlinked_picture_gets_the_placeholder(self):
        self.external = None
        self.run_import()
        self.assertEqual(models.Skill.objects.get(title="Juggling").picture.resource, PLACEHOLDER)
        self.assertEqual(models.MirroredFile.objects.get(url=self.flickr).status, "not-copied")
