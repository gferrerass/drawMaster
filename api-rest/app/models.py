from datetime import datetime
from . import db

class UserProfile(db.Model):
    __tablename__ = 'user_profiles'
    id = db.Column(db.Integer, primary_key=True)
    uid = db.Column(db.String(128), unique=True, nullable=False)  # Firebase uid
    display_name = db.Column(db.String(128))
    avatar_url = db.Column(db.String(512))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'uid': self.uid,
            'display_name': self.display_name,
            'avatar_url': self.avatar_url,
            'created_at': self.created_at.isoformat()
        }

class Friend(db.Model):
    __tablename__ = 'friends'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user_profiles.id'), nullable=False)
    friend_uid = db.Column(db.String(128), nullable=False)

class GameRecord(db.Model):
    __tablename__ = 'games'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user_profiles.id'), nullable=False)
    score = db.Column(db.Float, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
