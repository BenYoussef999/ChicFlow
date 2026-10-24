<?php
/** POST /api/outfits/favorite.php   Body: { id, is_favorite }   (true = favorite, false = not) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

$id       = parse_id($body['id'] ?? null);
$favorite = parse_bool($body['is_favorite'] ?? null);
if ($id === null)       json_error('A valid id is required.', 422);
if ($favorite === null) json_error('is_favorite must be true or false.', 422);
if (!find_outfit($userId, $id)) json_error('Outfit not found.', 404);

db()->prepare('UPDATE outfits SET is_favorite = ? WHERE id = ? AND user_id = ?')
    ->execute([$favorite ? 1 : 0, $id, $userId]);

json_success(['outfit' => find_outfit($userId, $id)]);
