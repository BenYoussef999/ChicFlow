<?php
/** POST /api/outfits/delete.php   Body: { id }   (its weekly-plan entries are removed too) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user = authenticate();
$body = get_json_body();

$id = parse_id($body['id'] ?? null);
if ($id === null) json_error('A valid id is required.', 422);

$stmt = db()->prepare('DELETE FROM outfits WHERE id = ? AND user_id = ?');
$stmt->execute([$id, $user['id']]);
if ($stmt->rowCount() === 0) json_error('Outfit not found.', 404);

json_success(['deleted' => true]);
