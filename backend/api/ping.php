<?php
/**
 * GET /api/ping.php
 * Health check: PHP runs, the database is reachable, the schema is imported AND migrated.
 */
require __DIR__ . '/../core/bootstrap.php';
require_method('GET');

$tables = (int)db()->query(
    'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()'
)->fetchColumn();

$hasUuid = (int)db()->query(
    "SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'clothing_items' AND column_name = 'client_uuid'"
)->fetchColumn() === 1;

json_success([
    'app'         => 'ChicFlow Tunisia API',
    'php_version' => PHP_VERSION,
    'server_time' => date('Y-m-d H:i:s'),
    'database'    => 'connected',
    'table_count' => $tables,
    'migrated'    => $hasUuid,                       // false -> run database/migrations/002_add_client_uuid.sql
    'schema_ok'   => $tables >= 9 && $hasUuid,
]);
