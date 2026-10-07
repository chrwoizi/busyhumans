import datetime
import re
import tempfile

from django.core import mail
from django.core.cache import cache
from django.test import Client, TestCase, override_settings
from django.utils import timezone

from mastery import models, notifications, search
from mastery.tests import helpers
from mastery.tests.helpers import act


class SiteTestCase(TestCase):

    def setUp(self):
        search.index.clear()
        cache.clear()
        # Windows keeps served files open for a moment
        media = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.addCleanup(media.cleanup)
        settings = override_settings(MEDIA_ROOT=media.name, BASE_URL="https://busy.example/")
        settings.enable()
        self.addCleanup(settings.disable)

    def login(self, person):
        client = Client()
        client.force_login(person.account.user)
        return client

    def page(self, client, token, **params):
        return client.get("/page", {"token": token, **params})


class PublicPagesTest(SiteTestCase):
    """What visitors see. Login is switched off, as on the live site."""

    def setUp(self):
        super().setUp()
        self.old = helpers.legacy_member("Old Timer", xp=1448)
        self.other = helpers.legacy_member("Second Person", email=None, xp=300)
        self.skill = helpers.skill(self.old, "Juggling")
        self.assignment = helpers.assignment(self.old, self.skill, "Juggle three balls", reward=131)
        self.activity = helpers.activity(self.other, self.assignment, "Look <b>here</b> http://example.com", rewarded=66)
        helpers.rating(self.activity, self.old)

    def test_shell_says_that_login_is_disabled(self):
        response = self.client.get("/")
        self.assertContains(response, "Login is disabled")
        self.assertNotContains(response, "login-menu-button")
        self.assertContains(response, 'id="search"')
        self.assertContains(response, "Juggling")
        self.assertContains(response, "1448 XP - Level 4")

    def test_security_headers(self):
        response = self.client.get("/")
        self.assertIn("script-src 'self'", response["Content-Security-Policy"])
        self.assertIn("frame-ancestors 'none'", response["Content-Security-Policy"])
        self.assertEqual(response["X-Frame-Options"], "DENY")
        self.assertEqual(response["X-Content-Type-Options"], "nosniff")
        self.assertEqual(response["Referrer-Policy"], "same-origin")

    def test_health_check_needs_no_host_name(self):
        self.assertEqual(Client(HTTP_HOST="10.1.2.3:8080").get("/healthz").status_code, 200)
        self.assertEqual(Client(HTTP_HOST="10.1.2.3:8080").get("/").status_code, 400)

    def test_list_of_assignments(self):
        response = self.client.get("/list/assignments", {"kind": "active", "sort": "ACTIVITY"})
        self.assertEqual(response["X-Count"], "1")
        self.assertContains(response, "#assignment=%s" % self.assignment.id)
        self.assertContains(response, "131 XP")
        self.assertContains(response, "1 activity")
        self.assertEqual(self.client.get("/list/assignments", {"kind": "active", "offset": 10})["X-Count"], "0")
        self.assertEqual(self.client.get("/list/assignments", {"kind": "nonsense"}).status_code, 400)

    def test_sort_orders(self):
        newer = helpers.assignment(self.old, self.skill, "Newer one", reward=50, days_ago=1)

        def order(sort):
            html = self.client.get("/list/assignments", {"kind": "active", "sort": sort}).content.decode()
            return re.findall(r'data-id="([0-9a-f-]+)"', html)

        self.assertEqual(order("NEWEST"), [str(newer.id), str(self.assignment.id)])
        self.assertEqual(order("REWARD"), [str(self.assignment.id), str(newer.id)])
        # The one without any activity comes last
        self.assertEqual(order("ACTIVITY"), [str(self.assignment.id), str(newer.id)])

    def test_assignment_page_escapes_what_users_wrote(self):
        response = self.page(self.client, "assignment=%s" % self.assignment.id)
        self.assertContains(response, "Look &lt;b&gt;here&lt;/b&gt; <a href=\"http://example.com\"")
        self.assertNotContains(response, "<b>here</b>")
        self.assertContains(response, "Gained 66 XP for this activity.")
        self.assertContains(response, 'title="1 likes and 0 dislikes"')
        self.assertContains(response, "Please login to add your activity.")
        self.assertNotContains(response, "new-activity")

    def test_unknown_things_do_not_exist(self):
        missing = "11111111-2222-3333-4444-555555555555"
        self.assertContains(self.page(self.client, "assignment=" + missing), "The requested assignment does not exist.")
        self.assertContains(self.page(self.client, "assignment=<script>"), "The requested assignment does not exist.")
        self.assertContains(self.page(self.client, "person=" + missing), "The requested person does not exist.")
        self.assertContains(self.page(self.client, "category=" + missing), "The requested category does not exist.")
        self.assertContains(self.page(self.client, "person=me"), "The requested person does not exist.")

    def test_deleted_and_reported_content_is_hidden(self):
        models.Assignment.objects.filter(id=self.assignment.id).update(abuse_reports_count=5)
        self.assertContains(self.page(self.client, "assignment=%s" % self.assignment.id), "does not exist")
        self.assertNotContains(self.client.get("/list/assignments", {"kind": "active"}), "Juggle three balls")
        models.Assignment.objects.filter(id=self.assignment.id).update(abuse_reports_count=0, deleted=True)
        self.assertContains(self.page(self.client, "assignment=%s" % self.assignment.id), "does not exist")

    def test_person_page_and_its_lists(self):
        response = self.page(self.client, "person=%s" % self.other.id)
        self.assertContains(response, "Second Person")
        self.assertContains(response, "14 years ago")
        completed = self.client.get("/list/assignments", {"kind": "completed", "person": str(self.other.id)})
        self.assertContains(completed, "Juggle three balls")
        created = self.client.get("/list/assignments", {"kind": "created", "person": str(self.other.id)})
        self.assertEqual(created["X-Count"], "0")
        achievements = self.client.get("/list/achievements", {"person": str(self.other.id)})
        self.assertContains(achievements, "#category=%s" % self.skill.id)
        self.assertContains(achievements, "Juggle three balls")

    def test_category_page(self):
        response = self.page(self.client, "category=%s" % self.skill.id)
        self.assertContains(response, "Throwing things")
        self.assertContains(self.client.get("/list/assignments", {"kind": "skill", "skill": str(self.skill.id)}),
                            "Juggle three balls")

    def test_static_pages(self):
        self.assertContains(self.page(self.client, "about"), "Welcome to Busy Humans!")
        self.assertContains(self.page(self.client, "imprint"), "Impressum")
        self.assertContains(self.page(self.client, "legal"), "Nutzungsbedingungen")
        self.assertEqual(self.page(self.client, "about")["X-Menu"], "about")
        # Anything unknown shows the list of assignments
        self.assertEqual(self.page(self.client, "whatever")["X-Menu"], "discover")

    def test_search(self):
        response = self.client.get("/search", {"q": "jugg"})
        self.assertContains(response, "#category=%s" % self.skill.id)
        self.assertContains(response, "#assignment=%s" % self.assignment.id)
        self.assertEqual(response["X-More"], "false")
        with self.settings(ENABLE_SEARCH=False):
            self.assertEqual(self.client.get("/search", {"q": "jugg"}).status_code, 404)

    def test_preview_page_for_shared_links_escapes_the_title(self):
        models.Assignment.objects.filter(id=self.assignment.id).update(title='"><script>alert(1)</script>')
        response = self.client.get("/meta", {"token": "assignment=%s" % self.assignment.id})
        self.assertNotContains(response, "<script>")
        self.assertContains(response, "https://busy.example/#assignment=%s" % self.assignment.id)
        injected = self.client.get("/meta", {"token": '"><script>alert(1)</script>'})
        self.assertNotContains(injected, "<script>")

    def test_nothing_can_be_changed_while_login_is_disabled(self):
        for name in ("login", "register", "rate", "create-assignment", "create-test-person", "create-announcement"):
            response = act(self.client, name, email="old@example.com", password="x")
            self.assertEqual(response.status_code, 403, name)
            self.assertEqual(response.json()["error"], "Login is disabled")
        self.assertEqual(self.client.post("/mastery/upload", {"file": helpers.png(), "register": "true"}).status_code, 403)
        self.assertEqual(models.Person.objects.count(), 2)

    def test_a_session_from_before_does_not_count_while_login_is_disabled(self):
        member = helpers.member("Alice")
        client = self.login(member)
        self.assertContains(client.get("/"), "Login is disabled")
        self.assertContains(self.page(client, "create"), "Please login.")

    def test_pictures_are_served_only_from_their_directories(self):
        from mastery import media
        picture_id = media.save_upload(helpers.png(3000, 1000).read())
        response = self.client.get("/mastery/res/dynamic/%s/small" % picture_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response["Content-Type"], "image/jpeg")
        self.assertIn("immutable", response["Cache-Control"])
        self.assertEqual(self.client.get("/mastery/res/dynamic/%s/nonsense" % picture_id).status_code, 404)
        self.assertEqual(self.client.get("/mastery/res/mirrored/..%2f..%2fbusyhumans.sqlite3").status_code, 404)
        self.assertEqual(self.client.get("/mastery/res/mirrored/" + "a" * 40 + ".jpg").status_code, 404)
        self.assertEqual(self.client.get("/mastery/res/youtube/../../x.jpg").status_code, 404)

    def test_uploaded_pictures_are_scaled_like_before(self):
        from PIL import Image
        from mastery import media
        picture_id = media.save_upload(helpers.png(3000, 1000).read())
        sizes = {size: Image.open(media.dynamic_file(picture_id, size)).size for size in media.SIZES}
        # hires fits into 2000 x 2000, the others fill their square
        self.assertEqual(sizes, {"hires": (2000, 666), "large": (1201, 400), "medium": (360, 120), "small": (150, 50)})

    def test_files_that_are_no_pictures_are_refused(self):
        from mastery import media
        for data in (b"<html><script>alert(1)</script></html>", b"GIF89a" + b"\x00" * 20, b""):
            with self.assertRaises(media.InvalidPicture):
                media.save_upload(data)


