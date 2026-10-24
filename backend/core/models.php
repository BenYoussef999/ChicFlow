<?php
declare(strict_types=1);
/**
 * Database rows -> the JSON shapes the app receives. Used by several endpoints (and sync/pull).
 * Every function takes the user id: a user can only ever read their own rows.
 */

const ITEM_COLUMNS     = 'id, client_uuid, name, category, color, price, photo_url, date_added, updated_at';
const OUTFIT_COLUMNS   = 'id, client_uuid, name, occasion, explanation, source, is_favorite, updated_at';
const PURCHASE_COLUMNS = 'id, client_uuid, description, amount, purchase_date';

function item_json(array $r): array
{
    return [
        'id'          => (int)$r['id'],
        'client_uuid' => $r['client_uuid'],
        'name'        => $r['name'],
        'category'    => $r['category'],
        'color'       => $r['color'],
        'price'       => (float)$r['price'],
        'photo_url'   => $r['photo_url'],
        'date_added'  => $r['date_added'],
        'updated_at'  => $r['updated_at'],
    ];
}

function purchase_json(array $r): array
{
    return [
        'id'            => (int)$r['id'],
        'client_uuid'   => $r['client_uuid'],
        'description'   => $r['description'],
        'amount'        => (float)$r['amount'],
        'purchase_date' => $r['purchase_date'],
    ];
}

function find_item(int $userId, int $id): ?array
{
    $stmt = db()->prepare('SELECT ' . ITEM_COLUMNS . ' FROM clothing_items WHERE id = ? AND user_id = ?');
    $stmt->execute([$id, $userId]);
    $row = $stmt->fetch();
    return $row ? item_json($row) : null;
}

function find_item_by_uuid(int $userId, string $uuid): ?array
{
    $stmt = db()->prepare('SELECT ' . ITEM_COLUMNS . ' FROM clothing_items WHERE user_id = ? AND client_uuid = ?');
    $stmt->execute([$userId, $uuid]);
    $row = $stmt->fetch();
    return $row ? item_json($row) : null;
}

function find_purchase(int $userId, int $id): ?array
{
    $stmt = db()->prepare('SELECT ' . PURCHASE_COLUMNS . ' FROM purchases WHERE id = ? AND user_id = ?');
    $stmt->execute([$id, $userId]);
    $row = $stmt->fetch();
    return $row ? purchase_json($row) : null;
}

function find_purchase_by_uuid(int $userId, string $uuid): ?array
{
    $stmt = db()->prepare('SELECT ' . PURCHASE_COLUMNS . ' FROM purchases WHERE user_id = ? AND client_uuid = ?');
    $stmt->execute([$userId, $uuid]);
    $row = $stmt->fetch();
    return $row ? purchase_json($row) : null;
}

/** Purchases of one month ("2026-10") or all of them (null), newest first. */
function fetch_purchases(int $userId, ?string $month = null): array
{
    $sql    = 'SELECT ' . PURCHASE_COLUMNS . ' FROM purchases WHERE user_id = ?';
    $params = [$userId];
    if ($month !== null) {
        [$first, $last] = month_range($month);
        $sql     .= ' AND purchase_date BETWEEN ? AND ?';
        $params[] = $first;
        $params[] = $last;
    }
    $stmt = db()->prepare($sql . ' ORDER BY purchase_date DESC, id DESC');
    $stmt->execute($params);
    return array_map('purchase_json', $stmt->fetchAll());
}

/** Total spent in a month: calculated here, never stored. */
function month_spent(int $userId, string $month): float
{
    [$first, $last] = month_range($month);
    $stmt = db()->prepare(
        'SELECT COALESCE(SUM(amount), 0) FROM purchases WHERE user_id = ? AND purchase_date BETWEEN ? AND ?'
    );
    $stmt->execute([$userId, $first, $last]);
    return round((float)$stmt->fetchColumn(), 2);
}

