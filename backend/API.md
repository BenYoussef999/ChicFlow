# ChicFlow API reference

Base URL (XAMPP): `http://localhost/chicflow/api/`  ·  From the Android emulator: `http://10.0.2.2/chicflow/api/`

* Every answer is JSON: `{"success":true,"data":{...}}` or `{"success":false,"message":"..."}`.
* Everything except `ping`, `register` and `login` needs the header `Authorization: Bearer <token>`.
* Writes use **POST with a JSON body** (some hosts block PUT/DELETE). Reads use GET.
* The user is always taken from the token, never from the request: nobody can read or change another user's rows
  (another user's id gives **404**, as if it did not exist).
* Money fields (`price`, `amount`, `spent`, ...) are always decimals. Dates are `YYYY-MM-DD`, months `YYYY-MM`.
* HTTP codes: 200 OK · 201 created · 400 broken JSON · 401 not logged in / bad token · 403 admin only ·
  404 not found (or not yours) · 405 wrong method · 409 e-mail already used · 422 validation error · 500 server error.

## Safe retries (`client_uuid`)
`clothes/create`, `outfits/create` and `budget/purchase_add` accept an optional `client_uuid` (a UUID the app
generates). If the same user sends the same `client_uuid` again, the server returns the **existing** row with
HTTP 200 instead of creating a duplicate. This lets the app re-send a pending change after a lost connection.
(Tested with 20 simultaneous identical requests: exactly one row is created.)

## Health
| Endpoint | Method | Answer |
|---|---|---|
| `ping.php` | GET | `php_version, server_time, database, table_count, migrated, schema_ok` |

## Auth
| Endpoint | Method | Body | Answer |
|---|---|---|---|
| `auth/register.php` | POST | `name, email, password, style_preference?` | 201 `token, expires_at, user` |
| `auth/login.php` | POST | `email, password` | `token, expires_at, user` |
| `auth/logout.php` | POST | – | `message` |
| `auth/profile.php` | GET | – | `user` |
| `auth/profile.php` | POST | `name?, style_preference?` | `user` |

`user` = `{id, name, email, role, style_preference}`. Style values: classic, casual, sporty, elegant, streetwear or "".
Roles can never be set through the API (see "Making an admin" below).

## Wardrobe
Item = `{id, client_uuid, name, category, color, price, photo_url, date_added, updated_at}`

| Endpoint | Method | Body / query | Answer |
|---|---|---|---|
| `clothes/list.php` | GET | `search, category, color, sort` | `items[]` |
| `clothes/create.php` | POST | `name, category, color, price?, date_added?, client_uuid?` | 201 `item` (200 if client_uuid existed) |
| `clothes/update.php` | POST | `id` + any of `name, category, color, price, date_added` | `item` |
| `clothes/delete.php` | POST | `id` | `deleted` |

* `category`: shirt, trousers, jacket, shoes, accessory, other
* `color`: Black, White, Grey, Beige, Brown, Navy, Blue, Green, Red, Burgundy, Pink, Yellow
* `sort`: `newest` (default), `name`, `price_asc`, `price_desc`
* `search` looks in the name and the color. `price`: 0 to 999999. `name`: 2 to 100 characters.
* Deleting an item also deletes any outfit that is left without clothes (and its weekly-plan entry).
* `photo_url` is `null` for now: photo upload comes later (it is not accepted from the client).

## Outfits
Outfit = `{id, client_uuid, name, occasion, explanation, source, is_favorite, item_ids[], updated_at}`

| Endpoint | Method | Body / query | Answer |
|---|---|---|---|
| `outfits/list.php` | GET | `favorites=1`? | `outfits[]` (newest first) |
| `outfits/create.php` | POST | `name, occasion, item_ids[1..20], explanation?, source?, is_favorite?, client_uuid?` | 201 `outfit` |
| `outfits/favorite.php` | POST | `id, is_favorite` | `outfit` |
| `outfits/delete.php` | POST | `id` | `deleted` |

* `occasion`: university, work, casual, event · `source`: manual (default), rules, ai
* Every id in `item_ids` must be one of the user's own items, otherwise 422.

## Budget
Summary = `{month, amount, spent, remaining, percent}`. `amount`, `remaining` and `percent` are `null` when no budget
is set. `spent`, `remaining` and `percent` are **calculated by the server** from the purchases
(`percent` = round(spent / amount * 100): 180 of 300 -> 60).

| Endpoint | Method | Body / query | Answer |
|---|---|---|---|
| `budget/get.php` | GET | `month?` (default: current month) | summary |
| `budget/set.php` | POST | `month, amount` (> 0) | summary (creates or replaces) |
| `budget/purchases_list.php` | GET | `month?` (default: all) | `purchases[]` (newest first) |
| `budget/purchase_add.php` | POST | `description, amount (> 0), purchase_date, client_uuid?` | 201 `purchase, budget` |
| `budget/purchase_delete.php` | POST | `id` | `deleted, budget` |

Purchase = `{id, client_uuid, description, amount, purchase_date}`. A purchase counts in the month of its date.

## Weekly plan
`day_of_week`: 1 = Monday ... 7 = Sunday. One outfit per day (setting a day again replaces it).

| Endpoint | Method | Body | Answer |
|---|---|---|---|
| `plan/list.php` | GET | – | `plan[]` = `{day_of_week, outfit_id}` |
| `plan/set.php` | POST | `day_of_week, outfit_id` | `plan[]` |
| `plan/remove.php` | POST | `day_of_week` | `plan[]` |

## Local Style (brands)
Brand = `{id, name, category, price_range, website_url}`

| Endpoint | Method | Who | Body | Answer |
|---|---|---|---|---|
| `brands/list.php` | GET | any logged-in user | – | `brands[]` (A to Z) |
| `brands/save.php` | POST | **admin** | `id?, name, category, price_range?, website_url?` | 201 / 200 `brand` |
| `brands/delete.php` | POST | **admin** | `id` | `deleted` |

`website_url` is cleaned (`example.tn` -> `https://example.tn`); `javascript:`, `ftp:`, `localhost`, malformed or
over-255-character addresses are rejected with 422. The catalog starts empty: only real brands should be added.

## Sync
`sync/pull.php` (GET) returns everything the user owns in one call:
`{server_time, user, items[], outfits[], budgets[{month, amount}], purchases[], plan[], brands[]}`.
The app first pushes its pending local changes, then replaces its local copy with this snapshot.
Strategy: last write wins; deletions are handled because the snapshot is complete (no tombstones needed).

## Making an admin
Roles cannot be set through the API. After registering, run in phpMyAdmin (SQL tab):
`UPDATE users SET role='admin' WHERE email='you@example.com';` then log out and in again.

## Testing the whole API
```
php tests/api_test.php http://localhost/chicflow/api
```
Registers two temporary users, runs ~145 checks (validation, privacy between users, the budget example,
safe retries...) and prints `ALL ... CHECKS PASSED`. Works against a deployed server too.