@override_settings(ENABLE_LOGIN=True)
class RewardsTest(SiteTestCase):

    def setUp(self):
        super().setUp()
        self.author = helpers.member("Author", xp=0)
        self.doer = helpers.member("Doer", xp=0)
        self.skill = helpers.skill(self.author)
        self.assignment = helpers.assignment(self.author, self.skill, reward=100)

    def test_looking_at_an_assignment_pays_out_what_is_due(self):
        activity = helpers.activity(self.doer, self.assignment, days_ago=4)
        helpers.rating(activity, self.author, positive=True)
        helpers.rating(activity, helpers.member("Critic"), positive=False)

        self.page(self.client, "assignment=%s" % self.assignment.id)

        activity.refresh_from_db()
        self.assertEqual(activity.rewarded, 50)
        self.assertEqual(models.Person.objects.get(id=self.doer.id).xp, 50)
        # The author of the assignment gets a tenth
        self.assertEqual(models.Person.objects.get(id=self.author.id).xp, 5)
        kinds = sorted(models.Notification.objects.values_list("typ", "account_id"))
        self.assertEqual(kinds, sorted([(20, self.doer.account_id), (30, self.author.account_id)]))

        # Only once
        self.page(self.client, "assignment=%s" % self.assignment.id)
        self.assertEqual(models.Person.objects.get(id=self.doer.id).xp, 50)

    def test_nothing_is_paid_before_the_rating_period_ends(self):
        activity = helpers.activity(self.doer, self.assignment, days_ago=1)
        response = self.page(self.client, "assignment=%s" % self.assignment.id)
        activity.refresh_from_db()
        self.assertIsNone(activity.rewarded)
        self.assertContains(response, "Rewards will be issued in 48 hours")
        self.assertContains(self.client.get("/list/assignments", {"kind": "active"}), "1 activity to rate")

    def test_legacy_accounts_get_no_notifications_and_no_email(self):
        old = helpers.legacy_member("Old Timer")
        old_assignment = helpers.assignment(old, self.skill, "Old assignment", reward=100)
        models.Subscription.objects.create(account=old.account, assignment=old_assignment, timestamp=timezone.now())
        helpers.activity(self.doer, old_assignment, days_ago=4)

        self.page(self.client, "assignment=%s" % old_assignment.id)
        notifications.create_other_activity(self.doer, old_assignment)

        self.assertEqual(models.Person.objects.get(id=old.id).xp, 5)
        self.assertFalse(models.Notification.objects.filter(account=old.account).exists())
        # Even a notification that got there some other way is never sent
        models.Notification.objects.create(
            account=old.account, typ=20, timestamp=timezone.now(), html="x", values=["5", "a", "b"])
        mail.outbox.clear()
        notifications.send_emails()
        self.assertEqual([message.to for message in mail.outbox], [[self.doer.account.user.email]])

    def test_notification_email(self):
        activity = helpers.activity(self.doer, self.assignment, days_ago=4)
        self.page(self.client, "assignment=%s" % self.assignment.id)
        mail.outbox.clear()
        self.assertEqual(notifications.send_emails(), 2)
        message = [m for m in mail.outbox if m.to == [self.doer.account.user.email]][0]
        html = message.alternatives[0][0]
        self.assertIn("gained 50 XP", html)
        self.assertIn('href="https://busy.example/#assignment=%s"' % self.assignment.id, html)
        self.assertIn("level 1 and 0th place", html)
        # Sent once
        mail.outbox.clear()
        self.assertEqual(notifications.send_emails(), 0)

    def test_preferences_switch_notifications_off(self):
        models.Account.objects.filter(id=self.doer.account_id).update(notify_activity_reward=False)
        helpers.activity(self.doer, self.assignment, days_ago=4)
        self.page(self.client, "assignment=%s" % self.assignment.id)
        mail.outbox.clear()
        notifications.send_emails()
        self.assertEqual([message.to for message in mail.outbox], [[self.author.account.user.email]])


