import io
import os
from flask import Blueprint, current_app, request, jsonify, g
from . import db
from .models import UserProfile, Friend, GameRecord
from .auth import requires_auth, init_firebase, firebase_auth
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
    to_email = data.get('to_email')
    if not to_uid and not to_email:
        return jsonify({'error': 'to_uid or to_email required'}), 400

    # If client provided an email, resolve it to a Firebase UID
    if to_email and not to_uid:
        try:
            init_firebase()
            user = firebase_auth.get_user_by_email(to_email)
            to_uid = user.uid
        except Exception as e:
            return jsonify({'error': 'user with provided email not found', 'details': str(e)}), 404
    from_uid = g.user.get('uid')
    if from_uid == to_uid:
        return jsonify({'error': 'cannot friend yourself'}), 400

    # check if they are already friends (either direction)
    from_profile = UserProfile.query.filter_by(uid=from_uid).first()
    to_profile = UserProfile.query.filter_by(uid=to_uid).first()
    already_friends = False
    if from_profile:
        existing = Friend.query.filter_by(user_id=from_profile.id, friend_uid=to_uid).first()
        if existing:
            already_friends = True
    if not already_friends and to_profile:
        existing_rev = Friend.query.filter_by(user_id=to_profile.id, friend_uid=from_uid).first()
        if existing_rev:
            already_friends = True
    if already_friends:
        return jsonify({'error': 'already friends'}), 400

    # check existing pending or accepted
    # check existing pending or accepted in same direction
    existing = FriendRequest.query.filter_by(from_uid=from_uid, to_uid=to_uid).first()
    if existing and existing.status == 'pending':
        return jsonify({'error': 'request already pending'}), 400
    if existing and existing.status == 'accepted':
        return jsonify({'error': 'already friends'}), 400

    # check if there is a pending request in the opposite direction
    reverse = FriendRequest.query.filter_by(from_uid=to_uid, to_uid=from_uid, status='pending').first()
    if reverse:
        return jsonify({'error': 'friend request already exists with that person'}), 400

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


@bp.route('/friends/reject', methods=['POST'])
@requires_auth
def reject_friend_request():
    data = request.json or {}
    req_id = data.get('request_id')
    if not req_id:
        return jsonify({'error': 'request_id required'}), 400
    uid = g.user.get('uid')
    fr = FriendRequest.query.get(req_id)
    if not fr:
        return jsonify({'error': 'request not found'}), 404
    if fr.to_uid != uid:
        return jsonify({'error': 'not authorized to reject this request'}), 403
    if fr.status != 'pending':
        return jsonify({'error': 'request already responded'}), 400

    # delete the friend request so the sender may send again later
    db.session.delete(fr)
    db.session.commit()
    return jsonify({'status': 'deleted', 'id': req_id})


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
        email = None
        try:
            init_firebase()
            user = firebase_auth.get_user(f.friend_uid)
            email = user.email
        except Exception:
            pass
        result.append({'friend_uid': f.friend_uid, 'email': email, 'display_name': p.display_name if p else None})
    return jsonify({'friends': result})


@bp.route('/friends/requests', methods=['GET'])
@requires_auth
def list_friend_requests():
    uid = g.user.get('uid')
    # pending requests where current user is the recipient
    reqs = FriendRequest.query.filter_by(to_uid=uid, status='pending').all()
    out = []
    for r in reqs:
        p = UserProfile.query.filter_by(uid=r.from_uid).first()
        # try to get email from Firebase as fallback
        email = None
        try:
            init_firebase()
            user = firebase_auth.get_user(r.from_uid)
            email = user.email
        except Exception:
            pass
        out.append({'id': r.id, 'from_uid': r.from_uid, 'from_email': email, 'display_name': p.display_name if p else None, 'created_at': r.created_at.isoformat() if r.created_at else None})
    return jsonify({'requests': out})


@bp.route('/friends/requests/sent', methods=['GET'])
@requires_auth
def list_sent_friend_requests():
    uid = g.user.get('uid')
    # pending requests where current user is the sender
    reqs = FriendRequest.query.filter_by(from_uid=uid, status='pending').all()
    out = []
    for r in reqs:
        p = UserProfile.query.filter_by(uid=r.to_uid).first()
        email = None
        try:
            init_firebase()
            user = firebase_auth.get_user(r.to_uid)
            email = user.email
        except Exception:
            pass
        out.append({'id': r.id, 'to_uid': r.to_uid, 'to_email': email, 'display_name': p.display_name if p else None, 'created_at': r.created_at.isoformat() if r.created_at else None})
    return jsonify({'requests': out})
