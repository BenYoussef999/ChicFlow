<?php
/**
 * End-to-end test of the ChicFlow API. Run it from a terminal:
 *
 *     php tests/api_test.php http://localhost/chicflow/api
 *
 * (works the same against your deployed server: php tests/api_test.php https://your-host/api)
 * It registers two temporary users (random e-mails), exercises every endpoint, and prints
 * PASS / FAIL for each check. Exit code 0 = everything passed.
 * Needs the PHP "curl" extension (enabled by default in XAMPP).
 */

date_default_timezone_set('Africa/Tunis');   // same as the server, so "today" matches
$base = rtrim($argv[1] ?? 'http://localhost/chicflow/api', '/');
$passed = 0;
$failed = 0;

/** Sends a request, returns [httpStatus, decodedJson]. $raw lets us send broken JSON on purpose. */
function call(string $method, string $path, $body = null, ?string $token = null, ?string $raw = null): array
{
    global $base;
    $ch = curl_init($base . '/' . $path);
    $headers = ['Accept: application/json'];
    if ($token !== null) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    if ($method === 'POST') {
        $headers[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $raw ?? json_encode($body ?? new stdClass()));
    }
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER     => $headers,
        CURLOPT_TIMEOUT        => 20,
    ]);
    $response = curl_exec($ch);
    $status   = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return [$status, json_decode((string)$response, true)];
}

function check(bool $condition, string $label): void
{
    global $passed, $failed;
    if ($condition) {
        $passed++;
        echo "  PASS  $label\n";
    } else {
        $failed++;
        echo "  FAIL  $label\n";
    }
}

function section(string $title): void
{
    echo "\n== $title ==\n";
}

function uuid(): string
{
    $d = random_bytes(16);
    $d[6] = chr((ord($d[6]) & 0x0f) | 0x40);
    $d[8] = chr((ord($d[8]) & 0x3f) | 0x80);
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($d), 4));
}

function register(string $name): array
{
    $email = 'test_' . bin2hex(random_bytes(4)) . '@example.com';
    [$s, $r] = call('POST', 'auth/register.php', ['name' => $name, 'email' => $email, 'password' => 'secret123']);
    if ($s !== 201) {
        echo "Cannot register a test user (HTTP $s). Is the API reachable and the database migrated?\n";
        print_r($r);
        exit(2);
    }
    return [$r['data']['token'], $email];
}

function addItem(string $token, string $name, string $category, string $color, $price, string $date): int
{
    [$s, $r] = call('POST', 'clothes/create.php', [
        'name' => $name, 'category' => $category, 'color' => $color, 'price' => $price, 'date_added' => $date,
    ], $token);
    return (int)($r['data']['item']['id'] ?? 0);
}

// ---------------------------------------------------------------------
section('Server');
[$s, $r] = call('GET', 'ping.php');
check($s === 200 && ($r['data']['schema_ok'] ?? false) === true,
    'ping: schema_ok = true' . (($r['data']['migrated'] ?? true) ? '' : '  (run database/migrations/002_add_client_uuid.sql!)'));

[$tokenA, $emailA] = register('Alice Test');
[$tokenB, $emailB] = register('Bob Test');
check($tokenA !== $tokenB && strlen($tokenA) === 64, 'two users registered, tokens are 64 characters');

