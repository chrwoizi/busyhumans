"""Sends the pending notification emails at every full hour.

Runs as a thread of the server process (see wsgi.py). That is enough because
there is exactly one server process: the database is a file.
"""
import datetime
import logging
import threading
import time

from django.conf import settings
from django.db import close_old_connections

logger = logging.getLogger(__name__)

_started = False


def seconds_to_next_hour(now=None):
    now = now or datetime.datetime.now(datetime.UTC)
    following = (now + datetime.timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)
    return (following - now).total_seconds()


def _run():
    from mastery import notifications
    while True:
        time.sleep(seconds_to_next_hour())
        try:
            close_old_connections()
            sent = notifications.send_emails()
            if sent:
                logger.info("Sent %d notification emails", sent)
        except Exception:
            logger.exception("Could not send the notification emails")
        finally:
            close_old_connections()


def start():
    """Starts the thread once. Without login nobody can get a notification, so nothing is started."""
    global _started
    if _started or not settings.ENABLE_LOGIN:
        return
    _started = True
    threading.Thread(target=_run, name="notifications", daemon=True).start()
