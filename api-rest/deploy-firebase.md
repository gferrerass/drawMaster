# Deploy Realtime Database rules (helpers)

This file describes quick ways to publish Realtime Database rules without installing the Firebase CLI locally.

Files in this repo:
- `firebase/database.rules.json` — rules file.
- `firebase.json` — firebase CLI config.
- `apply_rules.py` — Python script to upload rules using the service account.
- `draw-master-d1ba0-firebase-adminsdk-fbsvc-7aa7eac6f3.json` — service account (already in repo). **Keep it private**.

Option A — Paste in Console (fast)
1. Firebase Console → Realtime Database → Rules
2. Paste `firebase/database.rules.json` and Publish

Option B — Docker + firebase-tools (no local install)

From `api-rest` (PowerShell):

```powershell
docker run --rm -it \
  -v ${PWD}:/workspace \
  -v ${PWD}/draw-master-d1ba0-firebase-adminsdk-fbsvc-7aa7eac6f3.json:/tmp/serviceAccount.json:ro \
  -w /workspace node:18 bash -lc \
  "export GOOGLE_APPLICATION_CREDENTIALS=/tmp/serviceAccount.json && npx -y firebase-tools deploy --only database --project <PROJECT_ID>"
```

Replace `<PROJECT_ID>` with your Firebase project id (from `google-services.json` or console).

Option C — Python script (no Node, no CLI)

Install deps:

```bash
pip install google-auth requests
```

Run:

```bash
python apply_rules.py --sa draw-master-d1ba0-firebase-adminsdk-fbsvc-7aa7eac6f3.json --db-host draw-master-d1ba0-default-rtdb.europe-west1.firebasedatabase.app
```

Notes
- For CI/production: store service account in CI secrets or GCP Secret Manager, do NOT commit to public repositories.
- After deploy: run the Android invite flow again; if `Permission denied` persists, provide the emitter UID and recipient UID from the logs and I will adjust the rules.

If you want, I can run the Python script now using the service account present in the workspace — confirm and I'll execute it. (I will not upload credentials anywhere.)
