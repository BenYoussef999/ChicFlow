<?php
/**
 * POST /api/budget/purchase_add.php
 * Body: { description, amount, purchase_date, client_uuid? }   (retrying with the same client_uuid is safe)
 * Answer: { purchase, budget }   budget = the updated summary of the purchase's month
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

require_fields($body, ['description', 'amount', 'purchase_date']);

$description = clean_string($body['description'], 150);
$amount      = parse_amount($body['amount'], 0.01);
$uuid        = optional_uuid($body);

if (text_length($description) < 2)       json_error('Description must have at least 2 characters.', 422);
if ($amount === null)                    json_error('amount must be greater than 0 (maximum ' . MAX_AMOUNT . ').', 422);
if (!valid_date($body['purchase_date'])) json_error('purchase_date must be a valid date (YYYY-MM-DD).', 422);

$month = substr($body['purchase_date'], 0, 7);

if ($uuid !== null && ($existing = find_purchase_by_uuid($userId, $uuid))) {
    json_success(['purchase' => $existing, 'budget' => budget_summary($userId, $month)], 200);
}

try {
    db()->prepare(
        'INSERT INTO purchases (user_id, client_uuid, description, amount, purchase_date) VALUES (?, ?, ?, ?, ?)'
    )->execute([$userId, $uuid, $description, $amount, $body['purchase_date']]);
} catch (PDOException $e) {
    if ($e->getCode() === '23000' && $uuid !== null && ($existing = find_purchase_by_uuid($userId, $uuid))) {
        json_success(['purchase' => $existing, 'budget' => budget_summary($userId, $month)], 200);
    }
    throw $e;
}

json_success([
    'purchase' => find_purchase($userId, (int)db()->lastInsertId()),
    'budget'   => budget_summary($userId, $month),
], 201);
