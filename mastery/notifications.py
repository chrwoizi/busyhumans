"""Notifications about activities and rewards, sent by email once an hour.

Legacy accounts get no notifications and no email.
"""
import logging

from django.conf import settings
from django.core.mail import send_mail
from django.template.loader import render_to_string
from django.utils import timezone
from django.utils.html import escape

from mastery import balancing
from mastery.models import Account, Notification, Person, Subscription

logger = logging.getLogger(__name__)

LINK_STYLE = "font-family: Arial; font-size: 10pt;"
ITEM_STYLE = "margin-top: 10px; margin-bottom: 10px; font-family: Arial; font-size: 10pt;"


def _link(token, content):
    return '<a href="%s#%s" style="%s">%s</a>' % (escape(settings.BASE_URL), escape(token), LINK_STYLE, content)


def _person(person):
    return _link("person=%s" % person.id, escape(person.name))


def _assignment(assignment):
    return _link("assignment=%s" % assignment.id, escape(assignment.title))


def _create(account, typ, html, values):
    if account is None or account.legacy or account.deleted:
        return
    Notification.objects.create(account=account, typ=typ, timestamp=timezone.now(), html=html, values=values)


def create_other_activity(author, assignment):
    """Tells the subscribers of an assignment that somebody completed it."""
    subscriptions = Subscription.objects.filter(deleted=False, assignment=assignment).select_related("account")
    for subscription in subscriptions:
        if subscription.account_id != author.account_id:
            _create(
                subscription.account, Notification.Type.OTHER_ACTIVITY,
                "%s has completed %s." % (_person(author), _assignment(assignment)),
                [str(author.id), author.name, str(assignment.id), assignment.title])


def create_activity_reward(account, xp, assignment):
    _create(
        account, Notification.Type.REWARD_ACTIVITY,
        "%d XP for your activity on %s." % (xp, _assignment(assignment)),
        [str(xp), str(assignment.id), assignment.title])


def create_other_activity_reward(account, xp, author, assignment):
    _create(
        account, Notification.Type.REWARD_OTHER_ACTIVITY,
        "%d XP for %s's activity on %s." % (xp, _person(author), _assignment(assignment)),
        [str(xp), str(author.id), author.name, str(assignment.id), assignment.title])


def included_types(account):
    """The kinds of notifications that the account wants."""
    types = []
    if account.notify_other_activity:
        types.append(Notification.Type.OTHER_ACTIVITY)
    if account.notify_activity_reward:
        types.append(Notification.Type.REWARD_ACTIVITY)
    if account.notify_other_activity_reward:
        types.append(Notification.Type.REWARD_OTHER_ACTIVITY)
    return types


def pending(account):
    return list(Notification.objects.filter(
        deleted=False, account=account, emailed__isnull=True, typ__in=included_types(account),
    ).order_by("timestamp")[:999])


def ordinal(number):
    if number // 10 == 1:
        return "th"
    return {1: "st", 2: "nd", 3: "rd"}.get(number % 10, "th")


def get_items(account):
    """Returns the pending notifications of an account as pieces of HTML."""
    items = pending(account)
    result = [item.html for item in items if item.typ == Notification.Type.OTHER_ACTIVITY]

    reward_items = [item for item in items if item.typ in (
        Notification.Type.REWARD_ACTIVITY, Notification.Type.REWARD_OTHER_ACTIVITY)]
    if reward_items:
        person = Person.objects.filter(id=account.person_id, deleted=False).first()
        if person is None:
            return []
        you = _link("person=%s" % person.id, "You")
        total = sum(int(item.values[0]) for item in reward_items)
        sources = "".join('<li style="%s">%s</li>' % (ITEM_STYLE, item.html) for item in reward_items)
        result.append("%s gained %d XP:<ul>%s</ul>" % (you, total, sources))

        rank = Person.objects.filter(deleted=False, xp__gt=person.xp).count()
        result.append("%s have %d XP which puts you on level %d and %d%s place worldwide." % (
            you, person.xp, balancing.get_level(person.xp), rank, ordinal(rank)))
    return result


def send_emails():
    """Sends one email to every account that has pending notifications. Returns how many were sent."""
    sent = 0
    accounts = Account.objects.filter(
        deleted=False, legacy=False, notifications__deleted=False, notifications__emailed__isnull=True,
    ).select_related("user").distinct()
    for account in accounts:
        try:
            sent += send_email(account)
        except Exception:
            logger.exception("Could not send the notifications of account %s", account.id)
    return sent


def send_email(account):
    if not pending(account):
        return 0
    timestamp = timezone.now()
    address = account.user.email if account.user and account.user.is_active else None
    if address:
        html = render_to_string("mastery/email/notifications.html", {
            "items": get_items(account), "base_url": settings.BASE_URL, "contact": settings.CONTACT_EMAIL})
        send_mail("Notifications", "", settings.DEFAULT_FROM_EMAIL, [address], html_message=html)
    # Without an address the notifications are dropped
    Notification.objects.filter(
        deleted=False, account=account, emailed__isnull=True, timestamp__lt=timestamp).update(emailed=timestamp)
    return 1 if address else 0
