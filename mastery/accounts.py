"""Registration, login and everything else about accounts.

A new account always starts empty. It is never connected to a legacy account,
not even if the email address is the same.
"""
import re

from django.conf import settings
from django.contrib.auth import authenticate, login as session_login, logout as session_logout
from django.contrib.auth.password_validation import validate_password
from django.contrib.auth.tokens import PasswordResetTokenGenerator, default_token_generator
from django.core import signing
from django.core.cache import cache
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.db import transaction
from django.utils import timezone

from mastery import actions, media, sanitizer
from mastery.actions import ActionError
from mastery.models import Account, Person, User
from mastery.viewer import CURRENT_TOS_VERSION

EMAIL = re.compile(r"^[^@\s]+@[^@\s]+\.[^@.\s]+$")
PASSWORD_LENGTH_MAX = 64

EMAIL_CHANGE_SALT = "mastery.email-change"

REGISTERED_MESSAGE = (
    "Almost done! We have sent you an email. Please click the link in it to activate your account.")


def check_enabled():
    if not settings.ENABLE_LOGIN:
        raise ActionError("Login is disabled")


#
# limits against guessing
#

def throttle(kind, key, limit, seconds):
    """Counts an attempt. Raises if there were more than limit attempts within the time."""
    cache_key = "throttle:%s:%s" % (kind, key)
    cache.add(cache_key, 0, seconds)
    try:
        count = cache.incr(cache_key)
    except ValueError:
        cache.set(cache_key, 1, seconds)
        count = 1
    if count > limit:
        raise ActionError("Too many attempts. Please try again later.")


def client_address(request):
    """The address of the browser. Behind the ingress it is the last entry that the ingress added."""
    forwarded = request.META.get("HTTP_X_FORWARDED_FOR", "")
    if forwarded and not settings.DEBUG:
        return forwarded.split(",")[-1].strip()
    return request.META.get("REMOTE_ADDR", "")


#
# emails
#

def send(address, subject, lines):
    send_mail(subject, "\n\n".join(lines), settings.DEFAULT_FROM_EMAIL, [address])


def verification_token(user):
    return "%d-%s" % (user.pk, default_token_generator.make_token(user))


def user_for_token(token):
    """Returns the user of a link from an email, or None if the link is not valid (any more)."""
    user_id, _, token = (token or "").partition("-")
    if not user_id.isdigit():
        return None
    user = User.objects.filter(pk=int(user_id)).first()
    if user is None or not default_token_generator.check_token(user, token):
        return None
    return user


#
# registration
#

def register(request, email, password, name, picture_id=None, honeypot=""):
    check_enabled()
    throttle("register", client_address(request), 5, 3600)
    if honeypot:
        # A field that people do not see was filled in. Pretend that all is well.
        return REGISTERED_MESSAGE

    email = (email or "").strip().lower()
    if not EMAIL.match(email) or len(email) > 254:
        raise ActionError("The email address is invalid")
    name = sanitizer.person_name(name)
    if name is None:
        raise ActionError("The name is invalid")
    if not password or len(password) > PASSWORD_LENGTH_MAX:
        raise ActionError("The password is invalid")
    try:
        validate_password(password, User(email=email))
    except ValidationError as error:
        raise ActionError(" ".join(error.messages))

    uploads = request.session.get("uploads", [])
    if picture_id:
        actions.check_upload(picture_id, uploads)

    existing = User.objects.filter(email=email).first()
    if existing is not None:
        # Do not tell the visitor that the address is known. Tell the owner of the address instead.
        if existing.is_active:
            send(email, "Your Busy Humans account", [
                "Somebody tried to register a Busy Humans account with your email address. You already have one.",
                "If that was you and you forgot your password, you can set a new one here: %s#reset" % settings.BASE_URL,
                "If it was not you, you can ignore this email.",
            ])
        else:
            send_verification(existing)
        return REGISTERED_MESSAGE

    with transaction.atomic():
        user = User.objects.create_user(email=email, password=password, is_active=False)
        account = Account.objects.create(user=user, email=email, join_date=timezone.now())
        person = Person(account=account, registration_timestamp=timezone.now(), name=name)
        picture = actions.uploaded_picture(picture_id, person) if picture_id else actions.default_picture()
        picture.save()
        person.picture = picture
        person.save()
        account.person = person
        account.save(update_fields=["person"])
    request.session.pop("registration_upload", None)
    send_verification(user)
    return REGISTERED_MESSAGE


def send_verification(user):
    send(user.email, "Activate your Busy Humans account", [
        "Welcome to Busy Humans!",
        "Please click this link to activate your account: %s#verify=%s" % (settings.BASE_URL, verification_token(user)),
        "If you did not register at Busy Humans, you can ignore this email.",
    ])


def verify(request, token):
    """Activates the account of a registration link and logs its owner in."""
    check_enabled()
    throttle("token", client_address(request), 30, 3600)
    user = user_for_token(token)
    if user is None:
        raise ActionError("This link is not valid any more.")
    if not user.is_active:
        user.is_active = True
        user.save(update_fields=["is_active"])
    _login(request, user)


#
# login
#

