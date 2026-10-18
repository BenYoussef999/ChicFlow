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
