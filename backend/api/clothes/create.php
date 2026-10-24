<?php
/**
 * POST /api/clothes/create.php
 * Body: { name, category, color, price?, date_added?, client_uuid? }
 * If client_uuid was already used by this user, the EXISTING item is returned (HTTP 200)
 * instead of creating a duplicate: the app can safely retry after a lost connection.
 * Photos are not accepted here yet (they will have their own upload endpoint).
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user = authenticate();
$body = get_json_body();

require_fields($body, ['name', 'category', 'color']);

$name  = clean_string($body['name'], 100);
$price = parse_amount($body['price'] ?? 0);
$date  = $body['date_added'] ?? date('Y-m-d');
$uuid  = optional_uuid($body);

if (text_length($name) < 2)                              json_error('Name must have at least 2 characters.', 422);
if (!in_array($body['category'], CATEGORIES, true))      json_error('Unknown category.', 422);
if (!in_array($body['color'], COLORS, true))             json_error('Unknown color.', 422);
if ($price === null)                                     json_error('Price must be a number between 0 and ' . MAX_AMOUNT . '.', 422);
if (!valid_date($date))                                  json_error('date_added must be a valid date (YYYY-MM-DD).', 422);

if ($uuid !== null && ($existing = find_item_by_uuid((int)$user['id'], $uuid))) {
    json_success(['item' => $existing], 200);
}

try {
    db()->prepare(
        'INSERT INTO clothing_items (user_id, client_uuid, name, category, color, price, date_added)
         VALUES (?, ?, ?, ?, ?, ?, ?)'
    )->execute([$user['id'], $uuid, $name, $body['category'], $body['color'], $price, $date]);
} catch (PDOException $e) {
    // Same client_uuid sent twice at the same moment: return the row that won
    if ($e->getCode() === '23000' && $uuid !== null && ($existing = find_item_by_uuid((int)$user['id'], $uuid))) {
        json_success(['item' => $existing], 200);
    }
    throw $e;
}

json_success(['item' => find_item((int)$user['id'], (int)db()->lastInsertId())], 201);
