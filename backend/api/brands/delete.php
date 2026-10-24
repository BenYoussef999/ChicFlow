<?php
/** POST /api/brands/delete.php   ADMIN ONLY.   Body: { id } */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
require_admin();
$body = get_json_body();

$id = parse_id($body['id'] ?? null);
if ($id === null) json_error('A valid id is required.', 422);

$stmt = db()->prepare('DELETE FROM brands WHERE id = ?');
$stmt->execute([$id]);
if ($stmt->rowCount() === 0) json_error('Brand not found.', 404);

json_success(['deleted' => true]);