@override_settings(ENABLE_LOGIN=True)
class MemberActionsTest(SiteTestCase):

    def setUp(self):
        super().setUp()
        self.alice = helpers.member("Alice", xp=1448)
        self.bob = helpers.member("Bob")
        self.skill = helpers.skill(self.alice)
        self.assignment = helpers.assignment(self.alice, self.skill, reward=131)
        self.as_alice = self.login(self.alice)
        self.as_bob = self.login(self.bob)

    def test_requests_without_the_csrf_token_are_refused(self):
        client = Client(enforce_csrf_checks=True)
        client.force_login(self.bob.account.user)
        self.assertEqual(act(client, "rate", activity="x", value=1).status_code, 403)

    def test_visitors_cannot_do_anything(self):
        activity = helpers.activity(self.bob, self.assignment)
        for name, data in (("rate", {"activity": str(activity.id), "value": 1}),
                           ("boost", {"assignment": str(self.assignment.id), "value": 1}),
                           ("create-assignment", {"title": "New thing", "skill": str(self.skill.id)}),
                           ("delete-activity", {"activity": str(activity.id)}),
                           ("create-test-person", {"name": "Test"}),
                           ("set-cloak", {"person": str(self.bob.id)})):
            response = act(self.client, name, **data)
            self.assertEqual(response.status_code, 400, name)
        self.assertEqual(models.Rating.objects.count(), 0)
        self.assertEqual(models.Person.objects.count(), 2)

    def test_members_who_did_not_agree_to_the_terms_see_them_first(self):
        carol = helpers.member("Carol", tos=False)
        client = self.login(carol)
        self.assertContains(self.page(client, "discover"), "Zustimmen")
        self.assertEqual(act(client, "boost", assignment=str(self.assignment.id), value=1).status_code, 400)
        self.assertEqual(act(client, "confirm-tos").status_code, 200)
        self.assertContains(self.page(client, "discover"), "Available assignments:")
        self.assertEqual(act(client, "boost", assignment=str(self.assignment.id), value=1).status_code, 200)

    def test_create_assignment_in_an_existing_category(self):
        response = act(self.as_bob, "create-assignment", title="  balance a broom ", description="on your hand",
                       skill=str(self.skill.id))
        created = models.Assignment.objects.get(id=response.json()["id"])
        self.assertEqual((created.title, created.description, created.author), ("Balance a broom", "On your hand", self.bob))
        # A person of level 1 puts 50 XP on a new assignment
        self.assertEqual(created.rewards_sum, 50)
        self.assertEqual(created.rewards.get().amount, 50)
        # Authors subscribe to their own assignments
        self.assertTrue(models.Subscription.objects.filter(account=self.bob.account, assignment=created).exists())
        self.assertContains(self.as_bob.get("/search", {"q": "broom"}), "Balance a broom")

    def test_assignment_titles_are_unique(self):
        response = act(self.as_bob, "create-assignment", title="JUGGLE THREE BALLS", skill=str(self.skill.id))
        self.assertEqual(response.status_code, 400)
        self.assertTrue(act(self.as_bob, "assignment-exists", title="juggle three balls").json()["exists"])
        self.assertFalse(act(self.as_bob, "assignment-exists", title="juggle.*").json()["exists"])

    def test_create_assignment_with_a_new_category_and_an_uploaded_picture(self):
        picture = self.as_bob.post("/mastery/upload", {"file": helpers.png()}).json()["id"]
        response = act(self.as_bob, "create-assignment", title="Knit a scarf", description="",
                       newSkill={"title": "knitting", "description": "loops of yarn", "wikipedia": True,
                                 "picture": picture})
        created = models.Assignment.objects.get(id=response.json()["id"])
        self.assertEqual(created.skill.title, "Knitting")
        self.assertEqual(created.skill.description.license, models.LicenseType.WIKIPEDIA)
        self.assertEqual(created.skill.picture.medium, "mastery/res/dynamic/%s/medium" % picture)
        self.assertEqual(created.skill.picture.license, models.LicenseType.CC_BY_30)
        self.assertEqual(self.client.get("/" + created.skill.picture.medium).status_code, 200)

    def test_pictures_of_somebody_else_cannot_be_used(self):
        picture = self.as_alice.post("/mastery/upload", {"file": helpers.png()}).json()["id"]
        response = act(self.as_bob, "create-assignment", title="Knit a scarf",
                       newSkill={"title": "Knitting", "description": "", "picture": picture})
        self.assertEqual(response.status_code, 400)
        self.assertFalse(models.Skill.objects.filter(title="Knitting").exists())

    def test_upload_checks_what_the_file_really_is(self):
        import io
        fake = io.BytesIO(b"<script>alert(1)</script>")
        fake.name = "evil.png"
        response = self.as_bob.post("/mastery/upload", {"file": fake})
        self.assertEqual(response.status_code, 400)

    def test_post_an_activity_with_text_picture_and_video(self):
        picture = self.as_bob.post("/mastery/upload", {"file": helpers.png()}).json()["id"]
        self.assertContains(self.page(self.as_bob, "assignment=%s" % self.assignment.id), "new-activity")
        from unittest import mock
        with mock.patch("mastery.media.fetch_youtube_thumbnail", return_value=False):
            response = act(self.as_bob, "create-activity", assignment=str(self.assignment.id), text="did it",
                           blocks=[{"typ": 1, "value": picture}, {"typ": 2, "value": "dQw4w9WgXcQ"}])
        self.assertEqual(response.status_code, 200, response.content)
        activity = models.Activity.objects.get()
        self.assertEqual([block.typ for block in activity.content_blocks.all()], [0, 1, 2])
        self.assertEqual(activity.content_blocks.first().value.resource, "Did it")
        self.assertEqual(activity.reward_due_date - activity.timestamp, datetime.timedelta(days=3))
        self.assertEqual(models.Assignment.objects.get(id=self.assignment.id).last_activity, activity.timestamp)
        self.assertEqual(models.Achievement.objects.get().owner, self.bob)
        # Alice subscribed to nothing, so nobody is notified
        page = self.page(self.as_bob, "assignment=%s" % self.assignment.id)
        self.assertNotContains(page, "new-activity")
        self.assertContains(page, "youtube-nocookie.com/embed/dQw4w9WgXcQ")
        # Once per assignment
        again = act(self.as_bob, "create-activity", assignment=str(self.assignment.id), text="again", blocks=[])
        self.assertEqual(again.status_code, 400)

    def test_an_activity_needs_content_and_valid_videos(self):
        self.assertEqual(act(self.as_bob, "create-activity", assignment=str(self.assignment.id), text=" ",
                             blocks=[]).status_code, 400)
        self.assertEqual(act(self.as_bob, "create-activity", assignment=str(self.assignment.id), text="x",
                             blocks=[{"typ": 2, "value": "x\" onload=\"alert(1)"}]).status_code, 400)
        self.assertEqual(models.Activity.objects.count(), 0)

    def test_subscribers_are_told_about_new_activities(self):
        act(self.as_alice, "subscribe", assignment=str(self.assignment.id), subscribed=True)
        act(self.as_bob, "create-activity", assignment=str(self.assignment.id), text="did it", blocks=[])
        notification = models.Notification.objects.get()
        self.assertEqual((notification.account, notification.typ), (self.alice.account, 10))
        self.assertIn("Bob</a> has completed", notification.html)

    def test_rate_and_take_it_back(self):
        activity = helpers.activity(self.bob, self.assignment, days_ago=1)
        act(self.as_alice, "rate", activity=str(activity.id), value=1)
        self.assertContains(self.page(self.as_alice, "assignment=%s" % self.assignment.id), "1 likes and 0 dislikes")
        act(self.as_alice, "rate", activity=str(activity.id), value=-1)
        self.assertContains(self.page(self.as_alice, "assignment=%s" % self.assignment.id), "0 likes and 1 dislikes")
        act(self.as_alice, "rate", activity=str(activity.id), value=0)
        self.assertFalse(models.Rating.objects.filter(deleted=False).exists())
        self.assertEqual(act(self.as_alice, "rate", activity=str(activity.id), value=5).status_code, 400)

    def test_boost_raises_and_lowers_the_reward(self):
        carol = helpers.member("Carol")
        as_carol = self.login(carol)
        act(as_carol, "boost", assignment=str(self.assignment.id), value=1)
        self.assertContains(self.page(as_carol, "assignment=%s" % self.assignment.id), "181 XP")
        self.assertEqual(models.Assignment.objects.get(id=self.assignment.id).rewards_sum, 181)
        act(as_carol, "boost", assignment=str(self.assignment.id), value=-1)
        self.assertContains(self.page(as_carol, "assignment=%s" % self.assignment.id), "81 XP")
        act(as_carol, "boost", assignment=str(self.assignment.id), value=0)
        self.assertEqual(models.Assignment.objects.get(id=self.assignment.id).rewards_sum, 131)

    def test_abuse_reports_hide_content_at_five(self):
        activity = helpers.activity(self.bob, self.assignment)
        for number in range(5):
            reporter = self.login(helpers.member("Reporter %d" % number))
            self.assertEqual(act(reporter, "activity-abuse", activity=str(activity.id), abuse=True).status_code, 200)
            act(reporter, "activity-abuse", activity=str(activity.id), abuse=True)
        activity.refresh_from_db()
        self.assertEqual(activity.abuse_reports_count, 5)
        self.assertNotContains(self.page(self.client, "assignment=%s" % self.assignment.id), "I did it")
        # Authors cannot report their own content, and only admins can clear reports
        self.assertEqual(act(self.as_bob, "activity-abuse", activity=str(activity.id), abuse=True).status_code, 400)
        self.assertEqual(act(self.as_bob, "clear-activity-abuse", activity=str(activity.id)).status_code, 400)

    def test_authors_delete_their_activity_until_it_is_rewarded(self):
        activity = helpers.activity(self.bob, self.assignment, days_ago=1)
        self.assertEqual(act(self.as_alice, "delete-activity", activity=str(activity.id)).status_code, 400)
        self.assertEqual(act(self.as_bob, "delete-activity", activity=str(activity.id)).status_code, 200)
        activity.refresh_from_db()
        self.assertTrue(activity.deleted)
        self.assertTrue(models.Achievement.objects.get().deleted)
        self.assertIsNone(models.Assignment.objects.get(id=self.assignment.id).last_activity)

        rewarded = helpers.activity(self.bob, self.assignment, rewarded=50)
        self.assertEqual(act(self.as_bob, "delete-activity", activity=str(rewarded.id)).status_code, 400)

    def test_authors_delete_their_assignment_while_nobody_completed_it(self):
        self.assertEqual(act(self.as_bob, "delete-assignment", assignment=str(self.assignment.id)).status_code, 400)
        activity = helpers.activity(self.bob, self.assignment)
        self.assertEqual(act(self.as_alice, "delete-assignment", assignment=str(self.assignment.id)).status_code, 400)
        models.Activity.objects.filter(id=activity.id).update(deleted=True)
        self.assertEqual(act(self.as_alice, "delete-assignment", assignment=str(self.assignment.id)).status_code, 200)
        self.assertTrue(models.Assignment.objects.get(id=self.assignment.id).deleted)
        self.assertNotContains(self.client.get("/search", {"q": "juggle"}), "Juggle three balls")

    def test_preferences(self):
        response = act(self.as_bob, "preferences", subscribeOwnAssignments=False, notifyOtherActivity=True,
                       notifyActivityReward=False, notifyOtherActivityReward=True)
        self.assertEqual(response.status_code, 200)
        account = models.Account.objects.get(id=self.bob.account_id)
        self.assertEqual((account.subscribe_own_assignments, account.notify_activity_reward), (False, False))
        self.assertContains(self.page(self.as_bob, "preferences"), "bob@example.com")

    def test_admin_page_and_actions_need_an_admin(self):
        self.assertContains(self.page(self.as_bob, "admin"), "Please login as admin.")
        for name in ("create-announcement", "subscribe-all", "send-notifications", "set-cloak", "delete-skill",
                     "speedup", "create-test-person"):
            self.assertEqual(act(self.as_bob, name, text="x", show=0, skill=str(self.skill.id),
                                 assignment=str(self.assignment.id), days=1, name="Test").status_code, 400, name)
        self.assertFalse(models.Announcement.objects.exists())

    def test_admin(self):
        admin = helpers.member("Admin", admin=True)
        as_admin = self.login(admin)
        self.assertContains(as_admin.get("/"), 'data-menu="admin"')
        self.assertContains(self.page(as_admin, "admin"), "Registered Users:")

        self.assertEqual(act(as_admin, "create-announcement", text="Down {hide}", show=0, hide=600,
                             maintenance=True).status_code, 200)
        announcements = self.client.get("/sync").json()["announcements"]
        self.assertEqual([a["text"] for a in announcements], ["Down {hide}"])

        picture = as_admin.post("/mastery/upload", {"file": helpers.png()}).json()["id"]
        test_person = act(as_admin, "create-test-person", name="Test Person", picture=picture).json()["id"]
        self.assertEqual(act(as_admin, "set-cloak", person=test_person).status_code, 200)
        # The admin now acts as the test person
        act(as_admin, "create-activity", assignment=str(self.assignment.id), text="as somebody else", blocks=[])
        self.assertEqual(str(models.Activity.objects.get().author_id), test_person)
        # Only persons without an account can be impersonated
        self.assertEqual(act(as_admin, "set-cloak", person=str(self.bob.id)).status_code, 400)

        self.assertEqual(act(as_admin, "delete-skill", skill=str(self.skill.id)).status_code, 200)
        self.assertTrue(models.Assignment.objects.get(id=self.assignment.id).deleted)
        self.assertTrue(models.Activity.objects.get().deleted)


