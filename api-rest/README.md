# API REST for drawMaster

Scaffold Flask API for drawMaster. Includes:

- Flask + SQLAlchemy (configurable via `DATABASE_URL`)
- Firebase ID token verification (`firebase_admin`)
- Google Cloud Storage integration (`google-cloud-storage`)

Local quickstart:

```powershell
cd api-rest
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# Edit .env to set FIREBASE_CREDENTIALS (path) and optionally DATABASE_URL
python run.py
```

For deployment to PythonAnywhere, use `DATABASE_URL` pointing to MySQL (PythonAnywhere) or an external Postgres/MySQL.

Firebase: service account vs API Key
----------------------------------

- What you need for the backend: a *Service Account JSON* (Admin SDK key). The backend DOES NOT use the Firebase "API Key" to verify ID tokens. Instead the backend uses the Firebase Admin SDK with a service account key to verify ID tokens issued to Android clients.
- Where to get the Service Account JSON:
	1. Open Firebase Console → select your project.
	2. Project settings (gear) → Service accounts.
	3. Click "Generate new private key" (or "Create new key") and download the JSON file.
	4. Place that JSON locally (outside version control) and set `FIREBASE_CREDENTIALS` in `.env` to its path (or the absolute path on PythonAnywhere).

- What the API Key is and when you need it: the "Web API Key" (visible in Project settings → General) is used by client SDKs (web / some client-side REST calls) and for identifying the Firebase project from a client. You should NOT use the API Key to authenticate or verify tokens on the server. It's safe to keep the API Key client-side, but do not use it as a security mechanism.

Examples (.env)

```
FIREBASE_CREDENTIALS=./firebase-service-account.json
AUTH_SKIP=1   # solo para desarrollo; en producción usar 0 y validar tokens reales
```

PythonAnywhere notes
--------------------
- Upload the service account JSON to your PythonAnywhere `Files` panel (e.g. `/home/youruser/firebase-sa.json`) and set the web app environment variable `FIREBASE_CREDENTIALS=/home/youruser/firebase-sa.json`.

