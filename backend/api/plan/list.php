<?php
/** GET /api/plan/list.php   Answer: { "plan": [ {day_of_week: 1..7, outfit_id} ] }  (1 = Monday ... 7 = Sunday) */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user = authenticate();

json_success(['plan' => fetch_plan((int)$user['id'])]);
