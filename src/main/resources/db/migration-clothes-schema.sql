-- =====================================================================
-- CLOTHES 스키마 정리 + clothes_info_source 백필 (통합)
--
-- 목표:
--   - 레거시 info_source / source_type 기준으로 enum 값 백필
--   - info_source → clothes_info_source 컬럼 rename
--   - source_type(ownership) 제거 → WARDROBE_CLOTHES.ownership_status만 사용
--
-- 선행 조건 (미충족 시 아래 1~2단계 UPDATE가 0건이거나 ALTER가 실패할 수 있음):
--   - clothes.info_source 컬럼 존재 (없으면 migration.sql 1단계 먼저)
--   - clothes.source_type 컬럼 존재 (이미 제거된 신규 DB는 3~4단계만 적용)
--
-- 실행 순서: DB 백업 → 본 파일을 위에서 아래로 1회 실행 → 앱 배포
--
-- 주의: migration-info-source-enum.sql 단독 실행은 본 파일에 통합되었습니다.
--       rename/drop 이후에는 기존 백필 스크립트를 다시 실행할 수 없습니다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) wardrobe_clothes.registration_source 레거시 값 보정
--    (MANUAL / NAVER_API → 신규 enum, Hibernate 로딩 실패 방지)
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

-- ---------------------------------------------------------------------
-- 2) clothes.info_source 백필 (rename 전, source_type 활용)
-- ---------------------------------------------------------------------

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

-- ---------------------------------------------------------------------
-- 3) rename: info_source → clothes_info_source
--    (이미 clothes_info_source만 있는 DB면 본 단계 스킵)
-- ---------------------------------------------------------------------

ALTER TABLE `clothes`
    CHANGE COLUMN `info_source` `clothes_info_source` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE_HISTORY';

-- ---------------------------------------------------------------------
-- 4) source_type 제거 (이미 없으면 스킵)
-- ---------------------------------------------------------------------

ALTER TABLE `clothes`
    DROP COLUMN `source_type`;

-- ---------------------------------------------------------------------
-- 5) wardrobe_clothes.registration_source 최종 동기화
-- ---------------------------------------------------------------------

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = c.clothes_info_source;