def _login(request, user):
    account = Account.objects.filter(user=user, deleted=False, legacy=False).first()
    if account is None:
        raise ActionError("Wrong username or password.")
    if account.banned:
        raise ActionError("Please contact %s" % settings.CONTACT_EMAIL)
    # Starts a new session, so a session id from before the login is worthless
    session_login(request, user)
    Account.objects.filter(id=account.id).update(last_login=timezone.now())


def login(request, email, password):
    check_enabled()
    email = (email or "").strip().lower()
    address = client_address(request)
    throttle("login-address", address, 30, 900)
    throttle("login", "%s|%s" % (address, email), 6, 900)
    user = authenticate(request, username=email, password=password or "")
    if user is None:
        raise ActionError("Wrong username or password.")
    _login(request, user)
    cache.delete("throttle:login:%s|%s" % (address, email))


def logout(request):
    session_logout(request)


def confirm_tos(viewer):
    if not viewer.logged_in:
        raise ActionError("Please login.")
    Account.objects.filter(id=viewer.account.id).update(confirmed_tos_version=CURRENT_TOS_VERSION)


#
# forgotten password
#

RESET_MESSAGE = "If this address belongs to an account, we have sent an email with a link to set a new password."


def request_reset(request, email):
    check_enabled()
    throttle("reset", client_address(request), 5, 3600)
    email = (email or "").strip().lower()
    user = User.objects.filter(email=email, is_active=True).first()
    if user is not None and Account.objects.filter(user=user, deleted=False, legacy=False, banned=False).exists():
        token = "%d-%s" % (user.pk, PasswordResetTokenGenerator().make_token(user))
        send(user.email, "New password for Busy Humans", [
            "Click this link to set a new password for your Busy Humans account: %s#reset=%s" % (
                settings.BASE_URL, token),
            "If you did not ask for a new password, you can ignore this email.",
        ])
    return RESET_MESSAGE


def reset(request, token, password):
    """Sets the password of a link from an email and logs its owner in."""
    check_enabled()
    throttle("token", client_address(request), 30, 3600)
    user = user_for_token(token)
    if user is None or not user.is_active:
        raise ActionError("This link is not valid any more.")
    if not password or len(password) > PASSWORD_LENGTH_MAX:
        raise ActionError("The password is invalid")
    try:
        validate_password(password, user)
    except ValidationError as error:
        raise ActionError(" ".join(error.messages))
    user.set_password(password)
    user.save(update_fields=["password"])
    _login(request, user)


#
# preferences
#

def set_preferences(viewer, subscribe_own_assignments, notify_other_activity, notify_activity_reward,
                    notify_other_activity_reward):
    if not viewer.logged_in:
        raise ActionError("Please login.")
    Account.objects.filter(id=viewer.account.id).update(
        subscribe_own_assignments=bool(subscribe_own_assignments),
        notify_other_activity=bool(notify_other_activity),
        notify_activity_reward=bool(notify_activity_reward),
        notify_other_activity_reward=bool(notify_other_activity_reward))


def request_email_change(request, viewer, email, password):
    """Sends a link to the new address. The address changes when the link is opened."""
    if not viewer.logged_in:
        raise ActionError("Please login.")
    user = viewer.account.user
    throttle("email-change", user.pk, 5, 3600)
    email = (email or "").strip().lower()
    if not EMAIL.match(email) or len(email) > 254:
        raise ActionError("Please enter a valid email address.")
    if not user.check_password(password or ""):
        raise ActionError("Wrong password.")
    if not User.objects.filter(email=email).exists():
        token = signing.dumps({"user": user.pk, "email": email}, salt=EMAIL_CHANGE_SALT)
        send(email, "Confirm your new email address", [
            "Click this link to use this email address for your Busy Humans account: %s#email=%s" % (
                settings.BASE_URL, token),
            "If you did not ask for this, you can ignore this email.",
        ])
    return "We have sent an email to the new address. Please click the link in it to confirm the address."


def confirm_email_change(request, token):
    check_enabled()
    throttle("token", client_address(request), 30, 3600)
    try:
        data = signing.loads(token or "", salt=EMAIL_CHANGE_SALT, max_age=settings.PASSWORD_RESET_TIMEOUT)
    except signing.BadSignature:
        raise ActionError("This link is not valid any more.")
    user = User.objects.filter(pk=data["user"], is_active=True).first()
    if user is None or User.objects.filter(email=data["email"]).exists():
        raise ActionError("This link is not valid any more.")
    with transaction.atomic():
        user.email = data["email"]
        user.save(update_fields=["email"])
        Account.objects.filter(user=user).update(email=data["email"])


#
# uploads
#

def may_upload(viewer, for_registration):
    """Members may upload pictures, and visitors one for their registration."""
    if viewer.person is not None and not viewer.needs_tos_confirmation:
        return True
    return settings.ENABLE_LOGIN and for_registration and not viewer.logged_in


def remember_upload(request, picture_id, for_registration):
    """Notes the upload in the session. Only pictures of the own session can be used."""
    if for_registration:
        # A new picture for the registration replaces the previous one
        previous = request.session.get("registration_upload")
        if previous:
            media.delete_upload(previous)
            request.session["uploads"] = [u for u in request.session.get("uploads", []) if u != previous]
        request.session["registration_upload"] = str(picture_id)
    request.session["uploads"] = (request.session.get("uploads", []) + [str(picture_id)])[-50:]
