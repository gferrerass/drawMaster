# DrawMaster API — Quick Reference (for Postman / curl)

Base URL: `http://localhost:5000` (adjust if your server runs elsewhere)

Authentication
- The API expects a Firebase ID token as a Bearer token in the `Authorization` header for protected endpoints: `Authorization: Bearer <ID_TOKEN>`.
- To get an `idToken` via Firebase Auth REST (example):

```powershell
$body = @{ email = 'admin@example.com'; password = 'admin123'; returnSecureToken = $true } | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_API_KEY" -Method Post -ContentType 'application/json' -Body $body
$token = $resp.idToken
```

- For quick local testing you can set `AUTH_SKIP=1` in the server environment to bypass token verification.

Notes before testing
- If you run the API inside Docker, make sure containers were recreated after env changes: `docker compose up -d --build --force-recreate`.
- If you used to have avatar/GCS support, it has been removed — ignore `avatar_url` fields.

Endpoints

## 1) Health
- Method: GET
- URL: `/health`
- Auth: none
- Postman: New GET request → `http://localhost:5000/health`
- curl:
```bash
curl http://localhost:5000/health
```
- Success response (200):
```json
{ "status": "ok" }
```

## 2) Create profile
- Method: POST
- URL: `/profiles`
- Auth: required
- Headers: `Content-Type: application/json`, `Authorization: Bearer <ID_TOKEN>`
- Body (JSON):
```json
{ "display_name": "Your Name" }
```
- Postman: POST → Body → raw → JSON
- curl:
```bash
curl -X POST http://localhost:5000/profiles \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"display_name":"Your Name"}'
```
- Success (201): returns the created profile:
```json
{
  "id": 1,
  "uid": "<firebase-uid>",
  "display_name": "Your Name",
  "created_at": "2025-12-17T10:00:00"
}
```

## 3) Get my profile
- Method: GET
- URL: `/profiles/me`
- Auth: required
- Headers: `Authorization: Bearer <ID_TOKEN>`
- Postman: GET request with Authorization header
- curl:
```bash
curl -H "Authorization: Bearer $ID_TOKEN" http://localhost:5000/profiles/me
```
- Success (200): profile object (same fields as create)
- Error (404): profile not found

## 4) Post a game (score)
- Method: POST
- URL: `/games`
- Auth: required
- Headers: `Content-Type: application/json`, `Authorization: Bearer <ID_TOKEN>`
- Body (JSON):
```json
{ "score": 42 }
```
- curl:
```bash
curl -X POST http://localhost:5000/games \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"score": 42}'
```
- Success (201): returns `{ "id": <game_id>, "score": 42.0, "created_at": "..." }`

## 5) Send friend request
- Method: POST
- URL: `/friends/request`
- Auth: required
- Headers: `Content-Type: application/json`, `Authorization: Bearer <ID_TOKEN>`
- Body (JSON): provide one of these:
```json
{ "to_uid": "OTHER_FIREBASE_UID" }
```
or
```json
{ "to_email": "other@example.com" }
```
- If `to_email` is used the server resolves the Firebase UID for that email.
- curl example:
```bash
curl -X POST http://localhost:5000/friends/request \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"to_uid":"OTHER_FIREBASE_UID"}'
```
- Success (201): returns the FriendRequest record (id, from_uid, to_uid, status, created_at)

## 6) Accept friend request
- Method: POST
- URL: `/friends/accept`
- Auth: required
- Headers: `Content-Type: application/json`, `Authorization: Bearer <ID_TOKEN>`
- Body (JSON):
```json
{ "request_id": 123 }
```
- curl:
```bash
curl -X POST http://localhost:5000/friends/accept \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"request_id":123}'
```
- Success (200): returns updated FriendRequest with `status: "accepted"`.
- Error (403): if current user is not the recipient.

## 7) List friends
- Method: GET
- URL: `/friends`
- Auth: required
- Headers: `Authorization: Bearer <ID_TOKEN>`
- curl:
```bash
curl -H "Authorization: Bearer $ID_TOKEN" http://localhost:5000/friends
```
- Success (200):
```json
{ "friends": [ { "friend_uid": "uid1", "display_name": "Friend 1" }, ... ] }
```

Troubleshooting & tips
- Tokens: `idToken` expires (typically 1 hour). Use `refreshToken` to get a new `idToken` via `https://securetoken.googleapis.com/v1/token?key=YOUR_API_KEY` if needed.
- If you see `Invalid token` or errors about credentials, ensure backend has `FIREBASE_CREDENTIALS` set to a valid service-account JSON path and the container has access to that file.
- If DB errors occur, check container logs:
```bash
docker compose logs web
```
- If you want a Postman collection file, I can generate it (JSON) so you can import all requests at once.

— end of document —