// ---------------------------------------------------------------------
section('Authentication and HTTP methods');
foreach (['clothes/list.php', 'outfits/list.php', 'budget/get.php', 'budget/purchases_list.php',
          'plan/list.php', 'brands/list.php', 'sync/pull.php'] as $endpoint) {
    [$s] = call('GET', $endpoint);
    check($s === 401, "GET $endpoint without token -> 401");
}
foreach (['clothes/create.php', 'clothes/update.php', 'clothes/delete.php', 'outfits/create.php',
          'outfits/favorite.php', 'outfits/delete.php', 'budget/set.php', 'budget/purchase_add.php',
          'budget/purchase_delete.php', 'plan/set.php', 'plan/remove.php', 'brands/save.php', 'brands/delete.php'] as $endpoint) {
    [$s] = call('POST', $endpoint, ['x' => 1]);
    check($s === 401, "POST $endpoint without token -> 401");
}
[$s] = call('POST', 'clothes/list.php', [], $tokenA);
check($s === 405, 'POST on a GET endpoint -> 405');
[$s] = call('GET', 'clothes/create.php', null, $tokenA);
check($s === 405, 'GET on a POST endpoint -> 405');
[$s] = call('GET', 'clothes/list.php', null, 'a' . str_repeat('0', 63));
check($s === 401, 'made-up token -> 401');

// ---------------------------------------------------------------------
section('Wardrobe: create / validate');
$idChinos   = addItem($tokenA, 'Navy Chinos', 'trousers', 'Navy', 89.9, '2026-09-10');
$idOxford   = addItem($tokenA, 'White Oxford Shirt', 'shirt', 'White', 65, '2026-09-02');
$idSneakers = addItem($tokenA, 'White Sneakers', 'shoes', 'White', 150, '2026-09-28');
$idJacket   = addItem($tokenA, 'Black Leather Jacket', 'jacket', 'Black', 240, '2026-08-21');
$idHoodie   = addItem($tokenA, 'Grey Hoodie', 'shirt', 'Grey', 59.9, '2026-10-12');
$idBelt     = addItem($tokenA, 'Brown Leather Belt', 'accessory', 'Brown', 35, '2026-07-30');
check(min($idChinos, $idOxford, $idSneakers, $idJacket, $idHoodie, $idBelt) > 0, 'six items created');

[$s, $r] = call('POST', 'clothes/create.php', ['name' => 'Test Tee', 'category' => 'shirt', 'color' => 'Red'], $tokenA);
check($s === 201 && $r['data']['item']['price'] === 0.0 && $r['data']['item']['date_added'] === date('Y-m-d'),
    'price defaults to 0 and date_added to today');
$idTee = $r['data']['item']['id'];

$bad = [
    'unknown category'   => ['name' => 'Hat', 'category' => 'hat', 'color' => 'Red'],
    'unknown color'      => ['name' => 'Hat', 'category' => 'shirt', 'color' => 'Purple'],
    'name too short'     => ['name' => 'x', 'category' => 'shirt', 'color' => 'Red'],
    'name is a list'     => ['name' => ['a'], 'category' => 'shirt', 'color' => 'Red'],
    'negative price'     => ['name' => 'Tee', 'category' => 'shirt', 'color' => 'Red', 'price' => -1],
    'price is text'      => ['name' => 'Tee', 'category' => 'shirt', 'color' => 'Red', 'price' => 'abc'],
    'price too large'    => ['name' => 'Tee', 'category' => 'shirt', 'color' => 'Red', 'price' => 1000000],
    'impossible date'    => ['name' => 'Tee', 'category' => 'shirt', 'color' => 'Red', 'date_added' => '2026-02-30'],
    'bad uuid'           => ['name' => 'Tee', 'category' => 'shirt', 'color' => 'Red', 'client_uuid' => 'not-a-uuid'],
    'missing fields'     => ['color' => 'Red'],
];
foreach ($bad as $label => $payload) {
    [$s] = call('POST', 'clothes/create.php', $payload, $tokenA);
    check($s === 422, "create rejected: $label -> 422");
}
[$s] = call('POST', 'clothes/create.php', null, $tokenA, '{broken json');
check($s === 400, 'create with broken JSON -> 400');

// ---------------------------------------------------------------------
section('Wardrobe: list / search / filter / sort');
[$s, $r] = call('GET', 'clothes/list.php', null, $tokenA);
$names = array_column($r['data']['items'], 'name');
check($s === 200 && count($names) === 7, 'list returns the 7 items');
check($names[0] === 'Test Tee' && $names[1] === 'Grey Hoodie', 'default sort = newest first (today\'s tee, then 2026-10-12)');

