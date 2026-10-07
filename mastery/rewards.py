"""Pays out the XP for activities whose rating period is over."""
from django.db import transaction
from django.db.models import F
from django.utils import timezone

from mastery import balancing, notifications
from mastery.models import Activity, Person


def distribute(assignment_ids):
    """Rewards the due activities of the assignments.

    The author of the activity gets a part of the reward of the assignment that
    depends on the ratings, and the author of the assignment a tenth of that.
    """
    due = list(Activity.objects.filter(
        assignment_id__in=assignment_ids, deleted=False, rewarded__isnull=True,
        reward_due_date__lt=timezone.now(), assignment__deleted=False, assignment__skill__deleted=False,
    ).select_related("assignment__author__account", "author__account").prefetch_related(
        "ratings", "assignment__rewards"))
    for activity in due:
        with transaction.atomic():
            reward(activity)


def reward(activity):
    assignment = activity.assignment
    max_reward = max(0, sum(boost.amount for boost in assignment.rewards.all() if not boost.deleted))
    ratings = [rating for rating in activity.ratings.all() if not rating.deleted]
    likes = sum(rating.positive for rating in ratings)
    author_reward = balancing.get_activity_reward(max_reward, likes, len(ratings) - likes)

    # Only if nobody else rewarded it in the meantime
    if not Activity.objects.filter(id=activity.id, deleted=False, rewarded__isnull=True).update(
            rewarded=author_reward):
        return
    activity.rewarded = author_reward
    Person.objects.filter(id=activity.author_id, deleted=False).update(xp=F("xp") + author_reward)
    if author_reward <= 0:
        return

    notifications.create_activity_reward(activity.author.account, author_reward, assignment)
    if assignment.author_id != activity.author_id:
        owner_reward = balancing.get_owner_reward(author_reward)
        Person.objects.filter(id=assignment.author_id, deleted=False).update(xp=F("xp") + owner_reward)
        if owner_reward > 0:
            notifications.create_other_activity_reward(
                assignment.author.account, owner_reward, activity.author, assignment)
