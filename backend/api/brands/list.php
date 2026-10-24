<?php
/** GET /api/brands/list.php   Any logged-in user.   Answer: { "brands": [ {id, name, category, price_range, website_url} ] } */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
authenticate();

json_success(['brands' => fetch_brands()]);
