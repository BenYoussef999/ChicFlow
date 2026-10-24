<?php
/**
 * GET /api/sync/pull.php
 * Everything the logged-in user owns, in ONE call. The app replaces its local copy with it
 * (after pushing its own pending changes first). Data volumes are small, so a full snapshot is
 * simpler and more robust than "changes since" (which would also need deletion tombstones).
 *
 * Answer: { server_time, user, items[], outfits[], budgets[], purchases[], plan[], brands[] }
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user   = authenticate();
$userId = (int)$user['id'];

$stmt = db()->prepare('SELECT ' . ITEM_COLUMNS . ' FROM clothing_items WHERE user_id = ? ORDER BY id');
$stmt->execute([$userId]);
$items = array_map('item_json', $stmt->fetchAll());

$stmt = db()->prepare('SELECT month, amount FROM budgets WHERE user_id = ? ORDER BY month');
$stmt->execute([$userId]);
$budgets = array_map(
    fn($r) => ['month' => $r['month'], 'amount' => (float)$r['amount']],
    $stmt->fetchAll()
);

json_success([
    'server_time' => date('Y-m-d H:i:s'),
    'user'        => public_user($user),
    'items'       => $items,
    'outfits'     => fetch_outfits($userId),
    'budgets'     => $budgets,
    'purchases'   => fetch_purchases($userId),
    'plan'        => fetch_plan($userId),
    'brands'      => fetch_brands(),
]);
