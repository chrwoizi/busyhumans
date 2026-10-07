import os

from django.core.wsgi import get_wsgi_application

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "busyhumans.settings")

application = get_wsgi_application()

from mastery import scheduler  # noqa: E402

scheduler.start()
