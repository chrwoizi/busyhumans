"""Settings of Busy Humans.

Everything that differs between environments comes from environment variables.
"""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent


def env_flag(name, default):
    value = os.environ.get(name)
    if value is None:
        return default
    return value.strip().lower() in ("1", "true", "yes", "on")


DEBUG = env_flag("DEBUG", False)

SECRET_KEY = os.environ.get("SECRET_KEY", "")
if not SECRET_KEY:
    if not DEBUG:
        raise RuntimeError("SECRET_KEY must be set unless DEBUG is on.")
    SECRET_KEY = "insecure-key-for-local-development-only"

ALLOWED_HOSTS = [h for h in os.environ.get("ALLOWED_HOSTS", "").split(",") if h]
if DEBUG:
    ALLOWED_HOSTS += ["localhost", "127.0.0.1"]

# The database file and the uploaded pictures. In the cluster this is the volume.
DATA_DIR = Path(os.environ.get("DATA_DIR", BASE_DIR / "data"))

# Address of the site with a slash at the end, for links in emails and previews
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8000/")
if not BASE_URL.endswith("/"):
    BASE_URL += "/"

# Street and house number for the imprint. Not part of the repository.
IMPRINT_STREET = os.environ.get("IMPRINT_STREET", "")

# Shown to users who need help, and named in emails
CONTACT_EMAIL = os.environ.get("CONTACT_EMAIL", "busyhumans@woizischke.com")

#
# Features
#

# Login and registration are implemented but stay switched off for now.
ENABLE_LOGIN = env_flag("ENABLE_LOGIN", False)
ENABLE_SEARCH = env_flag("ENABLE_SEARCH", True)

# While this assignment exists, everybody who creates an assignment becomes a "founder" and gets
# its reward. It was the way to get the first assignments. Empty: nobody becomes a founder any more.
FOUNDER_ASSIGNMENT_ID = os.environ.get("FOUNDER_ASSIGNMENT_ID", "")

#
# Django
#

INSTALLED_APPS = [
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.staticfiles",
    "mastery",
]

MIDDLEWARE = [
    "mastery.middleware.HealthMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "whitenoise.middleware.WhiteNoiseMiddleware",
    "mastery.middleware.SecurityHeadersMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "busyhumans.urls"
WSGI_APPLICATION = "busyhumans.wsgi.application"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {"context_processors": ["django.template.context_processors.request"]},
    },
]

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": DATA_DIR / "busyhumans.sqlite3",
        "OPTIONS": {
            # Take the write lock at the start of a transaction instead of failing in the middle of it
            "transaction_mode": "IMMEDIATE",
            "timeout": 20,
            # The volume in the cluster is an NFS share, where the write-ahead log of SQLite
            # does not work reliably. The rollback journal does.
            "init_command": "PRAGMA journal_mode=DELETE; PRAGMA synchronous=FULL;",
        },
    }
}

AUTH_USER_MODEL = "mastery.User"

PASSWORD_HASHERS = ["django.contrib.auth.hashers.Argon2PasswordHasher"]

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator", "OPTIONS": {"min_length": 8}},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = False
USE_TZ = True

STATIC_URL = "/static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
MEDIA_ROOT = DATA_DIR / "media"
STORAGES = {
    "default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
    "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
}
if not DEBUG:
    # The file names carry a hash of the content, so browsers may keep them forever.
    # Needs "manage.py collectstatic", which the image build runs.
    STORAGES["staticfiles"] = {"BACKEND": "whitenoise.storage.CompressedManifestStaticFilesStorage"}
# The pictures of the design are referenced without a hash, in stylesheets and in stored data
WHITENOISE_MANIFEST_STRICT = False

# One server process: the limits on login attempts are counted in its memory
CACHES = {"default": {"BACKEND": "django.core.cache.backends.locmem.LocMemCache"}}

# Uploads are read into memory to be checked. Larger requests are refused.
DATA_UPLOAD_MAX_MEMORY_SIZE = 11 * 1024 * 1024
FILE_UPLOAD_MAX_MEMORY_SIZE = 11 * 1024 * 1024

#
# Email. Only needed once login is switched on.
#

EMAIL_HOST = os.environ.get("EMAIL_HOST", "")
EMAIL_PORT = int(os.environ.get("EMAIL_PORT", "587"))
EMAIL_HOST_USER = os.environ.get("EMAIL_USER", "")
EMAIL_HOST_PASSWORD = os.environ.get("EMAIL_PASSWORD", "")
EMAIL_USE_TLS = env_flag("EMAIL_STARTTLS", True)
EMAIL_TIMEOUT = 20
DEFAULT_FROM_EMAIL = os.environ.get("EMAIL_FROM", "Busy Humans <%s>" % CONTACT_EMAIL)
if not EMAIL_HOST:
    # Without a mail server the emails are written to the log
    EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"

#
# Security
#

SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_AGE = 30 * 24 * 60 * 60
# Links in emails (confirm the address, new password) are valid for three days
PASSWORD_RESET_TIMEOUT = 3 * 24 * 60 * 60
SESSION_COOKIE_SAMESITE = "Lax"
CSRF_COOKIE_SAMESITE = "Lax"
SECURE_CONTENT_TYPE_NOSNIFF = True
SECURE_REFERRER_POLICY = "same-origin"
X_FRAME_OPTIONS = "DENY"

if not DEBUG:
    SESSION_COOKIE_SECURE = True
    CSRF_COOKIE_SECURE = True
    SECURE_HSTS_SECONDS = 31536000
    # TLS ends at the ingress
    SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
    SECURE_SSL_REDIRECT = True