function listNames(string $query, string $token): array
{
    [, $r] = call('GET', 'clothes/list.php?' . $query, null, $token);
    return array_column($r['data']['items'] ?? [], 'name');
}
check(listNames('sort=name', $tokenA)[0] === 'Black Leather Jacket', 'sort=name starts with Black Leather Jacket');
check(listNames('sort=price_asc', $tokenA)[0] === 'Test Tee', 'sort=price_asc: cheapest first (the 0 TND tee)');
check(listNames('sort=price_desc', $tokenA)[0] === 'Black Leather Jacket', 'sort=price_desc: Black Leather Jacket (240)');
check(listNames('search=navy', $tokenA) === ['Navy Chinos'], 'search "navy" finds Navy Chinos');
check(count(listNames('search=white', $tokenA)) === 2, 'search "white" matches name or color (2 items)');
check(count(listNames('category=shirt', $tokenA)) === 3, 'category=shirt -> 3 items');
check(count(listNames('color=White', $tokenA)) === 2, 'color=White -> 2 items');
check(listNames('category=shoes&color=White', $tokenA) === ['White Sneakers'], 'category + color combined');
check(listNames('category=shirt&search=hoodie', $tokenA) === ['Grey Hoodie'], 'category + search combined');
foreach (['category=hat', 'color=Purple', 'sort=hack'] as $q) {
    [$s] = call('GET', 'clothes/list.php?' . $q, null, $tokenA);
    check($s === 422, "invalid filter $q -> 422");
}
[$s, $r] = call('GET', "clothes/list.php?search=" . urlencode("' OR '1'='1"), null, $tokenA);
check($s === 200 && $r['data']['items'] === [], "SQL injection text in search: no error, no data leaked");
check(listNames('search=' . urlencode('%'), $tokenA) === [] && listNames('search=_', $tokenA) === [],
    'search wildcards % and _ are treated as plain characters');
[$s] = call('GET', 'clothes/list.php?category[]=shirt', null, $tokenA);
check($s === 200, 'array-style query parameter does not crash the server');

// ---------------------------------------------------------------------
section('Wardrobe: idempotent create (safe retry)');
$u = uuid();
$payload = ['name' => 'Sync Test Cap', 'category' => 'accessory', 'color' => 'Green', 'price' => 20, 'client_uuid' => $u];
[$s1, $r1] = call('POST', 'clothes/create.php', $payload, $tokenA);
[$s2, $r2] = call('POST', 'clothes/create.php', $payload, $tokenA);
check($s1 === 201 && $s2 === 200 && $r1['data']['item']['id'] === $r2['data']['item']['id'],
    'same client_uuid twice -> 201 then 200 with the SAME item');
check($r1['data']['item']['client_uuid'] === $u, 'client_uuid is returned');
check(count(listNames('', $tokenA)) === 8, 'no duplicate was created (8 items)');
[$s3, $r3] = call('POST', 'clothes/create.php', $payload, $tokenB);
check($s3 === 201 && $r3['data']['item']['id'] !== $r1['data']['item']['id'], 'another user can use the same client_uuid independently');
$idCap = $r1['data']['item']['id'];

// ---------------------------------------------------------------------
section('Wardrobe: update / delete');
[$s, $r] = call('POST', 'clothes/update.php', ['id' => $idTee, 'name' => 'Red Tee', 'price' => '25.50'], $tokenA);
check($s === 200 && $r['data']['item']['name'] === 'Red Tee' && $r['data']['item']['price'] === 25.5,
    'update name + price (price sent as text works)');
