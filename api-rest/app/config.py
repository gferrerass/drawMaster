import os

class Config:
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL', 'sqlite:///data.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    FIREBASE_CREDENTIALS = os.getenv('FIREBASE_CREDENTIALS')
    FIREBASE_DB_URL = os.getenv('FIREBASE_DB_URL')
    GCS_BUCKET = os.getenv('GCS_BUCKET')
    AUTH_SKIP = os.getenv('AUTH_SKIP', '0') in ('1', 'true', 'True')
