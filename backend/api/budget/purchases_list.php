<?php
/** GET /api/budget/purchases_list.php[?month=2026-10]   (without month: all purchases) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user = authenticate();

$month = query_param('month');
if ($month !== '' && !valid_month($month)) json_error('month must look like 2026-10.', 422);

json_success(['purchases' => fetch_purchases((int)$user['id'], $month === '' ? null : $month)]);
