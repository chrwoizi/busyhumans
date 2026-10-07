from django.apps import AppConfig
from django.db.models.signals import post_save


class MasteryConfig(AppConfig):
    name = "mastery"
    default_auto_field = "django.db.models.BigAutoField"

    def ready(self):
        from mastery import search
        from mastery.models import Assignment, Person, Skill

        def follow(model, typ, field):
            def on_save(sender, instance, **kwargs):
                search.index.update(typ, instance.id, getattr(instance, field), instance.deleted)
            post_save.connect(on_save, sender=model, weak=False)

        # The search index follows every saved person, skill and assignment.
        # Changes that bypass save() (update(), bulk_create()) have to call search.index themselves.
        follow(Person, search.PERSON, "name")
        follow(Skill, search.SKILL, "title")
        follow(Assignment, search.ASSIGNMENT, "title")
