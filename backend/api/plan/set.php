<?php
/** POST /api/plan/set.php   Body: { day_of_week (1-7), outfit_id }   One outfit per day: replaces the old one. */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

$day      = parse_id($body['day_of_week'] ?? null);
$outfitId = parse_id($body['outfit_id'] ?? null);
if ($day === null || $day > 7) json_error('day_of_week must be between 1 (Monday) and 7 (Sunday).', 422);
if ($outfitId === null)        json_error('A valid outfit_id is required.', 422);
if (!find_outfit($userId, $outfitId)) json_error('Outfit not found.', 404);

db()->prepare(
    'INSERT INTO weekly_plans (user_id, day_of_week, outfit_id) VALUES (?, ?, ?)
     ON DUPLICATE KEY UPDATE outfit_id = VALUES(outfit_id)'
)->execute([$userId, $day, $outfitId]);

json_success(['plan' => fetch_plan($userId)]);
