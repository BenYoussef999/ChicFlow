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
