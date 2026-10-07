from django.core.management.base import BaseCommand, CommandError

from mastery.models import Account


class Command(BaseCommand):
    help = "Gives an account the rights of an admin, or takes them away with --revoke."

    def add_arguments(self, parser):
        parser.add_argument("email", help="Email address that the account logs in with")
        parser.add_argument("--revoke", action="store_true")

    def handle(self, *args, **options):
        email = options["email"].strip().lower()
        # Legacy accounts cannot log in, so they cannot be admins
        account = Account.objects.filter(user__email=email, deleted=False, legacy=False).first()
        if account is None:
            raise CommandError("No account logs in with '%s'." % email)
        account.admin = not options["revoke"]
        account.save(update_fields=["admin"])
        self.stdout.write("%s is %s." % (email, "an admin now" if account.admin else "no admin any more"))
