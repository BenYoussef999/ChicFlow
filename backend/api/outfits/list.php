<?php
/**
 * GET /api/outfits/list.php[?favorites=1]
 * Answer: { "outfits": [ {id, client_uuid, name, occasion, explanation, source, is_favorite, item_ids[], updated_at} ] }
 */
require __DIR__ . '/../../core/bootstrap.php';
require_method('GET');
$user = authenticate();

json_success(['outfits' => fetch_outfits((int)$user['id'], query_param('favorites') === '1')]);
