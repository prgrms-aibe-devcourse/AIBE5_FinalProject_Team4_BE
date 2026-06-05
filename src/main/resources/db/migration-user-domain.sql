-- =====================================================================
-- ERD v2.1 사용자 도메인 정합 마이그레이션
--
-- 대상:
--   - USERS: withdrawn 제거, status/region/marketing/profile_bio/external_link_url 추가
--   - USER_ACCOUNTS → SOCIAL_ACCOUNTS (컬럼명 정합, updated_at 제거)
--   - USER_EXTERNAL_LINKS: 신규 테이블
--   - USER_STYLES: style_code → style_id FK, feedback_weight 추가
--   - OUTFIT_BOOKS: audit 컬럼 제거
--
-- 실행 전 DB 백업 권장. 이미 ERD 스키마와 일치하면 해당 구문은 스킵하세요.
-- =====================================================================

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
-- develop에는 profile_image_url이 이미 있음 → ADD COLUMN 하지 않고 backfill 후 강화
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

-- OAuth 등으로 비어 있던 기존 row backfill (@PrePersist는 신규 insert에만 적용)
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

-- ERD: created_at, last_login_at만 유지 (updated_at 없음)
ALTER TABLE social_accounts
    DROP COLUMN updated_at;

-- ---------------------------------------------------------------------------
-- USER_EXTERNAL_LINKS
-- ---------------------------------------------------------------------------
CREATE TABLE user_external_links (
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

-- ---------------------------------------------------------------------------
-- USER_STYLES: style_code → style_id FK
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

-- backfill 검증: 아래 SELECT 결과가 0이어야 함. 0이 아니면 style_code를 유지한 채 수동 조치 후 재실행.
-- SELECT user_style_id, user_id, style_code FROM user_styles WHERE style_id IS NULL;

-- (user_id, style_code) UK가 style_code drop을 막으므로 먼저 제거
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

-- ---------------------------------------------------------------------------
-- OUTFIT_BOOKS
-- ---------------------------------------------------------------------------
ALTER TABLE outfit_books
    DROP COLUMN created_at;

ALTER TABLE outfit_books
    DROP COLUMN updated_at;
