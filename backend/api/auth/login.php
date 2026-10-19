<?php
/**
 * POST /api/auth/login.php
 * Body: { "email", "password" }
 * Answers with a token the app must send as "Authorization: Bearer <token>".
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');

// A valid bcrypt hash of a random string. Used when the email does not exist, so
// the answer takes the same time and does not reveal which emails are registered.
const DUMMY_HASH = '$2y$10$t.4b3mhy.U8UQElU.v.ODOcNvf2RtcMRqtTP8seYW2v9gklPxgxc2';

$body = get_json_body();
require_fields($body, ['email', 'password']);

$email    = strtolower(clean_string($body['email'], 150));
$password = (string)$body['password'];

$stmt = db()->prepare(
    'SELECT id, name, email, role, style_preference, password_hash FROM users WHERE email = ?'
);
$stmt->execute([$email]);
$user = $stmt->fetch();

$ok = password_verify($password, $user ? $user['password_hash'] : DUMMY_HASH);
if (!$user || !$ok) {
    json_error('Invalid email or password.', 401);
}

// Housekeeping: remove expired tokens
db()->prepare('DELETE FROM auth_tokens WHERE expires_at < ?')->execute([date('Y-m-d H:i:s')]);

[$token, $expiresAt] = create_token((int)$user['id']);

json_success([
    'token'      => $token,
    'expires_at' => $expiresAt,
    'user'       => public_user($user),
]);