[$s] = call('POST', 'clothes/update.php', ['id' => $idTee, 'category' => 'hat'], $tokenA);
check($s === 422, 'update with unknown category -> 422');
[$s] = call('POST', 'clothes/update.php', ['id' => $idTee], $tokenA);
check($s === 422, 'update with nothing to change -> 422');
[$s] = call('POST', 'clothes/update.php', ['id' => 999999, 'name' => 'Ghost'], $tokenA);
check($s === 404, 'update of an unknown id -> 404');
[$s] = call('POST', 'clothes/update.php', ['name' => 'No id'], $tokenA);
check($s === 422, 'update without id -> 422');

// ---------------------------------------------------------------------
section('Privacy: user B cannot touch user A\'s data');
[$s, $r] = call('GET', 'clothes/list.php', null, $tokenB);
check($s === 200 && count($r['data']['items']) === 1, 'B sees only his own single item');
[$s] = call('POST', 'clothes/update.php', ['id' => $idChinos, 'name' => 'Hacked'], $tokenB);
check($s === 404, 'B updating A\'s item -> 404');
[$s] = call('POST', 'clothes/delete.php', ['id' => $idChinos], $tokenB);
check($s === 404, 'B deleting A\'s item -> 404');
check(in_array('Navy Chinos', listNames('', $tokenA), true), 'A\'s item is untouched');

// ---------------------------------------------------------------------
section('Outfits');
[$s, $r] = call('POST', 'outfits/create.php', [
    'name' => 'Work look', 'occasion' => 'work', 'source' => 'rules', 'is_favorite' => true,
    'explanation' => 'For a day at work: ...', 'item_ids' => [$idOxford, $idChinos, $idSneakers],
], $tokenA);
check($s === 201 && $r['data']['outfit']['is_favorite'] === true && count($r['data']['outfit']['item_ids']) === 3
    && $r['data']['outfit']['source'] === 'rules', 'outfit created with 3 items, favorite, source = rules');
$outWork = $r['data']['outfit']['id'];

[$s] = call('POST', 'outfits/create.php', ['name' => 'Stolen', 'occasion' => 'work', 'item_ids' => [$idChinos]], $tokenB);
check($s === 422, 'B cannot build an outfit from A\'s clothes -> 422');
$badOutfits = [
    'unknown occasion'  => ['name' => 'Look', 'occasion' => 'beach', 'item_ids' => [$idOxford]],
    'unknown source'    => ['name' => 'Look', 'occasion' => 'work', 'source' => 'magic', 'item_ids' => [$idOxford]],
    'no items'          => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => []],
    'items not a list'  => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => 5],
    'item id zero'      => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => [0]],
    'item id text'      => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => ['abc']],
    'unknown item'      => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => [999999]],
    'too many items'    => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => range(1, 21)],
    'bad favorite flag' => ['name' => 'Look', 'occasion' => 'work', 'item_ids' => [$idOxford], 'is_favorite' => 'maybe'],
    'name too short'    => ['name' => 'L', 'occasion' => 'work', 'item_ids' => [$idOxford]],
];
foreach ($badOutfits as $label => $payload) {
    [$s] = call('POST', 'outfits/create.php', $payload, $tokenA);
    check($s === 422, "outfit rejected: $label -> 422");
}
[$s, $r] = call('POST', 'outfits/create.php', ['name' => 'Dup ids', 'occasion' => 'casual', 'item_ids' => [$idOxford, $idOxford]], $tokenA);
check($s === 201 && $r['data']['outfit']['item_ids'] === [$idOxford] && $r['data']['outfit']['is_favorite'] === false,
    'duplicate item ids are merged, favorite defaults to false');
$outDup = $r['data']['outfit']['id'];

$u = uuid();
$payload = ['name' => 'Uuid look', 'occasion' => 'event', 'item_ids' => [$idJacket], 'client_uuid' => $u];
[$s1, $r1] = call('POST', 'outfits/create.php', $payload, $tokenA);
[$s2, $r2] = call('POST', 'outfits/create.php', $payload, $tokenA);
check($s1 === 201 && $s2 === 200 && $r1['data']['outfit']['id'] === $r2['data']['outfit']['id'], 'outfit client_uuid: retry returns the same outfit');
$outUuid = $r1['data']['outfit']['id'];

