-- =====================================================================
-- CLOTHES.gender / clothing_ai_photos.draft_gender
--
-- 실행 순서: 백업 → 본 스크립트 → 앱 배포
--
-- [운영] Error 1060 (Duplicate column 'gender')
--   → clothes ADD 구문을 건너뛰고 UPDATE·clothing_ai_photos ADD만 실행
-- [운영] Error 1060 (Duplicate column 'draft_gender')
--   → clothing_ai_photos ADD 구문만 건너뛰기
-- [운영] clothes에 target_gender만 있고 gender가 없음 (ddl-auto 중간 상태)
--   → 아래 rename을 먼저 실행한 뒤 UPDATE 실행:
--   ALTER TABLE `clothes`
--       CHANGE COLUMN `target_gender` `gender` VARCHAR(20) NOT NULL DEFAULT 'UNISEX';
-- =====================================================================

ALTER TABLE `clothes`
    ADD COLUMN `gender` VARCHAR(20) NOT NULL DEFAULT 'UNISEX' AFTER `item_type`;

UPDATE `clothes`
SET `gender` = 'UNISEX'
WHERE `clothes_id` > 0
  AND (`gender` IS NULL OR TRIM(`gender`) = '');

ALTER TABLE `clothing_ai_photos`
    ADD COLUMN `draft_gender` VARCHAR(20) NULL DEFAULT NULL AFTER `draft_item_type`;
