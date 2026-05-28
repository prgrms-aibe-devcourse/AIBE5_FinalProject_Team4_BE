-- =====================================================================
-- 마이그레이션: clothes 테이블 분리 (color, wardrobe 관계 테이블 이전)
-- 실행 전 반드시 백업 후 진행하세요.
-- =====================================================================

-- 1. version 컬럼 추가 (낙관적 락)
ALTER TABLE `clothes`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

-- 2. 기존 clothes.color → clothing_colors 백필
--    (기존 테이블에 color VARCHAR 컬럼이 있었던 경우)
-- INSERT INTO `clothing_colors` (clothes_id, color_code, color_role, sort_order, created_at, updated_at)
-- SELECT id, color, 'PRIMARY', 0, NOW(), NOW()
-- FROM `clothes`
-- WHERE color IS NOT NULL;

-- 3. 기존 clothes.wardrobe_id → wardrobe_clothes 백필
--    (기존 테이블에 wardrobe_id, is_favorite 컬럼이 있었던 경우)
-- INSERT INTO `wardrobe_clothes`
--     (wardrobe_id, clothes_id, ownership_status, size, season, favorite,
--      registration_source, user_image_url, created_at, updated_at)
-- SELECT
--     wardrobe_id,
--     id,
--     source_type,         -- 'OWNED' or 'WISHLIST'
--     'FREE',              -- size 기본값 (실제 값으로 교체)
--     NULL,                -- season
--     COALESCE(is_favorite, 0),
--     'MANUAL',
--     image_url,
--     NOW(),
--     NOW()
-- FROM `clothes`
-- WHERE wardrobe_id IS NOT NULL;

-- 4. 이전 완료 후 기존 컬럼 제거
--    (데이터 검증 후 실행)
-- ALTER TABLE `clothes` DROP COLUMN `color`;
-- ALTER TABLE `clothes` DROP COLUMN `wardrobe_id`;
-- ALTER TABLE `clothes` DROP COLUMN `is_favorite`;
