<?php
/** POST /api/budget/purchase_delete.php   Body: { id }   Answer: { deleted, budget } (budget of that month) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

$id = parse_id($body['id'] ?? null);
if ($id === null) json_error('A valid id is required.', 422);

$purchase = find_purchase($userId, $id);
if (!$purchase) json_error('Purchase not found.', 404);

db()->prepare('DELETE FROM purchases WHERE id = ? AND user_id = ?')->execute([$id, $userId]);

json_success([
    'deleted' => true,
    'budget'  => budget_summary($userId, substr($purchase['purchase_date'], 0, 7)),
]);
