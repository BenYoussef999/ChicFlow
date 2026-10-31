#!/usr/bin/env bash
# Tests of the offline / synchronisation logic (SQLite SQL + sync engine against the REAL PHP API).
#
#   bash tests/sync/run_sync_tests.sh http://localhost/chicflow/api/
#
# Needs: JDK 8 or newer (javac), Python 3, git (to download the small org.json library once),
#        and the backend running (Apache + MySQL) with a freshly imported schema.sql.
# Windows: run it from "Git Bash" (installed with Git). See tests/sync/README.md.
set -e

BASE="${1:-http://localhost/chicflow/api/}"
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../../android/app/src/main/java/com/chicflow/tunisia"
WORK="${TMPDIR:-/tmp}/chicflow_sync_tests"

case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; *) SEP=":" ;; esac
PY=python3; command -v python3 >/dev/null 2>&1 || PY=python

rm -rf "$WORK/out" "$WORK/json"; mkdir -p "$WORK/out" "$WORK/json"

echo "[1/4] org.json library"
JSON_DIR="${JSON_JAVA_DIR:-$WORK/JSON-java}"
[ -d "$JSON_DIR" ] || git clone --quiet --depth 1 https://github.com/stleary/JSON-java.git "$JSON_DIR"
javac --release 8 -nowarn -d "$WORK/json" "$JSON_DIR"/src/main/java/org/json/*.java 2>&1 | grep -v "^Note:\|warning" || true

echo "[2/4] compiling the app's sync code (plain Java part)"
javac --release 8 -nowarn -cp "$WORK/json" -d "$WORK/out" \
  "$SRC/models/ClothingItem.java" \
  "$SRC/sync/SyncStatus.java" "$SRC/sync/LocalStore.java" "$SRC/sync/RemoteApi.java" \
  "$SRC/sync/SyncResult.java" "$SRC/sync/SyncEngine.java" \
  "$SRC/network/ApiException.java" "$SRC/network/Transport.java" \
  "$SRC/network/ItemJson.java" "$SRC/network/RestRemoteApi.java" \
  "$SRC/db/ItemSql.java" "$HERE/DumpSql.java" "$HERE/E2E.java" 2>&1 | grep -v "^Note:\|warning: \[options\]" || true

echo "[3/4] SQL statements, executed in SQLite"
java -cp "$WORK/out" DumpSql > "$WORK/dump.txt"
"$PY" "$HERE/test_sql.py" "$WORK/dump.txt" | grep -v "^  PASS"

echo "[4/4] sync engine against the real server: $BASE"
java -cp "$WORK/out$SEP$WORK/json" E2E "$BASE" | grep -v "^  PASS" | grep -v "^$" | grep -v "^=="
