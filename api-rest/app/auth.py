import os
import json
from functools import wraps
from flask import request, jsonify, g
from firebase_admin import auth as firebase_auth, credentials, initialize_app
from .config import Config

firebase_app = None

def init_firebase():
    global firebase_app
    if firebase_app is not None:
        return
    try:
        # Prefer explicit service account path from Config if provided
        sa_path = Config.FIREBASE_CREDENTIALS
        if sa_path:
            cred = credentials.Certificate(sa_path)
        else:
            cred = credentials.ApplicationDefault()

        # Prefer explicit DB URL from config if provided (handles regional DBs)
        db_url = getattr(Config, 'FIREBASE_DB_URL', None)
        if db_url:
            firebase_app = initialize_app(cred, {'databaseURL': db_url})
        else:
            # Try to derive Realtime Database URL from service account project_id
            # (fallback; may be incorrect for regional RTDB instances)
            if sa_path and os.path.exists(sa_path):
                try:
                    with open(sa_path, 'r', encoding='utf-8') as f:
                        sa_json = json.load(f)
                    project_id = sa_json.get('project_id')
                except Exception:
                    project_id = None
            else:
                project_id = None

            if project_id:
                database_url = f'https://{project_id}.firebaseio.com'
                firebase_app = initialize_app(cred, {'databaseURL': database_url})
            else:
                firebase_app = initialize_app(cred)
    except Exception as e:
        print(f"Error initializing Firebase auth: {e}")

def requires_auth(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        # Allow skipping auth for local dev if configured
        if Config.AUTH_SKIP:
            g.user = {'uid': 'dev-uid'}
            return func(*args, **kwargs)

        auth_header = request.headers.get('Authorization', None)
        if not auth_header:
            return jsonify({'error': 'Missing Authorization header'}), 401
        parts = auth_header.split()
        if parts[0].lower() != 'bearer' or len(parts) != 2:
            return jsonify({'error': 'Invalid Authorization header'}), 401
        token = parts[1]
        try:
            init_firebase()
            decoded = firebase_auth.verify_id_token(token)
            g.user = decoded
        except Exception as e:
            return jsonify({'error': 'Invalid token', 'details': str(e)}), 401
        return func(*args, **kwargs)
    return wrapper