[$s, $r] = call('GET', 'outfits/list.php', null, $tokenA);
check($s === 200 && count($r['data']['outfits']) === 3 && $r['data']['outfits'][0]['id'] === $outUuid, 'list: 3 outfits, newest first');
[$s, $r] = call('GET', 'outfits/list.php?favorites=1', null, $tokenA);
check(count($r['data']['outfits']) === 1 && $r['data']['outfits'][0]['id'] === $outWork, 'favorites=1 -> only the favorite');
[$s, $r] = call('POST', 'outfits/favorite.php', ['id' => $outDup, 'is_favorite' => true], $tokenA);
check($s === 200 && $r['data']['outfit']['is_favorite'] === true, 'mark favorite');
[$s, $r] = call('POST', 'outfits/favorite.php', ['id' => $outWork, 'is_favorite' => false], $tokenA);
[, $r] = call('GET', 'outfits/list.php?favorites=1', null, $tokenA);
check(count($r['data']['outfits']) === 1 && $r['data']['outfits'][0]['id'] === $outDup, 'un-favorite removes it from favorites only');
[$s] = call('POST', 'outfits/favorite.php', ['id' => $outWork, 'is_favorite' => 'x'], $tokenA);
check($s === 422, 'favorite with a bad flag -> 422');
[$s] = call('POST', 'outfits/favorite.php', ['id' => $outWork, 'is_favorite' => true], $tokenB);
check($s === 404, 'B cannot favorite A\'s outfit -> 404');
[$s] = call('POST', 'outfits/delete.php', ['id' => $outWork], $tokenB);
check($s === 404, 'B cannot delete A\'s outfit -> 404');
[, $r] = call('GET', 'outfits/list.php', null, $tokenB);
check($r['data']['outfits'] === [], 'B sees no outfits');

// deleting clothes
[$s, $r] = call('POST', 'outfits/create.php', ['name' => 'Casual look', 'occasion' => 'casual', 'item_ids' => [$idHoodie, $idChinos]], $tokenA);
$outCasual = $r['data']['outfit']['id'];
[$s, $r] = call('POST', 'outfits/create.php', ['name' => 'Belt only', 'occasion' => 'casual', 'item_ids' => [$idBelt]], $tokenA);
$outBelt = $r['data']['outfit']['id'];
[$s, $r] = call('POST', 'plan/set.php', ['day_of_week' => 2, 'outfit_id' => $outBelt], $tokenA);
check($s === 200, 'plan: outfit "Belt only" planned for Tuesday');
[$s] = call('POST', 'clothes/delete.php', ['id' => $idHoodie], $tokenA);
[, $r] = call('GET', 'outfits/list.php', null, $tokenA);
$casual = array_values(array_filter($r['data']['outfits'], fn($o) => $o['id'] === $outCasual));
check($s === 200 && count($casual) === 1 && $casual[0]['item_ids'] === [$idChinos], 'deleting an item removes it from its outfits');
call('POST', 'clothes/delete.php', ['id' => $idBelt], $tokenA);
[, $r] = call('GET', 'outfits/list.php', null, $tokenA);
check(!in_array($outBelt, array_column($r['data']['outfits'], 'id'), true), 'an outfit left without clothes is deleted');
[, $r] = call('GET', 'plan/list.php', null, $tokenA);
check($r['data']['plan'] === [], '... and so is its weekly-plan entry');
[$s] = call('POST', 'clothes/delete.php', ['id' => $idHoodie], $tokenA);
check($s === 404, 'deleting an already deleted item -> 404');
[$s] = call('POST', 'outfits/delete.php', ['id' => $outCasual], $tokenA);
[$s2] = call('POST', 'outfits/delete.php', ['id' => $outCasual], $tokenA);
check($s === 200 && $s2 === 404, 'delete outfit: 200, then 404');

