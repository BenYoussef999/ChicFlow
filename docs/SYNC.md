# Offline storage and synchronisation (design)

Status: **wardrobe items** are synchronised. Outfits, purchases/budget and the weekly plan follow
in the next increments, using the same engine.

## Goal
The app must work without a connection, and the data on the phone (SQLite) must end up identical
to the data on the server (MySQL). Nothing the user did offline may be lost.

## Where the data lives
| Layer | Role |
|---|---|
| SQLite (`clothing_items` on the phone) | what the screens read and write. Always available. |
| MySQL (server, through the PHP API) | the shared truth, reachable from every device |
| `WardrobeRepository` | the only door for the screens. Saves locally first, then asks for a sync. |
| `SyncManager` | runs the sync on a background thread; triggers; status for the UI |
| `SyncEngine` | the algorithm (plain Java, tested) |

## Each local row knows what is left to do
`sync_status`: `synced` · `pending_create` · `pending_update` · `pending_delete`
plus `remote_id` (the server's id, empty until the server knows the row), `client_uuid`
(generated on the phone) and `version` (+1 at every local change).

* A deleted item is only **hidden** (`pending_delete`) until the server has been told.
* `client_uuid` makes "create" safe to repeat: if the answer is lost and the app resends,
  the server recognises the uuid and returns the existing item instead of creating a duplicate.
* `version` protects edits made *while* a sync is running: the row only becomes `synced`
  if its version is still the one that was sent; otherwise it stays pending and is sent again.

## One synchronisation
1. **Push**: creates, then updates, then deletes (only rows that are pending).
2. **Pull**: download the server's list and make the phone match it:
   * new on the server -> inserted
   * changed on the server and not pending locally -> overwritten
   * gone from the server -> removed locally
   * pending local changes are **never** overwritten
   * rows deleted on the phone before their server id was known are looked up by `client_uuid` and deleted on the server

## When it runs
After every local change · when the main screen opens (login) · when the network comes back ·
when the user taps the status line on Home. Simultaneous requests are merged into one run.

## Failure handling
| Situation | Behaviour |
|---|---|
| No connection / timeout / server error | the sync stops, everything stays pending, status "Offline - N changes waiting" |
| Answer lost after the server saved an item | retry is idempotent (`client_uuid`): no duplicate |
| Server refuses one change (validation 422) | that change stays pending and is counted; the others still go through |
| Login token expired (401) | sync stops, the app returns to the login screen |
| Item deleted on another device | local row removed (server wins); updating a deleted item -> 404 -> removed |
| Same item edited on two devices | last one to sync wins; both phones end identical |

## Limits (on purpose, to stay simple)
* No automatic merge of two edits of the same item: last write wins.
* The pull downloads the whole list (small data), so no "changes since" and no tombstones are needed.
* Photos are not synchronised yet (bonus feature).

## How it is tested
`tests/sync/` : 47 checks run the exact SQL in SQLite; 60 checks run the real engine against the real
PHP API and MySQL (offline, lost responses, edits during a sync, refused changes, two devices,
expired login, privacy between users). Deliberately breaking the engine makes these tests fail.
