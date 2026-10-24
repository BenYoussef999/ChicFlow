<?php
/**
 * POST /api/clothes/update.php
 * Body: { id, name?, category?, color?, price?, date_added? }  (send only what changes)
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user = authenticate();
$body = get_json_body();

$id = parse_id($body['id'] ?? null);
if ($id === null) json_error('A valid id is required.', 422);
if (!find_item((int)$user['id'], $id)) json_error('Item not found.', 404);

$fields = [];
$values = [];

if (array_key_exists('name', $body)) {
    $name = clean_string(is_string($body['name']) ? $body['name'] : '', 100);
    if (text_length($name) < 2) json_error('Name must have at least 2 characters.', 422);
    $fields[] = 'name = ?';  $values[] = $name;
}
if (array_key_exists('category', $body)) {
    if (!in_array($body['category'], CATEGORIES, true)) json_error('Unknown category.', 422);
    $fields[] = 'category = ?';  $values[] = $body['category'];
}
if (array_key_exists('color', $body)) {
    if (!in_array($body['color'], COLORS, true)) json_error('Unknown color.', 422);
    $fields[] = 'color = ?';  $values[] = $body['color'];
}
if (array_key_exists('price', $body)) {
    $price = parse_amount($body['price']);
    if ($price === null) json_error('Price must be a number between 0 and ' . MAX_AMOUNT . '.', 422);
    $fields[] = 'price = ?';  $values[] = $price;
}
if (array_key_exists('date_added', $body)) {
    if (!valid_date($body['date_added'])) json_error('date_added must be a valid date (YYYY-MM-DD).', 422);
    $fields[] = 'date_added = ?';  $values[] = $body['date_added'];
}
if (!$fields) json_error('Nothing to update.', 422);

$values[] = $id;
$values[] = $user['id'];
db()->prepare('UPDATE clothing_items SET ' . implode(', ', $fields) . ' WHERE id = ? AND user_id = ?')
    ->execute($values);

json_success(['item' => find_item((int)$user['id'], $id)]);
