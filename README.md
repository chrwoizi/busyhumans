# Busy Humans #
Busy Humans is a social media website that encourages people to learn and share real life skills through gamification. Users can define challenges for other users and reward their learning progress with virtual experience points.

**Try the official online version: [busyhumans.woizischke.com](https://busyhumans.woizischke.com)**

The site is a [Django](https://www.djangoproject.com) application. Its data is one SQLite file and a directory of pictures.

Login and registration exist but are switched off (`ENABLE_LOGIN`). The accounts from before 2026 are kept as history: they cannot log in and never receive email.

## Run it locally

```sh
uv sync
DEBUG=1 uv run python manage.py migrate
DEBUG=1 uv run python manage.py runserver
```

The data is in `data/` (set `DATA_DIR` for another place). Tests: `DEBUG=1 uv run python manage.py test mastery`.

## Where things are

| Path | What |
| --- | --- |
| `busyhumans/` | Settings and URLs |
| `mastery/models.py` | The data |
| `mastery/presenter.py` | What the pages show |
| `mastery/actions.py`, `mastery/accounts.py` | What users can change; registration and login |
| `mastery/rewards.py`, `mastery/balancing.py` | XP, levels and rewards |
| `mastery/search.py` | The search, kept in memory |
| `mastery/media.py` | Uploaded and copied pictures |
| `mastery/templates/`, `mastery/static/` | Pages, styles and the client script |
| `mastery/legacy/` | Import of the old MongoDB database |
| `.pipeline/`, `Dockerfile`, `.woodpecker.yaml` | Image, chart and pipeline |

The pages are rendered on the server. `mastery/static/mastery/app.js` puts the page for the part of the address after the `#` into the frame, so links like `#assignment=<id>` work as they always did.

## Settings

All settings are environment variables.

| Variable | Meaning |
| --- | --- |
| `SECRET_KEY` | Required. Signs the sessions and the links in emails. |
| `ALLOWED_HOSTS` | Required. Host names of the site, separated by commas. |
| `BASE_URL` | Address of the site, for links in emails and previews. |
| `DATA_DIR` | Directory of the database file and the pictures. In the image: `/data`. |
| `ENABLE_LOGIN` | `true` switches registration and login on. Default: off. Needs the email settings. |
| `ENABLE_SEARCH` | Default: on. |
| `EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_USER`, `EMAIL_PASSWORD`, `EMAIL_STARTTLS`, `EMAIL_FROM` | Mail server. Without `EMAIL_HOST`, emails are written to the log. |
| `CONTACT_EMAIL` | Address that users are told to write to. |
| `IMPRINT_STREET` | Street line of the imprint. |
| `FOUNDER_ASSIGNMENT_ID` | See `busyhumans/settings.py`. Default: off. |
| `DEBUG` | Only for development. |

## Deployment

The pipeline tests, builds the image and installs the chart in `.pipeline/helm/busyhumans`. The cluster provides the volume for `/data`, the secret and the host name.

- Run **one** replica with one server process. The database is a file, the search index and the limits on login attempts are in the memory of that process. The chart and the image are set up like this.
- The volume has to be writable for user 1000.
- The container applies database migrations when it starts.
- Back up the volume. A consistent copy of the database while the site runs: `sqlite3 busyhumans.sqlite3 ".backup copy.sqlite3"`.

Commands inside the container:

```sh
python manage.py make_admin <email>       # give an account admin rights (--revoke takes them away)
python manage.py send_notifications       # send pending notification emails now
```

## Import of the old database

Only needed once. It needs an empty database.

```sh
mongodump --archive --gzip --db mastery > dump.archive.gz
python manage.py migrate
python manage.py import_legacy dump.archive.gz --external <directory with copies of hotlinked pictures>
```

The import marks every account as legacy and leaves out passwords and the tokens of Facebook, Twitter and Google. `mastery/legacy/importer.py` describes what else changes on the way.
