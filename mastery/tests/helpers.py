"""Builds test data."""
import datetime
import io
import json

from django.utils import timezone
from PIL import Image

from mastery import models
from mastery.viewer import CURRENT_TOS_VERSION

PASSWORD = "correct horse battery"


def resource(value="static/default-skill.png", license=models.LicenseType.INTERNAL_PICTURE):
    return models.Resource.objects.create(resource=value, license=license)


def member(name="Alice", email=None, xp=0, admin=False, tos=True):
    """Creates somebody who can log in. Returns the person; person.account and its user are set."""
    email = email or name.lower().replace(" ", "") + "@example.com"
    user = models.User.objects.create_user(email=email, password=PASSWORD)
    account = models.Account.objects.create(
        user=user, email=email, admin=admin, join_date=timezone.now(),
        confirmed_tos_version=CURRENT_TOS_VERSION if tos else 0)
    person = models.Person.objects.create(
        account=account, name=name, xp=xp, picture=resource(), registration_timestamp=timezone.now())
    account.person = person
    account.save()
    return person


def legacy_member(name="Old Timer", email="old@example.com", xp=0):
    account = models.Account.objects.create(legacy=True, email=email, confirmed_tos_version=CURRENT_TOS_VERSION)
    person = models.Person.objects.create(
        account=account, name=name, xp=xp, picture=resource(),
        registration_timestamp=timezone.now() - datetime.timedelta(days=5000))
    account.person = person
    account.save()
    return person


def skill(author, title="Juggling"):
    return models.Skill.objects.create(
        author=author, creation_timestamp=timezone.now(), title=title, picture=resource(),
        description=resource("Throwing things", models.LicenseType.INTERNAL_TEXT))


def assignment(author, skill_, title="Juggle three balls", reward=50, days_ago=20):
    created = timezone.now() - datetime.timedelta(days=days_ago)
    result = models.Assignment.objects.create(
        author=author, skill=skill_, title=title, description="For ten seconds", creation_timestamp=created,
        rewards_sum=reward)
    models.Reward.objects.create(assignment=result, donating_person=author, timestamp=created, amount=reward)
    return result


def activity(author, assignment_, text="I did it", days_ago=10, rewarded=None):
    """An activity that was posted days_ago. Its rating period ends three days later."""
    posted = timezone.now() - datetime.timedelta(days=days_ago)
    achievement = models.Achievement.objects.filter(owner=author, skill=assignment_.skill, deleted=False).first()
    if achievement is None:
        achievement = models.Achievement.objects.create(owner=author, skill=assignment_.skill)
    result = models.Activity.objects.create(
        author=author, assignment=assignment_, achievement=achievement, timestamp=posted,
        reward_due_date=posted + datetime.timedelta(days=3), rewarded=rewarded)
    models.ContentBlock.objects.create(
        activity=result, position=0, typ=models.ContentBlock.Type.TEXT,
        value=resource(text, models.LicenseType.INTERNAL_TEXT), authentic=True)
    models.Assignment.objects.filter(id=assignment_.id).update(last_activity=posted)
    return result


def rating(activity_, author, positive=True):
    return models.Rating.objects.create(
        activity=activity_, author=author, timestamp=timezone.now(), positive=positive)


def png(width=300, height=200, color=(200, 30, 30)):
    buffer = io.BytesIO()
    Image.new("RGB", (width, height), color).save(buffer, "PNG")
    buffer.seek(0)
    buffer.name = "picture.png"
    return buffer


def act(client, action, /, **data):
    """Sends an action like the client script does."""
    return client.post("/action/" + action, data=json.dumps(data), content_type="application/json")
