#!/bin/sh
set -e

# small sleep to wait for DB container to initialize
echo "Waiting for DB to be ready..."
sleep 5

echo "Running migrations (if any)"
export FLASK_APP=${FLASK_APP:-run.py}
python -m flask db upgrade || true

echo "Starting gunicorn"
exec gunicorn --bind 0.0.0.0:5000 wsgi:application --workers 2
