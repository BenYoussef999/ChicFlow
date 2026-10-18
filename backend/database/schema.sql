-- =====================================================================
-- ChicFlow Tunisia : MySQL schema (9 tables)
--
-- LOCAL (XAMPP/phpMyAdmin): create the database first, then select it:
--     CREATE DATABASE chicflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--     USE chicflow;
-- HOSTED: the host usually creates the database for you.
--     Select it in phpMyAdmin, then run/import this file (no CREATE DATABASE).
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  email            VARCHAR(150) NOT NULL UNIQUE,
  password_hash    VARCHAR(255) NOT NULL,
  role             ENUM('user','admin') NOT NULL DEFAULT 'user',
  style_preference VARCHAR(100) NULL,                 -- e.g. classic, sporty, casual
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_tokens (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  token_hash  CHAR(64) NOT NULL UNIQUE,               -- SHA-256 of the token
  expires_at  DATETIME NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clothing_items (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  name        VARCHAR(100) NOT NULL,
  category    ENUM('shirt','trousers','jacket','shoes','accessory','other') NOT NULL,
  color       VARCHAR(30) NOT NULL,
  price       DECIMAL(8,2) NOT NULL DEFAULT 0,
  photo_url   VARCHAR(255) NULL,
  date_added  DATE NOT NULL,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outfits (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  name        VARCHAR(100) NOT NULL,
  occasion    ENUM('university','work','casual','event') NOT NULL,
  explanation TEXT NULL,
  source      ENUM('manual','rules','ai') NOT NULL DEFAULT 'manual',  -- tells real AI apart
  is_favorite TINYINT(1) NOT NULL DEFAULT 0,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outfit_items (
  outfit_id INT NOT NULL,
  item_id   INT NOT NULL,
  PRIMARY KEY (outfit_id, item_id),
  FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE,
  FOREIGN KEY (item_id)   REFERENCES clothing_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS budgets (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  user_id  INT NOT NULL,
  month    CHAR(7) NOT NULL,                          -- '2026-10'
  amount   DECIMAL(8,2) NOT NULL,
  UNIQUE KEY uq_budget_user_month (user_id, month),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchases (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  item_id       INT NULL,                             -- optional link to a wardrobe item
  description   VARCHAR(150) NOT NULL,
  amount        DECIMAL(8,2) NOT NULL,
  purchase_date DATE NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (item_id) REFERENCES clothing_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS weekly_plans (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  day_of_week TINYINT NOT NULL,                       -- 1 = Monday ... 7 = Sunday
  outfit_id   INT NOT NULL,
  UNIQUE KEY uq_plan_user_day (user_id, day_of_week),
  FOREIGN KEY (user_id)   REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional "Local Style" catalog. Managed by an admin; filled with REAL data only.
CREATE TABLE IF NOT EXISTS brands (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  category    VARCHAR(50) NOT NULL,
  price_range VARCHAR(50) NULL,
  website_url VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
