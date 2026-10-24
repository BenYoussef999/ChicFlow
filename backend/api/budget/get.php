<?php
/**
 * GET /api/budget/get.php?month=2026-10     (month defaults to the current month)
 * Answer: { month, amount, spent, remaining, percent }
 * amount / remaining / percent are null when no budget is set for that month.
 * spent, remaining and percent are CALCULATED by the server from the purchases.
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user = authenticate();

$month = query_param('month', date('Y-m'));
if (!valid_month($month)) json_error('month must look like 2026-10.', 422);

json_success(budget_summary((int)$user['id'], $month));