// ---------------------------------------------------------------------
section('Budget: your example (300 TND budget, 180 spent = 60%)');
[$s, $r] = call('GET', 'budget/get.php?month=2026-10', null, $tokenA);
check($s === 200 && $r['data']['amount'] === null && $r['data']['percent'] === null && $r['data']['spent'] === 0.0,
    'no budget yet: amount/percent null, spent 0');
[$s, $r] = call('POST', 'budget/set.php', ['month' => '2026-10', 'amount' => 300], $tokenA);
check($s === 200 && $r['data']['amount'] === 300.0 && $r['data']['percent'] === 0 && $r['data']['remaining'] === 300.0, 'set budget 300');
call('POST', 'budget/purchase_add.php', ['description' => 'Grey Hoodie', 'amount' => 60, 'purchase_date' => '2026-10-03'], $tokenA);
call('POST', 'budget/purchase_add.php', ['description' => 'Burgundy Knit Sweater', 'amount' => 80, 'purchase_date' => '2026-10-08'], $tokenA);
[$s, $r] = call('POST', 'budget/purchase_add.php', ['description' => 'Brown Leather Belt', 'amount' => '40.00', 'purchase_date' => '2026-10-12'], $tokenA);
check($s === 201 && $r['data']['budget']['spent'] === 180.0 && $r['data']['budget']['percent'] === 60
    && $r['data']['budget']['remaining'] === 120.0, '180 spent -> 60% used, 120 TND remaining');
$purchaseBelt = $r['data']['purchase']['id'];
[$s, $r] = call('GET', 'budget/get.php?month=2026-10', null, $tokenA);
check($r['data']['percent'] === 60 && $r['data']['remaining'] === 120.0, 'get returns the same numbers');

[$s, $r] = call('POST', 'budget/purchase_add.php', ['description' => 'Last month shoes', 'amount' => 50, 'purchase_date' => '2026-09-30'], $tokenA);
check($s === 201 && $r['data']['budget']['month'] === '2026-09' && $r['data']['budget']['spent'] === 50.0, 'a purchase counts in ITS month (2026-09)');
[, $r] = call('GET', 'budget/get.php?month=2026-10', null, $tokenA);
check($r['data']['spent'] === 180.0, '... and not in October');

[$s, $r] = call('POST', 'budget/purchase_add.php', ['description' => 'Winter coat', 'amount' => 200, 'purchase_date' => '2026-10-20'], $tokenA);
check($r['data']['budget']['percent'] === 127 && $r['data']['budget']['remaining'] === -80.0, 'over budget: 127%, remaining -80');
$purchaseCoat = $r['data']['purchase']['id'];

[, $r] = call('GET', 'budget/purchases_list.php?month=2026-10', null, $tokenA);
check(count($r['data']['purchases']) === 4 && $r['data']['purchases'][0]['description'] === 'Winter coat', 'October: 4 purchases, newest first');
[, $r] = call('GET', 'budget/purchases_list.php', null, $tokenA);
check(count($r['data']['purchases']) === 5, 'without month: all 5 purchases');

[$s, $r] = call('POST', 'budget/set.php', ['month' => '2026-10', 'amount' => 500], $tokenA);
check($s === 200 && $r['data']['amount'] === 500.0 && $r['data']['percent'] === 76, 'setting again replaces the budget (380/500 = 76%)');

$u = uuid();
$payload = ['description' => 'Retry me', 'amount' => 10, 'purchase_date' => '2026-11-02', 'client_uuid' => $u];
[$s1, $r1] = call('POST', 'budget/purchase_add.php', $payload, $tokenA);
[$s2, $r2] = call('POST', 'budget/purchase_add.php', $payload, $tokenA);
check($s1 === 201 && $s2 === 200 && $r1['data']['purchase']['id'] === $r2['data']['purchase']['id']
    && $r2['data']['budget']['spent'] === 10.0, 'purchase client_uuid: retry does not double the spending');

