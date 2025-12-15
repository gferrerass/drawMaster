import io
import os
from flask import Blueprint, current_app, request, jsonify, g
from . import db
from .models import UserProfile, Friend, GameRecord
from .auth import requires_auth
from .storage import GCSClient
from .models import FriendRequest
from datetime import datetime

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


# Friends: send request
@bp.route('/friends/request', methods=['POST'])
@requires_auth
def send_friend_request():
    data = request.json or {}
    to_uid = data.get('to_uid')
    if not to_uid:
        return jsonify({'error': 'to_uid required'}), 400
    from_uid = g.user.get('uid')
    if from_uid == to_uid:
        return jsonify({'error': 'cannot friend yourself'}), 400

    # check existing pending or accepted
    existing = FriendRequest.query.filter_by(from_uid=from_uid, to_uid=to_uid).first()
    if existing and existing.status == 'pending':
        return jsonify({'error': 'request already pending'}), 400
    if existing and existing.status == 'accepted':
        return jsonify({'error': 'already friends'}), 400

    fr = FriendRequest(from_uid=from_uid, to_uid=to_uid, status='pending')
    db.session.add(fr)
    db.session.commit()
    return jsonify(fr.to_dict()), 201


# Friends: accept request
@bp.route('/friends/accept', methods=['POST'])
@requires_auth
def accept_friend_request():
    data = request.json or {}
    req_id = data.get('request_id')
    if not req_id:
        return jsonify({'error': 'request_id required'}), 400
    uid = g.user.get('uid')
    fr = FriendRequest.query.get(req_id)
    if not fr:
        return jsonify({'error': 'request not found'}), 404
    if fr.to_uid != uid:
        return jsonify({'error': 'not authorized to accept this request'}), 403
    if fr.status == 'accepted':
        return jsonify({'error': 'request already accepted'}), 400

    # mark accepted
    fr.status = 'accepted'
    fr.responded_at = datetime.utcnow()

    # ensure profiles exist
    from_profile = UserProfile.query.filter_by(uid=fr.from_uid).first()
    if not from_profile:
        from_profile = UserProfile(uid=fr.from_uid)
        db.session.add(from_profile)
        db.session.flush()

    to_profile = UserProfile.query.filter_by(uid=fr.to_uid).first()
    if not to_profile:
        to_profile = UserProfile(uid=fr.to_uid)
        db.session.add(to_profile)
        db.session.flush()

    # create Friend records for both sides if not exist
    existing_a = Friend.query.filter_by(user_id=to_profile.id, friend_uid=from_profile.uid).first()
    if not existing_a:
        f1 = Friend(user_id=to_profile.id, friend_uid=from_profile.uid)
        db.session.add(f1)

    existing_b = Friend.query.filter_by(user_id=from_profile.id, friend_uid=to_profile.uid).first()
    if not existing_b:
        f2 = Friend(user_id=from_profile.id, friend_uid=to_profile.uid)
        db.session.add(f2)

    db.session.commit()
    return jsonify(fr.to_dict())


@bp.route('/friends', methods=['GET'])
@requires_auth
def list_friends():
    uid = g.user.get('uid')
    profile = UserProfile.query.filter_by(uid=uid).first()
    if not profile:
        return jsonify({'friends': []})
    friends = Friend.query.filter_by(user_id=profile.id).all()
    result = []
    for f in friends:
        # try to include display name if profile exists
        p = UserProfile.query.filter_by(uid=f.friend_uid).first()
        result.append({'friend_uid': f.friend_uid, 'display_name': p.display_name if p else None, 'avatar_url': p.avatar_url if p else None})
    return jsonify({'friends': result})
