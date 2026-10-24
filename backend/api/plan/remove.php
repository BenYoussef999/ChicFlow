<?php
/** POST /api/plan/remove.php   Body: { day_of_week (1-7) }   Removing an empty day is not an error. */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

$day = parse_id($body['day_of_week'] ?? null);
if ($day === null || $day > 7) json_error('day_of_week must be between 1 (Monday) and 7 (Sunday).', 422);

db()->prepare('DELETE FROM weekly_plans WHERE user_id = ? AND day_of_week = ?')->execute([$userId, $day]);

json_success(['plan' => fetch_plan($userId)]);