$badBudget = [
    ['budget/get.php?month=2026-13', null], ['budget/get.php?month=october', null],
    ['budget/purchases_list.php?month=2026-1', null],
];
foreach ($badBudget as [$path]) {
    [$s] = call('GET', $path, null, $tokenA);
    check($s === 422, "invalid month $path -> 422");
}
$badPosts = [
    'budget zero'          => ['budget/set.php', ['month' => '2026-10', 'amount' => 0]],
    'budget negative'      => ['budget/set.php', ['month' => '2026-10', 'amount' => -5]],
    'budget text'          => ['budget/set.php', ['month' => '2026-10', 'amount' => 'abc']],
    'budget bad month'     => ['budget/set.php', ['month' => '2026-00', 'amount' => 100]],
    'budget missing'       => ['budget/set.php', ['month' => '2026-10']],
    'purchase bad date'    => ['budget/purchase_add.php', ['description' => 'Tee', 'amount' => 10, 'purchase_date' => '2026-10-32']],
    'purchase short text'  => ['budget/purchase_add.php', ['description' => 'a', 'amount' => 10, 'purchase_date' => '2026-10-01']],
    'purchase zero amount' => ['budget/purchase_add.php', ['description' => 'Tee', 'amount' => 0, 'purchase_date' => '2026-10-01']],
    'purchase no amount'   => ['budget/purchase_add.php', ['description' => 'Tee', 'purchase_date' => '2026-10-01']],
];
foreach ($badPosts as $label => [$path, $payload]) {
    [$s] = call('POST', $path, $payload, $tokenA);
    check($s === 422, "rejected: $label -> 422");
}

[$s] = call('POST', 'budget/purchase_delete.php', ['id' => $purchaseCoat], $tokenB);
check($s === 404, 'B cannot delete A\'s purchase -> 404');
[$s, $r] = call('POST', 'budget/purchase_delete.php', ['id' => $purchaseCoat], $tokenA);
check($s === 200 && $r['data']['budget']['spent'] === 180.0 && $r['data']['budget']['percent'] === 36, 'delete purchase: spent back to 180 (36% of 500)');
[$s] = call('POST', 'budget/purchase_delete.php', ['id' => $purchaseCoat], $tokenA);
check($s === 404, 'deleting it again -> 404');
[, $r] = call('GET', 'budget/get.php?month=2026-10', null, $tokenB);
check($r['data']['amount'] === null && $r['data']['spent'] === 0.0, 'B has no budget and no spending of his own');
[$s, $r] = call('GET', 'budget/get.php', null, $tokenA);
check($s === 200 && $r['data']['month'] === date('Y-m'), 'month defaults to the current month');

// ---------------------------------------------------------------------
section('Weekly plan');
[, $r] = call('POST', 'outfits/create.php', ['name' => 'Plan A', 'occasion' => 'work', 'item_ids' => [$idOxford, $idChinos]], $tokenA);
$planA = $r['data']['outfit']['id'];
[, $r] = call('POST', 'outfits/create.php', ['name' => 'Plan B', 'occasion' => 'casual', 'item_ids' => [$idSneakers]], $tokenA);
$planB = $r['data']['outfit']['id'];

