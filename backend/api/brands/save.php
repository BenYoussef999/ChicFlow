<?php
/**
 * POST /api/brands/save.php        ADMIN ONLY (HTTP 403 for normal users)
 * Body: { id?, name, category, price_range?, website_url? }
 * Without id a brand is created (201), with id it is updated.
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
require_admin();
$body = get_json_body();

require_fields($body, ['name', 'category']);

$name     = clean_string($body['name'], 100);
$category = clean_string($body['category'], 50);
$price    = optional_string($body, 'price_range', 50) ?? '';
$website  = normalize_website(optional_string($body, 'website_url', 2000) ?? '');

if (text_length($name) < 2)     json_error('Brand name must have at least 2 characters.', 422);
if (text_length($category) < 2) json_error('Category must have at least 2 characters.', 422);
if ($website === null)          json_error('website_url is not a valid website address.', 422);

$price   = $price === '' ? null : $price;
$website = $website === '' ? null : $website;

if (isset($body['id'])) {
    $id = parse_id($body['id']);
    if ($id === null) json_error('Invalid id.', 422);
    $exists = db()->prepare('SELECT id FROM brands WHERE id = ?');
    $exists->execute([$id]);
    if (!$exists->fetch()) json_error('Brand not found.', 404);

    db()->prepare('UPDATE brands SET name = ?, category = ?, price_range = ?, website_url = ? WHERE id = ?')
        ->execute([$name, $category, $price, $website, $id]);
    $status = 200;
} else {
    db()->prepare('INSERT INTO brands (name, category, price_range, website_url) VALUES (?, ?, ?, ?)')
        ->execute([$name, $category, $price, $website]);
    $id     = (int)db()->lastInsertId();
    $status = 201;
}

$stmt = db()->prepare('SELECT id, name, category, price_range, website_url FROM brands WHERE id = ?');
$stmt->execute([$id]);
json_success(['brand' => brand_json($stmt->fetch())], $status);
