<?php
/**
 * GET  /api/auth/profile.php  -> the logged-in user's profile
 * POST /api/auth/profile.php  -> update it. Body: { "name"?, "style_preference"? }
 * Email and role cannot be changed here.
 */
require __DIR__ . '/../../core/bootstrap.php';

$user   = authenticate();
$method = $_SERVER['REQUEST_METHOD'] ?? '';

if ($method === 'GET') {
    json_success(['user' => public_user($user)]);
}

if ($method !== 'POST') {
    json_error('Method not allowed. Use GET or POST.', 405);
}

$body   = get_json_body();
$fields = [];
$values = [];

if (array_key_exists('name', $body)) {
    $name = clean_string($body['name'], 100);
    if (!valid_name($name)) json_error('Name must have at least 2 characters.', 422);
    $fields[] = 'name = ?';
    $values[] = $name;
}
if (array_key_exists('style_preference', $body)) {
    $fields[] = 'style_preference = ?';
    $values[] = normalize_style($body['style_preference']);
}
if (!$fields) {
    json_error('Nothing to update. Send "name" and/or "style_preference".', 422);
}

$values[] = $user['id'];          // WHERE id = ? : always the token's user, never client input
db()->prepare('UPDATE users SET ' . implode(', ', $fields) . ' WHERE id = ?')->execute($values);

$stmt = db()->prepare('SELECT id, name, email, role, style_preference FROM users WHERE id = ?');
$stmt->execute([$user['id']]);
json_success(['user' => public_user($stmt->fetch())]);
