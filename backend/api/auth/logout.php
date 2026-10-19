<?php
/** POST /api/auth/logout.php : invalidates the token used for this request. */
require __DIR__ . '/../../core/bootstrap.php';
require_method('POST');

authenticate();            // 401 if not logged in
revoke_current_token();

json_success(['message' => 'Logged out.']);
