<?php
declare(strict_types=1);
/** Input validation shared by several endpoints. */

// Must match the style_options list in Android (res/values/strings.xml), in lowercase.
const ALLOWED_STYLES = ['classic', 'casual', 'sporty', 'elegant', 'streetwear'];

function valid_email(string $email): bool
{
    return strlen($email) <= 150 && filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
}

/** bcrypt only uses the first 72 bytes, so we cap the length there. */
function valid_password(string $password): bool
{
    $len = strlen($password);
    return $len >= 8 && $len <= 72;
}

function valid_name(string $name): bool
{
    $len = text_length($name);
    return $len >= 2 && $len <= 100;
}

/** '' or null -> null (no preference). Unknown value -> HTTP 422. */
function normalize_style($value): ?string
{
    $style = strtolower(clean_string($value ?? '', 100));
    if ($style === '') {
        return null;
    }
    if (!in_array($style, ALLOWED_STYLES, true)) {
        json_error('Unknown style. Allowed: ' . implode(', ', ALLOWED_STYLES) . '.', 422);
    }
    return $style;
}

// ---------------------------------------------------------------------
// Wardrobe / outfits / budget validation (stage 3)
// These lists must match the Android app (ClothingOptions, Occasions) and the MySQL ENUMs.
// ---------------------------------------------------------------------
const CATEGORIES      = ['shirt', 'trousers', 'jacket', 'shoes', 'accessory', 'other'];
const COLORS          = ['Black', 'White', 'Grey', 'Beige', 'Brown', 'Navy',
                         'Blue', 'Green', 'Red', 'Burgundy', 'Pink', 'Yellow'];
const OCCASIONS       = ['university', 'work', 'casual', 'event'];
const OUTFIT_SOURCES  = ['manual', 'rules', 'ai'];
const MAX_AMOUNT      = 999999;

/** "2026-10-31" yes, "2026-02-30" no. */
function valid_date($value): bool
{
    if (!is_string($value) || !preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $value, $m)) {
        return false;
    }
    return checkdate((int)$m[2], (int)$m[3], (int)$m[1]);
}

/** "2026-10" */
function valid_month($value): bool
{
    return is_string($value) && preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $value) === 1;
}

function valid_uuid($value): bool
{
    return is_string($value)
        && preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $value) === 1;
}

/** Positive whole number from JSON (int or digit string), otherwise null. */
function parse_id($value): ?int
{
    if (is_int($value)) {
        return $value > 0 ? $value : null;
    }
    if (is_string($value) && ctype_digit($value) && strlen($value) < 10) {
        $id = (int)$value;
        return $id > 0 ? $id : null;
    }
    return null;
}

/** A number between $min and $max (rounded to 2 decimals), otherwise null. */
function parse_amount($value, float $min = 0, float $max = MAX_AMOUNT): ?float
{
    if (is_bool($value) || !is_numeric($value)) {
        return null;
    }
    $number = (float)$value;
    if (!is_finite($number) || $number < $min || $number > $max) {
        return null;
    }
    return round($number, 2);
}

/** true/false/1/0/"1"/"0" -> bool, anything else -> null. */
function parse_bool($value): ?bool
{
    if ($value === true || $value === 1 || $value === '1') return true;
    if ($value === false || $value === 0 || $value === '0') return false;
    return null;
}

/** Optional text field: null if absent, 422 if it is not text. */
function optional_string(array $body, string $key, int $max): ?string
{
    if (!array_key_exists($key, $body) || $body[$key] === null) {
        return null;
    }
    if (!is_string($body[$key])) {
        json_error("Field '$key' must be text.", 422);
    }
    return clean_string($body[$key], $max);
}

/** Optional client_uuid: null if absent, 422 if malformed. */
function optional_uuid(array $body): ?string
{
    if (!array_key_exists('client_uuid', $body) || $body['client_uuid'] === null) {
        return null;
    }
    if (!valid_uuid($body['client_uuid'])) {
        json_error('client_uuid must be a UUID.', 422);
    }
    return strtolower($body['client_uuid']);
}

/** First and last day of a month: "2026-10" -> ["2026-10-01", "2026-10-31"]. */
function month_range(string $month): array
{
    $first = $month . '-01';
    return [$first, date('Y-m-t', strtotime($first))];
}

/** A query-string value as text (arrays like ?a[]=x are ignored). */
function query_param(string $name, string $default = ''): string
{
    $value = $_GET[$name] ?? $default;
    return is_string($value) ? trim($value) : $default;
}

/**
 * Cleans a website address typed by an admin.
 * Returns '' (no website), a clean http(s) address, or null when invalid.
 * Same rules as UrlUtils.java in the app.
 */
function normalize_website(string $input): ?string
{
    $url = trim($input);
    if ($url === '') {
        return '';
    }
    if (strlen($url) > 255) {
        return null;
    }
    if (!preg_match('#^https?://#i', $url)) {
        $url = 'https://' . $url;
    }
    if (filter_var($url, FILTER_VALIDATE_URL) === false) {
        return null;
    }
    $parts = parse_url($url);
    if ($parts === false || empty($parts['host'])) {
        return null;
    }
    $hostPattern = '/^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+$/';
    if (!preg_match($hostPattern, $parts['host'])) {
        return null;
    }
    return $url;
}