@override_settings(ENABLE_LOGIN=True)
class AccountsTest(SiteTestCase):

    def link(self, kind):
        """The token of the link in the last email."""
        return re.search(r"#%s=(\S+)" % kind, mail.outbox[-1].body).group(1)

    def register(self, client=None, **overrides):
        data = {"email": "New@Example.com", "password": helpers.PASSWORD, "name": "New Member", "website": ""}
        data.update(overrides)
        return act(client or self.client, "register", **data)

    def test_register_verify_agree(self):
        self.assertContains(self.client.get("/"), "login-menu-button")
        response = self.register()
        self.assertEqual(response.status_code, 200)
        self.assertIn("sent you an email", response.json()["message"])
        user = models.User.objects.get()
        self.assertEqual((user.email, user.is_active), ("new@example.com", False))
        self.assertTrue(user.password.startswith("argon2"))
        # Not before the address is confirmed
        self.assertEqual(act(self.client, "login", email="new@example.com", password=helpers.PASSWORD).status_code, 400)

        self.assertEqual(act(self.client, "verify", token=self.link("verify")).status_code, 200)
        self.assertContains(self.client.get("/"), "New Member")
        self.assertContains(self.page(self.client, "discover"), "Zustimmen")
        act(self.client, "confirm-tos")
        self.assertContains(self.page(self.client, "create"), "Create a new assignment:")
        account = models.Account.objects.get()
        self.assertFalse(account.legacy)
        self.assertEqual(account.person.name, "New Member")
        self.assertEqual(account.person.picture.resource, "static/default-skill.png")

    def test_a_link_works_once(self):
        self.register()
        token = self.link("verify")
        act(self.client, "verify", token=token)
        self.assertEqual(act(Client(), "verify", token=token).status_code, 400)
        self.assertEqual(act(Client(), "verify", token="1-nonsense").status_code, 400)

    def test_registration_checks_its_input(self):
        for overrides in ({"email": "nonsense"}, {"password": "short"}, {"password": "x" * 65}, {"name": "X"},
                          {"password": "12345678"}):
            self.assertEqual(self.register(**overrides).status_code, 400, overrides)
        self.assertFalse(models.User.objects.exists())

    def test_registering_a_known_address_does_not_reveal_it(self):
        helpers.member("Alice", email="alice@example.com")
        mail.outbox.clear()
        response = self.register(email="alice@example.com")
        self.assertIn("sent you an email", response.json()["message"])
        self.assertEqual(models.User.objects.count(), 1)
        self.assertIn("already have one", mail.outbox[-1].body)

    def test_a_new_account_never_takes_over_a_legacy_account(self):
        old = helpers.legacy_member("Old Timer", email="new@example.com", xp=999)
        self.register()
        act(self.client, "verify", token=self.link("verify"))
        new = models.Account.objects.get(legacy=False)
        self.assertNotEqual(new.person_id, old.id)
        self.assertEqual(new.person.xp, 0)
        self.assertIsNone(models.Account.objects.get(id=old.account_id).user)

    def test_legacy_accounts_cannot_log_in_or_reset_a_password(self):
        helpers.legacy_member("Old Timer", email="old@example.com")
        self.assertEqual(act(self.client, "login", email="old@example.com", password="anything").status_code, 400)
        mail.outbox.clear()
        act(self.client, "request-reset", email="old@example.com")
        self.assertEqual(mail.outbox, [])

    def test_login_logout(self):
        helpers.member("Alice", email="alice@example.com")
        self.assertEqual(act(self.client, "login", email="alice@example.com", password="wrong").json()["error"],
                         "Wrong username or password.")
        before = self.client.session.session_key
        self.assertEqual(act(self.client, "login", email="ALICE@example.com", password=helpers.PASSWORD).status_code, 200)
        self.assertNotEqual(self.client.session.session_key, before)
        self.assertContains(self.client.get("/"), "logout-button")
        act(self.client, "logout")
        self.assertContains(self.client.get("/"), "login-menu-button")

    def test_too_many_wrong_passwords_lock_the_login_for_a_while(self):
        helpers.member("Alice", email="alice@example.com")
        for _ in range(6):
            act(self.client, "login", email="alice@example.com", password="wrong")
        response = act(self.client, "login", email="alice@example.com", password=helpers.PASSWORD)
        self.assertEqual(response.json()["error"], "Too many attempts. Please try again later.")

    def test_banned_accounts_are_kept_out(self):
        alice = helpers.member("Alice", email="alice@example.com")
        client = self.login(alice)
        models.Account.objects.filter(id=alice.account_id).update(banned=True)
        self.assertContains(client.get("/"), "Please contact")
        self.assertEqual(act(client, "boost", assignment="x", value=1).status_code, 400)
        self.assertEqual(act(self.client, "login", email="alice@example.com", password=helpers.PASSWORD).status_code, 400)

    def test_forgotten_password(self):
        helpers.member("Alice", email="alice@example.com")
        mail.outbox.clear()
        unknown = act(self.client, "request-reset", email="nobody@example.com").json()["message"]
        known = act(self.client, "request-reset", email="alice@example.com").json()["message"]
        self.assertEqual(unknown, known)
        self.assertEqual(len(mail.outbox), 1)
        token = self.link("reset")
        self.assertEqual(act(self.client, "reset", token=token, password="short").status_code, 400)
        self.assertEqual(act(self.client, "reset", token=token, password="a new long password").status_code, 200)
        self.assertContains(self.client.get("/"), "logout-button")
        self.assertEqual(act(Client(), "reset", token=token, password="another long password").status_code, 400)
        self.assertEqual(act(Client(), "login", email="alice@example.com", password="a new long password").status_code, 200)

    def test_change_email_needs_the_password_and_a_confirmation(self):
        alice = helpers.member("Alice", email="alice@example.com")
        client = self.login(alice)
        mail.outbox.clear()
        self.assertEqual(act(client, "change-email", email="new@example.com", password="wrong").status_code, 400)
        self.assertEqual(act(client, "change-email", email="new@example.com", password=helpers.PASSWORD).status_code, 200)
        self.assertEqual(models.User.objects.get().email, "alice@example.com")
        self.assertEqual(mail.outbox[-1].to, ["new@example.com"])
        self.assertEqual(act(client, "confirm-email", token=self.link("email")).status_code, 200)
        self.assertEqual(models.User.objects.get().email, "new@example.com")
        self.assertEqual(act(client, "confirm-email", token="nonsense").status_code, 400)

    def test_visitors_upload_one_picture_for_their_registration(self):
        self.assertEqual(self.client.post("/mastery/upload", {"file": helpers.png()}).status_code, 403)
        first = self.client.post("/mastery/upload", {"file": helpers.png(), "register": "true"}).json()["id"]
        second = self.client.post("/mastery/upload", {"file": helpers.png(), "register": "true"}).json()["id"]
        # The first one is gone
        self.assertEqual(self.client.get("/mastery/res/dynamic/%s/small" % first).status_code, 404)
        self.register(picture=second)
        self.assertEqual(models.Person.objects.get().picture.small, "mastery/res/dynamic/%s/small" % second)

    def test_fields_that_only_robots_fill_in_stop_the_registration_silently(self):
        response = self.register(website="http://spam.example")
        self.assertEqual(response.status_code, 200)
        self.assertFalse(models.User.objects.exists())


class CommandsTest(SiteTestCase):

    def test_make_admin(self):
        from django.core.management import CommandError, call_command
        alice = helpers.member("Alice", email="alice@example.com")
        helpers.legacy_member("Old Timer", email="old@example.com")
        call_command("make_admin", "Alice@example.com", stdout=open(__import__("os").devnull, "w"))
        self.assertTrue(models.Account.objects.get(id=alice.account_id).admin)
        call_command("make_admin", "alice@example.com", "--revoke", stdout=open(__import__("os").devnull, "w"))
        self.assertFalse(models.Account.objects.get(id=alice.account_id).admin)
        with self.assertRaises(CommandError):
            call_command("make_admin", "old@example.com")

    def test_the_mail_job_waits_for_the_full_hour(self):
        from mastery import scheduler
        now = datetime.datetime(2026, 10, 7, 12, 59, 30, tzinfo=datetime.UTC)
        self.assertEqual(scheduler.seconds_to_next_hour(now), 30)
