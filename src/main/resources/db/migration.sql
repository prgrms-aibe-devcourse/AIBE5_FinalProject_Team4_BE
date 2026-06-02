-- =====================================================================
-- 마이그레이션: clothes 테이블 분리 (color, wardrobe 관계 테이블 이전)
-- 실행 전 반드시 백업 후 진행하세요.
-- =====================================================================

-- 1. version 컬럼 추가 (낙관적 락)
--    ddl-auto: update 환경에서는 앱 재시작 시 자동 추가됨
ALTER TABLE `clothes`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

-- 1-1. 옷 정보 출처 enum 컬럼 (PHOTO, PURCHASE_HISTORY, EXTERNAL_SHOPPING)
--      레거시 MANUAL / NAVER_API enum 값은 제거됨 → db/migration-info-source-enum.sql 참고
ALTER TABLE `clothes`
    ADD COLUMN `info_source` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE_HISTORY';

-- 1-2. info_source / registration_source 레거시 백필
--      운영 배포 시 앱 기동 전에 db/migration-info-source-enum.sql 을 반드시 실행하세요.
--      (주석 처리된 UPDATE는 참고용이며, 실행 가능한 스크립트는 별도 파일에 있습니다)
--
--      MANUAL → WISHLIST: EXTERNAL_SHOPPING / OWNED: PURCHASE_HISTORY
--      NAVER_API → EXTERNAL_SHOPPING (WISHLIST)
--      PHOTO → PHOTO (유지)

-- =====================================================================
-- 아래 구문은 기존 데이터가 있을 경우에만 실행하세요.
-- 로컬 개발 환경은 DB를 초기화 후 앱을 재시작하면 됩니다.
-- =====================================================================

-- 2. 기존 clothes.color → clothing_colors 백필
--    (기존 테이블에 color VARCHAR 컬럼이 있었던 경우)
-- INSERT INTO `clothing_colors` (clothes_id, color_code, color_role, sort_order, created_at, updated_at)
-- SELECT clothes_id, color, 'PRIMARY', 0, NOW(), NOW()
-- FROM `clothes`
-- WHERE color IS NOT NULL;

-- 3. 기존 clothes.wardrobe_id → wardrobe_clothes 백필
--    (기존 테이블에 wardrobe_id, is_favorite 컬럼이 있었던 경우)
-- INSERT INTO `wardrobe_clothes`
--     (wardrobe_id, clothes_id, ownership_status, size, season, favorite,
--      registration_source, user_image_url, created_at, updated_at)
-- SELECT
--     wardrobe_id,
--     clothes_id,
--     source_type,         -- 'OWNED' or 'WISHLIST'
--     'FREE',              -- size 기본값 (실제 값으로 교체)
--     NULL,                -- season
--     COALESCE(is_favorite, 0),
--     'PURCHASE_HISTORY',
--     image_url,
--     NOW(),
--     NOW()
-- FROM `clothes`
-- WHERE wardrobe_id IS NOT NULL;

-- 4. clothing_styles에 style_role, sort_order 컬럼 추가된 경우 백필
--    (기존 clothing_styles row에 해당 컬럼이 NULL인 경우)
-- ALTER TABLE `clothing_styles`
--     ADD COLUMN IF NOT EXISTS `style_role` VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
--     ADD COLUMN IF NOT EXISTS `sort_order` TINYINT NOT NULL DEFAULT 0;
--
-- -- 첫 번째 스타일은 PRIMARY, 나머지는 SECONDARY로 설정
-- UPDATE `clothing_styles` cs
-- JOIN (
--     SELECT clothes_id, style_id,
--            ROW_NUMBER() OVER (PARTITION BY clothes_id ORDER BY clothing_style_id) AS rn
--     FROM `clothing_styles`
-- ) ranked ON cs.clothes_id = ranked.clothes_id AND cs.style_id = ranked.style_id
-- SET cs.style_role = IF(ranked.rn = 1, 'PRIMARY', 'SECONDARY'),
--     cs.sort_order = ranked.rn - 1;

-- 5. 이전 완료 후 기존 컬럼 제거 (데이터 검증 후 실행)
-- ALTER TABLE `clothes` DROP COLUMN `color`;
-- ALTER TABLE `clothes` DROP COLUMN `wardrobe_id`;
-- ALTER TABLE `clothes` DROP COLUMN `is_favorite`;
