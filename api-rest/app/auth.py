import os
from functools import wraps
from flask import request, jsonify, g
from firebase_admin import auth as firebase_auth, credentials, initialize_app
from .config import Config

firebase_app = None

def init_firebase():
    global firebase_app
    if firebase_app is not None:
        return
    cred_path = Config.FIREBASE_CREDENTIALS
    if cred_path:
        cred = credentials.Certificate(cred_path)
        firebase_app = initialize_app(cred)

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
