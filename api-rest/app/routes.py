import io
import os
from flask import Blueprint, current_app, request, jsonify, g
from . import db
from .models import UserProfile, Friend, GameRecord
from .auth import requires_auth
from .storage import GCSClient

bp = Blueprint('api', __name__)


@bp.route('/health')
def health():
    return jsonify({'status': 'ok'})


@bp.route('/profiles', methods=['POST'])
@requires_auth
def create_profile():
    data = request.json or {}
    uid = g.user.get('uid')
    if not uid:
        return jsonify({'error': 'missing uid in token'}), 400
    profile = UserProfile.query.filter_by(uid=uid).first()
    if profile:
        return jsonify(profile.to_dict()), 200
    profile = UserProfile(uid=uid, display_name=data.get('display_name'))
    db.session.add(profile)
    db.session.commit()
    return jsonify(profile.to_dict()), 201


@bp.route('/profiles/me', methods=['GET'])
@requires_auth
def get_my_profile():
    uid = g.user.get('uid')
    profile = UserProfile.query.filter_by(uid=uid).first()
    if not profile:
        return jsonify({'error': 'profile not found'}), 404
    return jsonify(profile.to_dict())


@bp.route('/profiles/me/avatar', methods=['POST'])
@requires_auth
def upload_avatar():
    if 'file' not in request.files:
        return jsonify({'error': 'no file part'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'no selected file'}), 400

    # upload to GCS if configured
    bucket = os.getenv('GCS_BUCKET')
    if not bucket:
        return jsonify({'error': 'GCS_BUCKET not configured'}), 500

    client = GCSClient(bucket)
    # destination name: avatars/<uid>/<filename>
    uid = g.user.get('uid')
    dest_name = f"avatars/{uid}/{file.filename}"
    public_url = client.upload_file(file.stream, dest_name, content_type=file.mimetype)

    # update profile
    profile = UserProfile.query.filter_by(uid=uid).first()
    if not profile:
        profile = UserProfile(uid=uid)
        db.session.add(profile)
    profile.avatar_url = public_url
    db.session.commit()

    return jsonify({'avatar_url': public_url})


@bp.route('/games', methods=['POST'])
@requires_auth
def post_game():
    data = request.json or {}
    uid = g.user.get('uid')
    profile = UserProfile.query.filter_by(uid=uid).first()
    if not profile:
        profile = UserProfile(uid=uid)
        db.session.add(profile)
        db.session.commit()
    score = float(data.get('score', 0))
    game = GameRecord(user_id=profile.id, score=score)
    db.session.add(game)
    db.session.commit()
    return jsonify({'id': game.id, 'score': game.score, 'created_at': game.created_at.isoformat()}), 201
