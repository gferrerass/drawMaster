FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1
WORKDIR /app

# Install minimal system deps
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY . /app

RUN chmod +x /app/entrypoint.sh

EXPOSE 5000

# Run the entrypoint via the shell to avoid executable-bit/CRLF issues on Windows mounts
CMD ["sh", "-c", "if [ -f /app/entrypoint.sh ]; then sh /app/entrypoint.sh; else exec gunicorn --bind 0.0.0.0:5000 wsgi:application --workers 2; fi"]
