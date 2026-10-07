from django.core.management.base import BaseCommand

from mastery import notifications


class Command(BaseCommand):
    help = "Sends the pending notification emails now. The server does this by itself at every full hour."

    def handle(self, *args, **options):
        self.stdout.write("Sent %d emails." % notifications.send_emails())
