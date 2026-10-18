<?php
declare(strict_types=1);
/**
 * Token-based authentication.
 *  - At login the server creates a random token and sends it to the app.
 *  - Only the SHA-256 hash of the token is stored in MySQL.
 *  - The app sends:  Authorization: Bearer <token>  with every request.
 */

function bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? null;

    if ($header === null && function_exists('getallheaders')) {
        foreach (getallheaders() as $name => $value) {
            if (strtolower($name) === 'authorization') {
                $header = $value;
            }
        }
    }
    if ($header !== null && preg_match('/^Bearer\s+(\S+)$/i', $header, $m)) {
        return $m[1];
    }
    return null;
}

/** Create a token for a user. Returns [rawToken, expiresAt]. */
function create_token(int $userId): array
{
    $raw     = bin2hex(random_bytes(32));          // 64 hex chars
    $hash    = hash('sha256', $raw);
    $days    = (int)config()['token_ttl_days'];
    $expires = date('Y-m-d H:i:s', time() + $days * 86400);

    $stmt = db()->prepare(
        'INSERT INTO auth_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)'
    );
    $stmt->execute([$userId, $hash, $expires]);

    return [$raw, $expires];
}

/** Delete the token used by the current request (logout). */
function revoke_current_token(): void
{
    $token = bearer_token();
    if ($token !== null) {
        db()->prepare('DELETE FROM auth_tokens WHERE token_hash = ?')
            ->execute([hash('sha256', $token)]);
    }
}

/**
 * Call at the top of every protected endpoint.
 * Returns the logged-in user (id, name, email, role, style_preference) or stops with 401.
 * The user id ALWAYS comes from here, never from the request body.
 */
function authenticate(): array
{
    $token = bearer_token();
    if ($token === null) {
        json_error('Authentication required.', 401);
    }

    $stmt = db()->prepare(
        'SELECT u.id, u.name, u.email, u.role, u.style_preference
           FROM auth_tokens t
           JOIN users u ON u.id = t.user_id
          WHERE t.token_hash = ? AND t.expires_at > ?'
    );
    $stmt->execute([hash('sha256', $token), date('Y-m-d H:i:s')]);
    $user = $stmt->fetch();

    if (!$user) {
        json_error('Invalid or expired token. Please log in again.', 401);
    }
    return $user;
}

/** Like authenticate(), but only lets admins through. */
function require_admin(): array
{
    $user = authenticate();
    if ($user['role'] !== 'admin') {
        json_error('Administrator access required.', 403);
    }
    return $user;
}
