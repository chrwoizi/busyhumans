from django.conf import settings
from django.core.management.base import BaseCommand, CommandError

from mastery.legacy.archive import load_collections
from mastery.legacy.importer import Importer, ImportFailed


class Command(BaseCommand):
    help = "Imports the data of the old MongoDB database from a mongodump archive into the empty database."

    def add_arguments(self, parser):
        parser.add_argument("archive", help="File written by 'mongodump --archive' (gzip or plain)")
        parser.add_argument(
            "--external",
            help="Directory with the copies of the hotlinked pictures and their manifest.json. "
                 "Without it, all hotlinked pictures get the placeholder.")

    def handle(self, *args, **options):
        collections = load_collections(options["archive"])
        importer = Importer(collections, settings.MEDIA_ROOT, options["external"])
        try:
            counts, notes = importer.run()
        except ImportFailed as ex:
            raise CommandError(str(ex))

        self.stdout.write("Imported rows:")
        for name, count in sorted(counts.items()):
            self.stdout.write("  %-14s %d" % (name, count))
        if notes:
            self.stdout.write("Notes:")
            for note, count in sorted(notes.items()):
                self.stdout.write("  %d %s" % (count, note))
