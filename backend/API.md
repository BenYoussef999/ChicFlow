# ChicFlow API reference

All answers: `{"success":true,"data":{...}}` or `{"success":false,"message":"..."}`.
Protected endpoints need the header `Authorization: Bearer <token>`.

## Auth (stage 2)

| Endpoint | Method | Auth | Body | Success data |
|---|---|---|---|---|
| `/api/auth/register.php` | POST | no | `name`, `email`, `password`, `style_preference`? | `token`, `expires_at`, `user` (HTTP 201) |
| `/api/auth/login.php` | POST | no | `email`, `password` | `token`, `expires_at`, `user` |
| `/api/auth/logout.php` | POST | yes | none | `message` |
| `/api/auth/profile.php` | GET | yes | none | `user` |
| `/api/auth/profile.php` | POST | yes | `name`?, `style_preference`? | `user` |

`user` = `{ id, name, email, role, style_preference }`

Errors: 401 bad credentials / missing or expired token, 405 wrong method,
409 email already registered, 422 validation error, 500 server error.

Allowed `style_preference` values: classic, casual, sporty, elegant, streetwear (or empty).

## Making an admin
Roles cannot be set through the API. After registering, run in phpMyAdmin:
`UPDATE users SET role='admin' WHERE email='you@example.com';`
