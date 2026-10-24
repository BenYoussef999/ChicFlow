<?php
declare(strict_types=1);
/**
 * JSON helpers. Every API answer has the same shape:
 *   success: { "success": true,  "data": { ... } }
 *   error:   { "success": false, "message": "..." }
 */

function send_json_headers(): void
{
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: Content-Type, Authorization');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
        http_response_code(204);
        exit;
    }
}

function json_success($data = [], int $status = 200): void
{
    http_response_code($status);
    echo json_encode(['success' => true, 'data' => $data], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
    exit;
}

function json_error(string $message, int $status = 400): void
{
    http_response_code($status);
    echo json_encode(['success' => false, 'message' => $message], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
    exit;
}

/** Stop with 405 if the HTTP method is not the expected one. */
function require_method(string $method): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== strtoupper($method)) {
        json_error('Method not allowed. Use ' . strtoupper($method) . '.', 405);
    }
}

/** Read the request body as JSON and return it as an array. */
function get_json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return [];
    }
    $data = json_decode($raw, true);
    if (!is_array($data)) {
        json_error('Invalid JSON body.', 400);
    }
    return $data;
}

/** Stop with 422 if one of the required fields is missing or empty. */
function require_fields(array $body, array $fields): void
{
    $missing = [];
    foreach ($fields as $f) {
        if (!isset($body[$f]) || is_array($body[$f]) || (is_string($body[$f]) && trim($body[$f]) === '')) {
            $missing[] = $f;
        }
    }
    if ($missing) {
        json_error('Missing or invalid field(s): ' . implode(', ', $missing), 422);
    }
}

/** Number of characters (not bytes) in a UTF-8 string. Works even without the mbstring extension. */
function text_length(string $s): int
{
    return function_exists('mb_strlen') ? mb_strlen($s) : (int)preg_match_all('/./us', $s);
}

/** Cut a UTF-8 string to at most $max characters. Works even without the mbstring extension. */
function text_cut(string $s, int $max): string
{
    if (function_exists('mb_substr')) {
        return mb_substr($s, 0, $max);
    }
    return preg_match('/^.{0,' . $max . '}/us', $s, $m) ? $m[0] : '';
}

/** Trim a string and limit its length. */
function clean_string($value, int $maxLength = 255): string
{
    return text_cut(trim((string)$value), $maxLength);
}
