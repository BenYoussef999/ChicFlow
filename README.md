# ChicFlow Tunisia: Le dressing intelligent

Android (Java/XML) + PHP REST API + MySQL (+ SQLite locally, AI via PHP).

```
ChicFlowTunisia/
├── backend/     PHP API + MySQL schema   (goes on the web server)
└── android/     Android Studio project   (open THIS folder in Android Studio)
```

## Stage 1: what is included

| Part | Files |
|---|---|
| MySQL schema (9 tables) | `backend/database/schema.sql` |
| PHP foundation | `backend/core/` (`bootstrap`, `db`, `response`, `auth`) |
| Health-check endpoint | `backend/api/ping.php` |
| Android project, theme, networking | `android/` (`ApiConfig`, `ApiClient`, `SessionManager`, Splash + connection-test screen) |

## Run it locally (XAMPP + Android emulator)

### A. Backend
1. Install XAMPP, start **Apache** and **MySQL** in the XAMPP control panel.
2. Copy the `backend` folder to `C:\xampp\htdocs\` and rename it **`chicflow`**
   (result: `C:\xampp\htdocs\chicflow\api\ping.php`).
3. Open http://localhost/phpmyadmin → **New** → database name `chicflow`,
   collation `utf8mb4_unicode_ci` → Create.
4. Select the `chicflow` database → **Import** → choose `backend/database/schema.sql` → Go.
   You should see 9 tables.
5. In `C:\xampp\htdocs\chicflow\config\`, copy `config.sample.php` to **`config.php`**.
   XAMPP defaults (user `root`, empty password) already match, so no edit is needed.
6. Open http://localhost/chicflow/api/ping.php in the browser. Expected:
   `{"success":true,"data":{... "table_count":9,"schema_ok":true}}`

### B. Android app
1. Android Studio → **Open** → select the `android` folder. Wait for Gradle sync.
2. Run on an emulator (Device Manager → create a Pixel device).
3. `ApiConfig.BASE_URL` is already `http://10.0.2.2/chicflow/api/`
   (`10.0.2.2` is how the emulator reaches your PC's localhost).
4. Tap **Test connection**. You should see "Schema OK".

### Real phone on the same Wi-Fi (optional, local test)
- Find your PC's IP (`ipconfig`, e.g. `192.168.1.20`).
- Set `BASE_URL = "http://192.168.1.20/chicflow/api/"` and add that IP in
  `res/xml/network_security_config.xml`. Allow Apache through the Windows firewall.
- Remove the IP again for the final (https) build.

## Git
```
git init
git add .
git commit -m "Stage 1: backend foundation and Android skeleton"
```
`config.php` is in `.gitignore`: your passwords and API keys never go to GitHub.


## Status

| Part | Content | Backend connected? |
|---|---|---|
| Stage 1 | MySQL schema, PHP core, `ping.php`, Android skeleton | yes |
| Stage 2 | Register / login / logout / profile (token auth, roles) | yes |
| Frontend 1 | Navigation, Home, Wardrobe (search, filter, sort, add/edit/delete) | no (in memory) |
| Frontend 2 | Smart Outfit (rule-based), saved outfits, 60 s challenge, Budget + ProgressBar | no (in memory) |
| Frontend 3 | My Week planner, Profile (connected), Local Style, dashboard | Profile only |
| Backend 3 | Full REST API: wardrobe, outfits, budget, purchases, weekly plan, brands, sync snapshot | API ready, tested (145 checks) |
| Next | Android networking + SQLite + sync, then AI endpoint, deployment | |

"In memory" screens read their data through the `repository/` classes. Those classes are the
only files that change when SQLite + the PHP API are plugged in.

## Demo data (frontend phase)
- 7 demo clothing items and a 300 TND budget with 180 TND spent are re-created every time the app starts.
- The Local Style catalog is empty on purpose: brands, prices and links must be real.
  Make yourself an administrator to add some:
  `UPDATE users SET role='admin' WHERE email='you@example.com';` then log out and in again.

## Backend: install / update (stage 3)
1. **Existing database** (created before stage 3): in phpMyAdmin, select `chicflow` -> SQL tab ->
   paste `backend/database/migrations/002_add_client_uuid.sql` -> Go. (Run it once.)
   **New database:** just import `backend/database/schema.sql` (it already includes the change).
2. Copy the updated `backend` folder over `C:\xampp\htdocs\chicflow` (your `config/config.php` is not in the zip, so it stays).
3. Open http://localhost/chicflow/api/ping.php -> must show `"migrated":true,"schema_ok":true`.
4. Run the automatic test (needs PHP on the PATH; XAMPP has it in `C:\xampp\php`):
   `C:\xampp\php\php.exe backend\tests\api_test.php http://localhost/chicflow/api`
   -> `ALL 145 CHECKS PASSED`.
Full endpoint list: `backend/API.md`.
