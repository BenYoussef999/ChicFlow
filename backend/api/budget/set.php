<?php
/** POST /api/budget/set.php   Body: { month, amount }   (creates or replaces the budget of that month) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

require_fields($body, ['month', 'amount']);
if (!valid_month($body['month'])) json_error('month must look like 2026-10.', 422);
$amount = parse_amount($body['amount'], 0.01);
if ($amount === null) json_error('amount must be greater than 0 (maximum ' . MAX_AMOUNT . ').', 422);

db()->prepare(
    'INSERT INTO budgets (user_id, month, amount) VALUES (?, ?, ?)
     ON DUPLICATE KEY UPDATE amount = VALUES(amount)'
)->execute([$userId, $body['month'], $amount]);

json_success(budget_summary($userId, $body['month']));
