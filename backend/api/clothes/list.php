<?php
/**
 * GET /api/clothes/list.php?search=&category=&color=&sort=
 *   search   text found in the name or color
 *   category shirt | trousers | jacket | shoes | accessory | other
 *   color    one of the 12 colors
 *   sort     newest (default) | name | price_asc | price_desc
 * Answer: { "items": [ {id, client_uuid, name, category, color, price, photo_url, date_added, updated_at} ] }
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user = authenticate();

$search   = clean_string(query_param('search'), 100);
$category = query_param('category');
$color    = query_param('color');
$sort     = query_param('sort', 'newest');

if ($category !== '' && !in_array($category, CATEGORIES, true)) json_error('Unknown category.', 422);
if ($color !== '' && !in_array($color, COLORS, true))           json_error('Unknown color.', 422);

// The ORDER BY text comes from this fixed list, never from the request
$orders = [
    'newest'     => 'date_added DESC, id DESC',
    'name'       => 'name ASC, id ASC',
    'price_asc'  => 'price ASC, id ASC',
    'price_desc' => 'price DESC, id ASC',
];
if (!isset($orders[$sort])) json_error('Unknown sort. Use: ' . implode(', ', array_keys($orders)) . '.', 422);

$sql    = 'SELECT ' . ITEM_COLUMNS . ' FROM clothing_items WHERE user_id = ?';
$params = [$user['id']];
if ($category !== '') { $sql .= ' AND category = ?'; $params[] = $category; }
if ($color !== '')    { $sql .= ' AND color = ?';    $params[] = $color; }
if ($search !== '') {
    $like    = '%' . addcslashes($search, '%_\\') . '%';   // the user's % and _ are plain characters
    $sql    .= ' AND (name LIKE ? OR color LIKE ?)';
    $params[] = $like;
    $params[] = $like;
}

$stmt = db()->prepare($sql . ' ORDER BY ' . $orders[$sort]);
$stmt->execute($params);

json_success(['items' => array_map('item_json', $stmt->fetchAll())]);