[$s, $r] = call('POST', 'plan/set.php', ['day_of_week' => 1, 'outfit_id' => $planA], $tokenA);
check($s === 200 && $r['data']['plan'] === [['day_of_week' => 1, 'outfit_id' => $planA]], 'Monday = Plan A');
[$s, $r] = call('POST', 'plan/set.php', ['day_of_week' => 1, 'outfit_id' => $planB], $tokenA);
check($r['data']['plan'] === [['day_of_week' => 1, 'outfit_id' => $planB]], 'setting Monday again REPLACES the outfit');
call('POST', 'plan/set.php', ['day_of_week' => 7, 'outfit_id' => $planA], $tokenA);
[, $r] = call('GET', 'plan/list.php', null, $tokenA);
check(array_column($r['data']['plan'], 'day_of_week') === [1, 7], 'list is ordered Monday to Sunday');
foreach ([0, 8, 'x', null] as $day) {
    [$s] = call('POST', 'plan/set.php', ['day_of_week' => $day, 'outfit_id' => $planA], $tokenA);
    check($s === 422, 'invalid day ' . json_encode($day) . ' -> 422');
}
[$s] = call('POST', 'plan/set.php', ['day_of_week' => 3, 'outfit_id' => 999999], $tokenA);
check($s === 404, 'unknown outfit -> 404');
[$s] = call('POST', 'plan/set.php', ['day_of_week' => 3, 'outfit_id' => $planA], $tokenB);
check($s === 404, 'B cannot plan A\'s outfit -> 404');
[, $r] = call('GET', 'plan/list.php', null, $tokenB);
check($r['data']['plan'] === [], 'B has an empty plan');
[$s, $r] = call('POST', 'plan/remove.php', ['day_of_week' => 1], $tokenA);
check($s === 200 && array_column($r['data']['plan'], 'day_of_week') === [7], 'remove Monday');
[$s] = call('POST', 'plan/remove.php', ['day_of_week' => 4], $tokenA);
check($s === 200, 'removing an empty day is not an error');
call('POST', 'outfits/delete.php', ['id' => $planA], $tokenA);
[, $r] = call('GET', 'plan/list.php', null, $tokenA);
check($r['data']['plan'] === [], 'deleting an outfit clears it from the plan');

// ---------------------------------------------------------------------
section('Brands and roles');
[$s, $r] = call('GET', 'brands/list.php', null, $tokenA);
check($s === 200 && $r['data']['brands'] === [], 'normal user can read the (empty) catalog');
[$s] = call('POST', 'brands/save.php', ['name' => 'Some Brand', 'category' => 'clothing'], $tokenA);
check($s === 403, 'normal user cannot add a brand -> 403');
[$s] = call('POST', 'brands/delete.php', ['id' => 1], $tokenA);
check($s === 403, 'normal user cannot delete a brand -> 403');

// ---------------------------------------------------------------------
section('Sync snapshot');
[, $items]   = call('GET', 'clothes/list.php', null, $tokenA);
[, $outfits] = call('GET', 'outfits/list.php', null, $tokenA);
[, $purch]   = call('GET', 'budget/purchases_list.php', null, $tokenA);
[$s, $r]     = call('GET', 'sync/pull.php', null, $tokenA);
$d = $r['data'] ?? [];
check($s === 200 && isset($d['server_time'], $d['user'], $d['items'], $d['outfits'], $d['budgets'], $d['purchases'], $d['plan'], $d['brands']),
    'pull returns every section');
check(($d['user']['email'] ?? '') === strtolower($emailA) && !isset($d['user']['password_hash']), 'pull contains the user (never the password hash)');
check(count($d['items']) === count($items['data']['items']) && count($d['outfits']) === count($outfits['data']['outfits'])
    && count($d['purchases']) === count($purch['data']['purchases']), 'pull counts match the list endpoints');
check(array_column($d['budgets'], 'amount', 'month') === ['2026-10' => 500.0], 'pull contains the budgets');
[, $rb] = call('GET', 'sync/pull.php', null, $tokenB);
check(count($rb['data']['items']) === 1 && $rb['data']['outfits'] === [] && $rb['data']['purchases'] === [], 'B\'s snapshot contains only B\'s data');

// ---------------------------------------------------------------------
echo "\n";
echo $failed === 0
    ? "ALL $passed CHECKS PASSED\n"
    : "$failed FAILED, $passed passed\n";
exit($failed === 0 ? 0 : 1);