/** The monthly budget summary: amount, spent, remaining and percent (null when no budget). */
function budget_summary(int $userId, string $month): array
{
    $stmt = db()->prepare('SELECT amount FROM budgets WHERE user_id = ? AND month = ?');
    $stmt->execute([$userId, $month]);
    $amount = $stmt->fetchColumn();
    $spent  = month_spent($userId, $month);

    if ($amount === false) {
        return ['month' => $month, 'amount' => null, 'spent' => $spent, 'remaining' => null, 'percent' => null];
    }
    $amount = (float)$amount;
    return [
        'month'     => $month,
        'amount'    => $amount,
        'spent'     => $spent,
        'remaining' => round($amount - $spent, 2),
        'percent'   => $amount > 0 ? (int)round($spent / $amount * 100) : 0,   // 180 of 300 -> 60
    ];
}

/**
 * Outfits of a user (newest first) with their item ids.
 * $onlyId: fetch a single outfit.  $favoritesOnly: only favorites.
 */
function fetch_outfits(int $userId, bool $favoritesOnly = false, ?int $onlyId = null): array
{
    $sql    = 'SELECT ' . OUTFIT_COLUMNS . ' FROM outfits WHERE user_id = ?';
    $params = [$userId];
    if ($favoritesOnly) {
        $sql .= ' AND is_favorite = 1';
    }
    if ($onlyId !== null) {
        $sql     .= ' AND id = ?';
        $params[] = $onlyId;
    }
    $stmt = db()->prepare($sql . ' ORDER BY id DESC');
    $stmt->execute($params);
    $rows = $stmt->fetchAll();
    if (!$rows) {
        return [];
    }

    $ids          = array_map(fn($r) => (int)$r['id'], $rows);
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    $links        = db()->prepare(
        "SELECT outfit_id, item_id FROM outfit_items WHERE outfit_id IN ($placeholders) ORDER BY item_id"
    );
    $links->execute($ids);
    $itemIds = [];
    foreach ($links->fetchAll() as $l) {
        $itemIds[(int)$l['outfit_id']][] = (int)$l['item_id'];
    }

    $result = [];
    foreach ($rows as $r) {
        $result[] = [
            'id'          => (int)$r['id'],
            'client_uuid' => $r['client_uuid'],
            'name'        => $r['name'],
            'occasion'    => $r['occasion'],
            'explanation' => $r['explanation'],
            'source'      => $r['source'],
            'is_favorite' => (bool)$r['is_favorite'],
            'item_ids'    => $itemIds[(int)$r['id']] ?? [],
            'updated_at'  => $r['updated_at'],
        ];
    }
    return $result;
}

function find_outfit(int $userId, int $id): ?array
{
    $list = fetch_outfits($userId, false, $id);
    return $list[0] ?? null;
}

function find_outfit_by_uuid(int $userId, string $uuid): ?array
{
    $stmt = db()->prepare('SELECT id FROM outfits WHERE user_id = ? AND client_uuid = ?');
    $stmt->execute([$userId, $uuid]);
    $id = $stmt->fetchColumn();
    return $id === false ? null : find_outfit($userId, (int)$id);
}

/** The weekly plan as [{day_of_week, outfit_id}, ...] ordered Monday to Sunday. */
function fetch_plan(int $userId): array
{
    $stmt = db()->prepare('SELECT day_of_week, outfit_id FROM weekly_plans WHERE user_id = ? ORDER BY day_of_week');
    $stmt->execute([$userId]);
    return array_map(
        fn($r) => ['day_of_week' => (int)$r['day_of_week'], 'outfit_id' => (int)$r['outfit_id']],
        $stmt->fetchAll()
    );
}

function brand_json(array $r): array
{
    return [
        'id'          => (int)$r['id'],
        'name'        => $r['name'],
        'category'    => $r['category'],
        'price_range' => $r['price_range'],
        'website_url' => $r['website_url'],
    ];
}

function fetch_brands(): array
{
    $rows = db()->query('SELECT id, name, category, price_range, website_url FROM brands ORDER BY name')->fetchAll();
    return array_map('brand_json', $rows);
}

/** Deletes outfits that have no clothing items left (their weekly-plan entries go with them). */
function delete_empty_outfits(int $userId): void
{
    db()->prepare(
        'DELETE FROM outfits WHERE user_id = ? AND id NOT IN (SELECT outfit_id FROM outfit_items)'
    )->execute([$userId]);
}
