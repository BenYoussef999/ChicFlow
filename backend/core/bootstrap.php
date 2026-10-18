<?php
declare(strict_types=1);
/**
 * Every endpoint starts with:   require __DIR__ . '/../../core/bootstrap.php';
 * It loads the helpers, sets JSON headers and installs a safe error handler.
 */

date_default_timezone_set('Africa/Tunis');
ini_set('display_errors', '0');          // never print PHP errors into the JSON
error_reporting(E_ALL);

require_once __DIR__ . '/response.php';

function config(): array
{
    static $cfg = null;
    if ($cfg === null) {
        $file = __DIR__ . '/../config/config.php';
        if (!is_file($file)) {
            json_error('Server not configured: backend/config/config.php is missing.', 500);
        }
        $cfg = require $file;
    }
    return $cfg;
}

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/auth.php';

send_json_headers();

set_exception_handler(function (Throwable $e) {
    error_log('[ChicFlow] ' . $e->getMessage() . ' in ' . $e->getFile() . ':' . $e->getLine());
    $debug = (config()['debug'] ?? false) === true;
    json_error($debug ? 'Server error: ' . $e->getMessage() : 'Server error.', 500);
});
