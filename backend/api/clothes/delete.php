<?php
/**
 * POST /api/clothes/delete.php   Body: { id }
 * Outfits that are left without any clothes are deleted too (like in the app).
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user = authenticate();
$body = get_json_body();

$id = parse_id($body['id'] ?? null);
if ($id === null) json_error('A valid id is required.', 422);

db()->beginTransaction();
try {
    $stmt = db()->prepare('DELETE FROM clothing_items WHERE id = ? AND user_id = ?');
    $stmt->execute([$id, $user['id']]);
    if ($stmt->rowCount() === 0) {
        db()->rollBack();
        json_error('Item not found.', 404);
    }
    delete_empty_outfits((int)$user['id']);
    db()->commit();
} catch (Throwable $e) {
    if (db()->inTransaction()) db()->rollBack();
    throw $e;
}

json_success(['deleted' => true]);
