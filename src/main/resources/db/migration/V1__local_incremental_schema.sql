-- =====================================================================
-- V1: 로컬 개발 중 누적된 스키마/데이터 마이그레이션 통합본
--
-- Flyway 기본값: FLYWAY_ENABLED=false (application.yml)
-- 이미 JPA ddl-auto 또는 수동 실행으로 반영된 로컬 DB에서는 Flyway 활성화 시
-- baseline-on-migrate: true 로 baseline만 기록하거나, 본 파일을 실행하지 마세요.
--
-- 실행 순서: 백업 → 위에서 아래로 1회
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) clothes 기본 컬럼 + recommendation_feedbacks
--     (출처: migration.sql)
-- ---------------------------------------------------------------------

ALTER TABLE `clothes`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

ALTER TABLE `clothes`
    ADD COLUMN `info_source` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE_HISTORY';

CREATE TABLE IF NOT EXISTS `recommendation_feedbacks` (
    `recommendation_feedback_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `clothes_id` BIGINT NOT NULL,
    `disliked` TINYINT(1) NOT NULL DEFAULT 0,
    `disliked_at` DATETIME NULL,
    `excluded` TINYINT(1) NOT NULL DEFAULT 0,
    `excluded_at` DATETIME NULL,
    `saved` TINYINT(1) NOT NULL DEFAULT 0,
    `saved_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`recommendation_feedback_id`),
    CONSTRAINT `uk_feedback_user_clothes` UNIQUE (`user_id`, `clothes_id`),
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_feedback_clothes` FOREIGN KEY (`clothes_id`) REFERENCES `clothes` (`clothes_id`)
);

-- ---------------------------------------------------------------------
-- 2) 사용자 도메인 (users, social_accounts, user_styles, …)
--     (출처: migration-user-domain.sql)
-- ---------------------------------------------------------------------

UPDATE users
SET profile_image_url = ''
WHERE profile_image_url IS NULL;

ALTER TABLE users
    MODIFY COLUMN profile_image_url VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE users
    ADD COLUMN region_name VARCHAR(50) NOT NULL DEFAULT '' AFTER gender;

ALTER TABLE users
    ADD COLUMN region_code VARCHAR(50) NOT NULL DEFAULT '' AFTER region_name;

ALTER TABLE users
    ADD COLUMN marketing_agreed TINYINT(1) NOT NULL DEFAULT 0 AFTER region_code;

ALTER TABLE users
    ADD COLUMN marketing_agreed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' AFTER marketing_agreed;

ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER marketing_agreed_at;

UPDATE users
SET status = 'WITHDRAWN'
WHERE withdrawn = 1;

UPDATE users
SET withdrawn_at = '1970-01-01 00:00:00'
WHERE withdrawn_at IS NULL;

UPDATE users
SET withdrawn_at = '1970-01-01 00:00:00'
WHERE withdrawn = 0;

UPDATE users
SET birth_date = '2000-01-01'
WHERE birth_date IS NULL
   OR TRIM(CAST(birth_date AS CHAR)) = '';

UPDATE users
SET gender = 'OTHER'
WHERE gender IS NULL
   OR TRIM(gender) = '';

ALTER TABLE users
    MODIFY COLUMN birth_date DATE NOT NULL;

ALTER TABLE users
    MODIFY COLUMN gender VARCHAR(20) NOT NULL;

ALTER TABLE users
    MODIFY COLUMN withdrawn_at DATETIME NOT NULL;

ALTER TABLE users
    DROP COLUMN withdrawn;

ALTER TABLE users
    ADD COLUMN profile_bio VARCHAR(255) NOT NULL DEFAULT '' AFTER profile_image_url;

ALTER TABLE users
    ADD COLUMN external_link_url VARCHAR(255) NOT NULL DEFAULT '' AFTER profile_bio;

RENAME TABLE user_accounts TO social_accounts;

ALTER TABLE social_accounts
    CHANGE COLUMN user_account_id social_account_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE social_accounts
    CHANGE COLUMN provider_id provider_user_id VARCHAR(255) NOT NULL;

ALTER TABLE social_accounts
    ADD COLUMN provider_email VARCHAR(255) NOT NULL DEFAULT '' AFTER provider_user_id;

ALTER TABLE social_accounts
    ADD COLUMN last_login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

UPDATE social_accounts sa
INNER JOIN users u ON u.user_id = sa.user_id
SET sa.provider_email = u.email
WHERE sa.provider_email = '';

ALTER TABLE social_accounts
    DROP COLUMN updated_at;

CREATE TABLE IF NOT EXISTS user_external_links (
    user_external_link_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    link_type             VARCHAR(30)  NOT NULL,
    title                 VARCHAR(50)  NOT NULL,
    url                   VARCHAR(500) NOT NULL,
    sort_order            TINYINT      NOT NULL,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NOT NULL,
    PRIMARY KEY (user_external_link_id),
    CONSTRAINT fk_user_external_links_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

ALTER TABLE user_styles
    ADD COLUMN style_id BIGINT NULL AFTER user_id;

UPDATE user_styles us
INNER JOIN styles s ON s.code = us.style_code
SET us.style_id = s.style_id;

ALTER TABLE user_styles
    ADD COLUMN feedback_weight INT NOT NULL DEFAULT 0 AFTER wardrobe_weight;

UPDATE user_styles
SET combined_weight = preference_weight + wardrobe_weight + feedback_weight;

ALTER TABLE user_styles
    DROP INDEX uk_user_styles_user_style;

ALTER TABLE user_styles
    DROP COLUMN style_code;

ALTER TABLE user_styles
    MODIFY COLUMN style_id BIGINT NOT NULL;

ALTER TABLE user_styles
    ADD CONSTRAINT uk_user_styles_user_style UNIQUE (user_id, style_id);

ALTER TABLE user_styles
    ADD CONSTRAINT fk_user_styles_style
        FOREIGN KEY (style_id) REFERENCES styles (style_id);

ALTER TABLE outfit_books
    DROP COLUMN created_at;

ALTER TABLE outfit_books
    DROP COLUMN updated_at;

-- ---------------------------------------------------------------------
-- 3) clothes_info_source 정리
--     (출처: migration-clothes-schema.sql)
-- ---------------------------------------------------------------------

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = 'EXTERNAL_SHOPPING'
WHERE wc.registration_source IN ('MANUAL', 'NAVER_API')
  AND c.source_type = 'WISHLIST';

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = 'PURCHASE_HISTORY'
WHERE wc.registration_source = 'MANUAL'
  AND c.source_type = 'OWNED';

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = 'EXTERNAL_SHOPPING'
WHERE c.source_type = 'WISHLIST'
  AND wc.registration_source NOT IN ('PHOTO', 'PURCHASE_HISTORY', 'EXTERNAL_SHOPPING');

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = 'PURCHASE_HISTORY'
WHERE c.source_type = 'OWNED'
  AND wc.registration_source NOT IN ('PHOTO', 'PURCHASE_HISTORY', 'EXTERNAL_SHOPPING');

UPDATE `clothes`
SET `info_source` = 'EXTERNAL_SHOPPING'
WHERE `source_type` = 'WISHLIST'
  AND `external_source` <> 'NONE';

UPDATE `clothes` c
INNER JOIN `wardrobe_clothes` wc ON wc.clothes_id = c.clothes_id
SET c.info_source = 'PHOTO'
WHERE wc.registration_source = 'PHOTO';

UPDATE `clothes`
SET `info_source` = 'EXTERNAL_SHOPPING'
WHERE `source_type` = 'WISHLIST'
  AND `info_source` = 'PURCHASE_HISTORY';

UPDATE `clothes`
SET `info_source` = 'PURCHASE_HISTORY'
WHERE `source_type` = 'OWNED'
  AND `info_source` NOT IN ('PHOTO', 'EXTERNAL_SHOPPING');

ALTER TABLE `clothes`
    CHANGE COLUMN `info_source` `clothes_info_source` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE_HISTORY';

ALTER TABLE `clothes`
    DROP COLUMN `source_type`;

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = c.clothes_info_source;

-- ---------------------------------------------------------------------
-- 4) gender / draft_gender
--     (출처: migration-clothes-gender.sql)
-- ---------------------------------------------------------------------

ALTER TABLE `clothes`
    ADD COLUMN `gender` VARCHAR(20) NOT NULL DEFAULT 'UNISEX' AFTER `item_type`;

UPDATE `clothes`
SET `gender` = 'UNISEX'
WHERE `clothes_id` > 0
  AND (`gender` IS NULL OR TRIM(`gender`) = '');

ALTER TABLE `clothing_ai_photos`
    ADD COLUMN `draft_gender` VARCHAR(20) NULL DEFAULT NULL AFTER `draft_item_type`;

-- ---------------------------------------------------------------------
-- 5) season + item_type 백필
--     (출처: migration-clothes-season.sql, migration-clothes-season-backfill.sql)
-- ---------------------------------------------------------------------

ALTER TABLE `clothes`
    ADD COLUMN `season` VARCHAR(20) NOT NULL DEFAULT 'ALL_SEASON' AFTER `gender`;

UPDATE `clothes`
SET `season` = 'ALL_SEASON'
WHERE `clothes_id` > 0
  AND (`season` IS NULL OR TRIM(`season`) = '');

UPDATE `clothes`
SET `season` = 'SUMMER'
WHERE `category` = 'TOP'
  AND `item_type` IN ('SHORT_SLEEVE', 'SLEEVELESS')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'WINTER'
WHERE `category` = 'TOP'
  AND `item_type` = 'KNIT'
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'FALL'
WHERE `category` = 'TOP'
  AND `item_type` IN ('LONG_SLEEVE', 'SWEAT', 'HOODIE')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'SPRING'
WHERE `category` = 'TOP'
  AND `item_type` IN ('SHIRT', 'COLLAR_TEE')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'SUMMER'
WHERE `category` = 'BOTTOM'
  AND `item_type` IN ('SHORTS', 'SKIRT')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'FALL'
WHERE `category` = 'BOTTOM'
  AND `item_type` IN ('SLACKS', 'CARGO')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'SPRING'
WHERE `category` = 'BOTTOM'
  AND `item_type` = 'COTTON'
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'ALL_SEASON'
WHERE `category` = 'BOTTOM'
  AND `item_type` IN ('DENIM', 'TRAINING')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'WINTER'
WHERE `category` = 'OUTER'
  AND `item_type` IN (
      'PADDING', 'LIGHT_PADDING', 'SINGLE_COAT', 'DOUBLE_COAT',
      'BALMACAAN_COAT', 'TTEOKBOKKI_COAT', 'SHEARLING', 'FLEECE_JACKET'
  )
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'FALL'
WHERE `category` = 'OUTER'
  AND `item_type` IN (
      'WINDBREAKER', 'HOOD_ZIPUP', 'TRAINING_JACKET', 'BLOUSON', 'MA1',
      'VARSITY_JACKET', 'COACH_JACKET', 'DENIM_JACKET', 'WORK_JACKET', 'LEATHER_JACKET'
  )
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

UPDATE `clothes`
SET `season` = 'SPRING'
WHERE `category` = 'OUTER'
  AND `item_type` IN ('BLAZER', 'VEST')
  AND (`season` IS NULL OR `season` = 'ALL_SEASON');

-- ---------------------------------------------------------------------
-- 6) AI draft season, wardrobe_clothes 정리, soft delete
--     (출처: migration-clothing-ai-photo-draft-season.sql,
--            migration-wardrobe-clothes-drop-season.sql,
--            migration-wardrobe-clothes-soft-delete.sql,
--            migration-wardrobe-outfits-soft-delete.sql)
-- ---------------------------------------------------------------------

ALTER TABLE clothing_ai_photos
    ADD COLUMN draft_season VARCHAR(20) NULL AFTER draft_gender;

ALTER TABLE wardrobe_clothes DROP COLUMN season;

ALTER TABLE `wardrobe_clothes`
    ADD COLUMN `deleted_at` DATETIME(6) NULL DEFAULT NULL;

ALTER TABLE `outfits`
    ADD COLUMN `deleted_at` DATETIME(6) NULL DEFAULT NULL;
