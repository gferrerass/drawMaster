#!/bin/sh
set -e

# small sleep to wait for DB container to initialize
echo "Waiting for DB to be ready..."
sleep 5

echo "Running migrations (if any)"
export FLASK_APP=${FLASK_APP:-run.py}
python -m flask db upgrade || true

# If FIREBASE_CREDENTIALS and FIREBASE_DB_HOST are provided, attempt to apply realtime DB rules
if [ -n "${FIREBASE_CREDENTIALS}" ] && [ -n "${FIREBASE_DB_HOST}" ] && [ -f "${FIREBASE_CREDENTIALS}" ]; then
    echo "Applying Firebase Realtime DB rules..."
    # run script but don't fail container startup if rules deployment fails
    python apply_rules.py --sa "${FIREBASE_CREDENTIALS}" --db-host "${FIREBASE_DB_HOST}" --rules firebase/database.rules.json || echo "Failed to deploy DB rules (continuing)"
else
    echo "Skipping DB rules deployment (FIREBASE_CREDENTIALS or FIREBASE_DB_HOST not set)"
fi

echo "Starting gunicorn"
exec gunicorn --bind 0.0.0.0:5000 wsgi:application --workers 2
