<?php
/**
 * GET /api/ping.php
 * Health check: confirms PHP runs, the database is reachable and the schema is imported.
 * Used by the Android "Test connection" button in stage 1.
 */
require __DIR__ . '/../core/bootstrap.php';
require_method('GET');

$tables = (int)db()->query(
    'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()'
)->fetchColumn();

json_success([
    'app'         => 'ChicFlow Tunisia API',
    'php_version' => PHP_VERSION,
    'server_time' => date('Y-m-d H:i:s'),
    'database'    => 'connected',
    'table_count' => $tables,
    'schema_ok'   => $tables >= 9,   // we expect 9 tables
]);
