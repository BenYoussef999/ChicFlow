<?php
/**
 * POST /api/auth/register.php
 * Body: { "name", "email", "password", "style_preference"? }
 * Creates a normal user (role is ALWAYS 'user', whatever the client sends)
 * and logs them in right away.
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');

$body = get_json_body();
require_fields($body, ['name', 'email', 'password']);

$name     = clean_string($body['name'], 100);
$email    = strtolower(clean_string($body['email'], 150));
$password = (string)$body['password'];
$style    = normalize_style($body['style_preference'] ?? null);

if (!valid_name($name))         json_error('Name must have at least 2 characters.', 422);
if (!valid_email($email))       json_error('Invalid email address.', 422);
if (!valid_password($password)) json_error('Password must have between 8 and 72 characters.', 422);

$check = db()->prepare('SELECT id FROM users WHERE email = ?');
$check->execute([$email]);
if ($check->fetch()) {
    json_error('This email is already registered.', 409);
}

try {
    $insert = db()->prepare(
        'INSERT INTO users (name, email, password_hash, style_preference) VALUES (?, ?, ?, ?)'
    );
    $insert->execute([$name, $email, password_hash($password, PASSWORD_DEFAULT), $style]);
} catch (PDOException $e) {
    // Two registrations with the same email at the same moment
    if ($e->getCode() === '23000') {
        json_error('This email is already registered.', 409);
    }
    throw $e;
}

$userId = (int)db()->lastInsertId();
[$token, $expiresAt] = create_token($userId);

$row = db()->prepare('SELECT id, name, email, role, style_preference FROM users WHERE id = ?');
$row->execute([$userId]);

json_success([
    'token'      => $token,
    'expires_at' => $expiresAt,
    'user'       => public_user($row->fetch()),
], 201);
