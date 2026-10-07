"""Who is looking at the page."""
from dataclasses import dataclass

from django.conf import settings

from mastery.models import Account, Person

# Unix time in milliseconds when the terms of service were last changed
CURRENT_TOS_VERSION = 1342275766000


@dataclass(frozen=True)
class Viewer:
    account: Account | None = None
    # The person that the viewer acts as: the own one, or for an admin the chosen test person
    person: Person | None = None
    is_admin: bool = False
    # The account exists but is not allowed in
    banned: bool = False
    needs_tos_confirmation: bool = False

    @property
    def logged_in(self):
        return self.account is not None

    @property
    def person_id(self):
        return self.person.id if self.person else None

    @property
    def cloaked(self):
        return self.account is not None and self.person_id != self.account.person_id


ANONYMOUS = Viewer()


def get_viewer(request):
    """Returns the viewer of a request. Without login everybody is anonymous."""
    if not settings.ENABLE_LOGIN or not request.user.is_authenticated:
        return ANONYMOUS
    account = (Account.objects.filter(user=request.user, deleted=False, legacy=False)
               .select_related("person__picture", "cloak__picture").first())
    if account is None or account.person is None or account.person.deleted:
        return ANONYMOUS
    if account.banned:
        return Viewer(banned=True)
    person = account.person
    if account.admin and account.cloak is not None and not account.cloak.deleted:
        person = account.cloak
    return Viewer(
        account=account, person=person, is_admin=account.admin,
        needs_tos_confirmation=account.confirmed_tos_version < CURRENT_TOS_VERSION)
