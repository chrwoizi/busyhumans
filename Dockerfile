FROM python:3.14-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    DATA_DIR=/data

RUN useradd --uid 1000 --create-home busyhumans && mkdir /data && chown busyhumans /data

WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install --no-cache-dir uv && uv export --no-dev --no-hashes --no-emit-project -o requirements.txt \
    && pip install --no-cache-dir -r requirements.txt && pip uninstall -y uv

COPY manage.py ./
COPY busyhumans busyhumans
COPY mastery mastery
RUN SECRET_KEY=build ALLOWED_HOSTS=build python manage.py collectstatic --noinput

USER 1000
EXPOSE 8080
CMD ["sh", "-c", "python manage.py migrate --noinput && exec gunicorn busyhumans.wsgi --bind 0.0.0.0:8080 --workers 1 --threads 8 --access-logfile -"]
