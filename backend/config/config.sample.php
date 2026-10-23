<?php
/**
 * COPY this file to config.php and fill in your real values.
 * config.php is listed in .gitignore: NEVER commit it to GitHub.
 */
return [
    'db' => [
        'host'    => 'localhost',
        'name'    => 'chicflow',
        'user'    => 'root',
        'pass'    => '',
        'charset' => 'utf8mb4',
    ],

    // How long a login token stays valid (days)
    'token_ttl_days' => 30,

    // Used in stage 9 (AI). Keep empty for now.
    'ai' => [
        'provider' => 'gemini',
        'api_key'  => '',
        'model'    => 'gemini-2.0-flash',
    ],

    // true  = error details are included in JSON responses (development only!)
    // false = generic message only (use this on the deployed server)
    'debug' => true,
];
