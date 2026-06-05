-- =====================================================================
-- ERD v2.1 사용자 도메인 정합 마이그레이션
--
-- 대상:
--   - USERS: withdrawn 제거, status/region/marketing 컬럼 추가
--   - USER_ACCOUNTS → SOCIAL_ACCOUNTS (컬럼명 정합)
--   - USER_STYLES: style_code → style_id FK, feedback_weight 추가
--   - OUTFIT_BOOKS: audit 컬럼 제거
--
-- 실행 전 DB 백업 권장. 이미 ERD 스키마와 일치하면 해당 구문은 스킵하세요.
-- =====================================================================

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NOT NULL DEFAULT '' AFTER nickname;

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

ALTER TABLE users
    MODIFY COLUMN birth_date DATE NOT NULL;

ALTER TABLE users
    MODIFY COLUMN gender VARCHAR(20) NOT NULL;

ALTER TABLE users
    MODIFY COLUMN withdrawn_at DATETIME NOT NULL;

ALTER TABLE users
    DROP COLUMN withdrawn;

-- ---------------------------------------------------------------------------
-- USER_ACCOUNTS → SOCIAL_ACCOUNTS
-- (이미 social_accounts면 RENAME 구문은 스킵)
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- USER_STYLES
-- ---------------------------------------------------------------------------
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
    DROP COLUMN style_code;

ALTER TABLE user_styles
    MODIFY COLUMN style_id BIGINT NOT NULL;

ALTER TABLE user_styles
    ADD CONSTRAINT uk_user_styles_user_style UNIQUE (user_id, style_id);

ALTER TABLE user_styles
    ADD CONSTRAINT fk_user_styles_style
        FOREIGN KEY (style_id) REFERENCES styles (style_id);

-- ---------------------------------------------------------------------------
-- OUTFIT_BOOKS
-- ---------------------------------------------------------------------------
ALTER TABLE outfit_books
    DROP COLUMN created_at;

ALTER TABLE outfit_books
    DROP COLUMN updated_at;
