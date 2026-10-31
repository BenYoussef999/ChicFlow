# Sync tests

These tests check the part of the app that is hardest to get right: working offline and
synchronising SQLite (phone) with MySQL (server).

| Step | What it checks |
|---|---|
| SQL (`test_sql.py`) | the exact SQL strings of `ItemSql.java`, executed in SQLite: schema, unique keys, local edits, the "edited while syncing" rules, search / filter / sort, SQL-injection text |
| Sync engine (`E2E.java`) | the real `SyncEngine` + `RestRemoteApi` + `ItemJson` talking to the **real PHP API and MySQL**: first sync, edit, delete, second device, offline, lost responses (no duplicates), edits during a sync, refused changes, conflicts, expired login, privacy between users |

(The phone's database is replaced by a small in-memory copy that follows the same rules as the SQL.)

## Run
1. Backend running (XAMPP: Apache + MySQL), database freshly imported from `backend/database/schema.sql`.
2. In **Git Bash**, from the project folder:
   ```
   bash tests/sync/run_sync_tests.sh http://localhost/chicflow/api/
   ```
   Needs a JDK (8 or newer) and Python 3 on the PATH, and internet once (it downloads the small
   `org.json` library from GitHub). Use a **throw-away database**: the test creates users and items.
   If the `mysql` command is not on the PATH, one optional check is skipped (it says `SKIP`).

Expected end of the output:
```
ALL 47 SQL CHECKS PASSED
ALL 60 SYNC CHECKS PASSED
```
