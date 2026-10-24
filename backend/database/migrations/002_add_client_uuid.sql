-- =====================================================================
-- Migration 002: adds client_uuid (makes "create" requests safe to retry).
-- Run ONCE on a database that was created with the stage 1 / stage 2 schema.sql.
-- phpMyAdmin: select the chicflow database -> SQL tab -> paste -> Go.
-- (A database created with the NEW schema.sql already has these columns:
--  running this on it fails with "Duplicate column name", which is harmless.)
-- =====================================================================

ALTER TABLE clothing_items
  ADD COLUMN client_uuid CHAR(36) NULL AFTER user_id,
  ADD UNIQUE KEY uq_item_client (user_id, client_uuid);

ALTER TABLE outfits
  ADD COLUMN client_uuid CHAR(36) NULL AFTER user_id,
  ADD UNIQUE KEY uq_outfit_client (user_id, client_uuid);

ALTER TABLE purchases
  ADD COLUMN client_uuid CHAR(36) NULL AFTER user_id,
  ADD UNIQUE KEY uq_purchase_client (user_id, client_uuid);
