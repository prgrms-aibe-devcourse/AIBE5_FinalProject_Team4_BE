-- =====================================================================
-- 운영 배포: ClothesInfoSource enum 전환 (MANUAL/NAVER_API 제거)
--
-- 새 enum: PHOTO, PURCHASE_HISTORY, EXTERNAL_SHOPPING
-- 레거시 MANUAL 매핑 정책:
--   - WISHLIST + MANUAL (및 NAVER_API) → EXTERNAL_SHOPPING
--   - OWNED   + MANUAL              → PURCHASE_HISTORY
--   - PHOTO                         → PHOTO (유지)
--
-- 실행 순서: 백업 → (info_source 컬럼 DDL) → 본 스크립트 → 앱 배포
--
-- [중요] migration-clothes-schema.sql 에 백필+rename+drop 이 통합되었습니다.
--        운영 DB는 migration-clothes-schema.sql 만 실행하세요.
--        본 파일을 rename/drop 이후에 실행하면 source_type·info_source 부재로 실패합니다.
-- =====================================================================

-- 1) wardrobe_clothes.registration_source 레거시 값 보정 (앱 기동 전 필수)
--    Hibernate enum 로딩 실패(No enum constant ... MANUAL) 방지

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

-- 2) clothes.info_source 백필 (컬럼 추가 DEFAULT 'PURCHASE_HISTORY' 이후)

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

-- 3) wardrobe_clothes.registration_source ← clothes.info_source 최종 동기화

UPDATE `wardrobe_clothes` wc
INNER JOIN `clothes` c ON c.clothes_id = wc.clothes_id
SET wc.registration_source = c.info_source;
