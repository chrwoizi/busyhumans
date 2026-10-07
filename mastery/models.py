import uuid

from django.contrib.auth.models import AbstractBaseUser, BaseUserManager
from django.db import models
from django.db.models import Q


class UserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra):
        if not email:
            raise ValueError("An email address is required.")
        user = self.model(email=self.normalize_email(email).lower(), **extra)
        user.set_password(password)
        user.save(using=self._db)
        return user


class User(AbstractBaseUser):
    """Login of an active account. Legacy accounts have none, so they cannot log in."""

    email = models.EmailField(unique=True)
    is_active = models.BooleanField(default=True)
    date_joined = models.DateTimeField(auto_now_add=True)

    objects = UserManager()

    USERNAME_FIELD = "email"

    def __str__(self):
        return self.email


class UniqueIdModel(models.Model):
    """Base of all business models. The ids are public: they are part of the urls."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    # Deleted entities stay in the database but are never shown
    deleted = models.BooleanField(default=False)

    class Meta:
        abstract = True


class LicenseType(models.IntegerChoices):
    UNKNOWN = 0
    NONE = 1
    CC_BY_30 = 10
    CC_BY_SA_30 = 11
    CC_BY_ND_30 = 12
    YOUTUBE = 20
    FACEBOOK = 30
    INTERNAL_TEXT = 40
    INTERNAL_PICTURE = 41
    WIKIPEDIA = 50
    TWITTER = 60


class Resource(UniqueIdModel):
    """A text, picture or video together with its author and license."""

    # The text itself, the path or url of the picture, or the YouTube id of the video
    resource = models.TextField()
    small = models.CharField(max_length=500, null=True, blank=True)
    medium = models.CharField(max_length=500, null=True, blank=True)
    large = models.CharField(max_length=500, null=True, blank=True)
    hires = models.CharField(max_length=500, null=True, blank=True)
    author_name = models.CharField(max_length=200, null=True, blank=True)
    author_url = models.CharField(max_length=500, null=True, blank=True)
    license = models.IntegerField(choices=LicenseType, default=LicenseType.UNKNOWN)


class MirroredFile(models.Model):
    """A picture that was hotlinked from another site and has been copied to disk.

    Resources point to the local path. This keeps the original url, also for the
    pictures that no longer exist and got the placeholder.
    """

    url = models.CharField(max_length=500, unique=True)
    # Path below the media directory, null if the picture could not be copied
    path = models.CharField(max_length=200, null=True, blank=True)
    status = models.CharField(max_length=50)


class City(UniqueIdModel):
    facebook_id = models.CharField(max_length=100, null=True, blank=True)
    name = models.CharField(max_length=200)


class Person(UniqueIdModel):
    """The public profile of a user. Test persons of the admin have no account."""

    account = models.OneToOneField(
        "Account", null=True, blank=True, on_delete=models.SET_NULL, related_name="own_person")
    registration_timestamp = models.DateTimeField(null=True, blank=True)
    name = models.CharField(max_length=200)
    picture = models.OneToOneField(Resource, on_delete=models.PROTECT, related_name="+")
    gender = models.CharField(max_length=20, null=True, blank=True)
    birthday = models.DateField(null=True, blank=True)
    city = models.ForeignKey(City, null=True, blank=True, on_delete=models.SET_NULL)
    locale = models.CharField(max_length=20, null=True, blank=True)
    timezone = models.FloatField(default=0)
    xp = models.IntegerField(default=0)
    founder = models.BooleanField(null=True, blank=True)


class Contact(UniqueIdModel):
    person = models.ForeignKey(Person, on_delete=models.CASCADE, related_name="contacts")
    timestamp = models.DateTimeField()
    typ = models.CharField(max_length=50)
    value = models.CharField(max_length=500)


class Account(UniqueIdModel):
    user = models.OneToOneField(User, null=True, blank=True, on_delete=models.SET_NULL)
    # Accounts from before the migration: they cannot log in and never receive email
    legacy = models.BooleanField(default=False)
    email = models.EmailField(null=True, blank=True)
    join_date = models.DateTimeField(null=True, blank=True)
    last_login = models.DateTimeField(null=True, blank=True)
    # Unix time in milliseconds of the terms of service that the user agreed to
    confirmed_tos_version = models.BigIntegerField(default=0)
    person = models.ForeignKey(Person, null=True, blank=True, on_delete=models.SET_NULL, related_name="+")
    # Person that an admin currently acts as
    cloak = models.ForeignKey(Person, null=True, blank=True, on_delete=models.SET_NULL, related_name="+")
    admin = models.BooleanField(default=False)
    banned = models.BooleanField(default=False)
    last_announcement = models.DateTimeField(null=True, blank=True)

    subscribe_own_assignments = models.BooleanField(default=True)
    notify_other_activity = models.BooleanField(default=True)
    notify_activity_reward = models.BooleanField(default=True)
    notify_other_activity_reward = models.BooleanField(default=True)

    class Meta:
        constraints = [
            models.CheckConstraint(
                condition=Q(legacy=False) | Q(user__isnull=True), name="legacy_account_has_no_login"),
        ]


class Skill(UniqueIdModel):
    author = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    creation_timestamp = models.DateTimeField()
    title = models.CharField(max_length=40)
    description = models.OneToOneField(Resource, on_delete=models.PROTECT, related_name="+")
    picture = models.OneToOneField(Resource, on_delete=models.PROTECT, related_name="+")
    abuse_reports_count = models.IntegerField(default=0)


class Assignment(UniqueIdModel):
    creation_timestamp = models.DateTimeField()
    author = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    skill = models.ForeignKey(Skill, on_delete=models.PROTECT, related_name="assignments")
    title = models.CharField(max_length=40)
    description = models.TextField(blank=True)
    rewards_sum = models.IntegerField(default=0)
    last_activity = models.DateTimeField(null=True, blank=True)
    abuse_reports_count = models.IntegerField(default=0)


class Reward(UniqueIdModel):
    """XP that a person adds to (or takes from) the reward of an assignment."""

    assignment = models.ForeignKey(Assignment, on_delete=models.CASCADE, related_name="rewards")
    timestamp = models.DateTimeField()
    donating_person = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    amount = models.IntegerField()


class Achievement(UniqueIdModel):
    """All activities of a person for one skill."""

    owner = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="achievements")
    skill = models.ForeignKey(Skill, on_delete=models.PROTECT, related_name="+")


class Activity(UniqueIdModel):
    author = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    timestamp = models.DateTimeField()
    assignment = models.ForeignKey(Assignment, on_delete=models.PROTECT, related_name="activities")
    achievement = models.ForeignKey(Achievement, on_delete=models.PROTECT, related_name="activities")
    # The ratings at this date determine the reward
    reward_due_date = models.DateTimeField()
    # Null before the due date, afterwards the XP that the author got
    rewarded = models.IntegerField(null=True, blank=True)
    abuse_reports_count = models.IntegerField(default=0)


class ContentBlock(UniqueIdModel):
    class Type(models.IntegerChoices):
        TEXT = 0
        IMAGE = 1
        VIDEO = 2

    activity = models.ForeignKey(Activity, on_delete=models.CASCADE, related_name="content_blocks")
    position = models.PositiveSmallIntegerField()
    typ = models.IntegerField(choices=Type)
    value = models.OneToOneField(Resource, on_delete=models.PROTECT, related_name="+")
    # The content was uploaded by the author of the activity
    authentic = models.BooleanField(null=True, blank=True)

    class Meta:
        ordering = ["position"]


class Rating(UniqueIdModel):
    activity = models.ForeignKey(Activity, on_delete=models.CASCADE, related_name="ratings")
    author = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    timestamp = models.DateTimeField()
    positive = models.BooleanField()


class AbuseReport(UniqueIdModel):
    """A report against exactly one skill, assignment or activity."""

    author = models.ForeignKey(Person, on_delete=models.PROTECT, related_name="+")
    timestamp = models.DateTimeField()
    skill = models.ForeignKey(Skill, null=True, blank=True, on_delete=models.CASCADE, related_name="abuse_reports")
    assignment = models.ForeignKey(
        Assignment, null=True, blank=True, on_delete=models.CASCADE, related_name="abuse_reports")
    activity = models.ForeignKey(
        Activity, null=True, blank=True, on_delete=models.CASCADE, related_name="abuse_reports")

    class Meta:
        constraints = [
            models.CheckConstraint(
                condition=(
                    Q(skill__isnull=False, assignment__isnull=True, activity__isnull=True)
                    | Q(skill__isnull=True, assignment__isnull=False, activity__isnull=True)
                    | Q(skill__isnull=True, assignment__isnull=True, activity__isnull=False)
                ),
                name="abuse_report_has_one_target"),
        ]


class Subscription(UniqueIdModel):
    timestamp = models.DateTimeField()
    account = models.ForeignKey(Account, on_delete=models.CASCADE, related_name="subscriptions")
    assignment = models.ForeignKey(Assignment, on_delete=models.CASCADE, related_name="subscriptions")


class Notification(UniqueIdModel):
    class Type(models.IntegerChoices):
        UNKNOWN = 0
        OTHER_ACTIVITY = 10
        REWARD_ACTIVITY = 20
        REWARD_OTHER_ACTIVITY = 30

    typ = models.IntegerField(choices=Type)
    account = models.ForeignKey(Account, on_delete=models.CASCADE, related_name="notifications")
    timestamp = models.DateTimeField()
    # When the notification was sent by email, null while it is pending
    emailed = models.DateTimeField(null=True, blank=True)
    html = models.TextField()
    values = models.JSONField(default=list)


class Announcement(UniqueIdModel):
    show_time = models.DateTimeField()
    hide_time = models.DateTimeField(null=True, blank=True)
    text = models.TextField()
    maintenance = models.BooleanField(default=False)
