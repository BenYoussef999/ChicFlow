<?php
/**
 * POST /api/outfits/create.php
 * Body: { name, occasion, item_ids[], explanation?, source?, is_favorite?, client_uuid? }
 * Every item must belong to the logged-in user. Retrying with the same client_uuid is safe.
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');
$user   = authenticate();
$userId = (int)$user['id'];
$body   = get_json_body();

require_fields($body, ['name', 'occasion']);

$name        = clean_string($body['name'], 100);
$explanation = optional_string($body, 'explanation', 1000);
$source      = $body['source'] ?? 'manual';
$favorite    = parse_bool($body['is_favorite'] ?? false);
$uuid        = optional_uuid($body);

if (text_length($name) < 2)                          json_error('Name must have at least 2 characters.', 422);
if (!in_array($body['occasion'], OCCASIONS, true))   json_error('Unknown occasion. Use: ' . implode(', ', OCCASIONS) . '.', 422);
if (!in_array($source, OUTFIT_SOURCES, true))        json_error('Unknown source. Use: ' . implode(', ', OUTFIT_SOURCES) . '.', 422);
if ($favorite === null)                              json_error('is_favorite must be true or false.', 422);

// item_ids: 1 to 20 different positive integers
$rawIds = $body['item_ids'] ?? null;
if (!is_array($rawIds) || count($rawIds) < 1 || count($rawIds) > 20) {
    json_error('item_ids must be a list of 1 to 20 clothing item ids.', 422);
}
$itemIds = [];
foreach ($rawIds as $raw) {
    $itemId = parse_id($raw);
    if ($itemId === null) json_error('item_ids must contain only positive integers.', 422);
    $itemIds[$itemId] = $itemId;
}
$itemIds = array_values($itemIds);

if ($uuid !== null && ($existing = find_outfit_by_uuid($userId, $uuid))) {
    json_success(['outfit' => $existing], 200);
}

// Authorization: all items must be this user's own
$placeholders = implode(',', array_fill(0, count($itemIds), '?'));
$own = db()->prepare("SELECT COUNT(*) FROM clothing_items WHERE user_id = ? AND id IN ($placeholders)");
$own->execute(array_merge([$userId], $itemIds));
if ((int)$own->fetchColumn() !== count($itemIds)) {
    json_error('One or more clothing items do not exist.', 422);
}

db()->beginTransaction();
try {
    db()->prepare(
        'INSERT INTO outfits (user_id, client_uuid, name, occasion, explanation, source, is_favorite)
         VALUES (?, ?, ?, ?, ?, ?, ?)'
    )->execute([$userId, $uuid, $name, $body['occasion'], $explanation, $source, $favorite ? 1 : 0]);
    $outfitId = (int)db()->lastInsertId();

    $link = db()->prepare('INSERT INTO outfit_items (outfit_id, item_id) VALUES (?, ?)');
    foreach ($itemIds as $itemId) {
        $link->execute([$outfitId, $itemId]);
    }
    db()->commit();
} catch (PDOException $e) {
    if (db()->inTransaction()) db()->rollBack();
    if ($e->getCode() === '23000' && $uuid !== null && ($existing = find_outfit_by_uuid($userId, $uuid))) {
        json_success(['outfit' => $existing], 200);
    }
    throw $e;
} catch (Throwable $e) {
    if (db()->inTransaction()) db()->rollBack();
    throw $e;
}

json_success(['outfit' => find_outfit($userId, $outfitId)], 201);
