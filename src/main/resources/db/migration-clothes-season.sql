-- =====================================================================
-- CLOTHES.season (Gemini RECO-004 시즌 분류 저장)
--
-- 실행 순서: 백업 → 본 스크립트 → 앱 배포
--
-- [운영] Error 1060 (Duplicate column 'season')
--   → ADD 구문을 건너뛰고 UPDATE만 실행
-- =====================================================================

ALTER TABLE `clothes`
    ADD COLUMN `season` VARCHAR(20) NOT NULL DEFAULT 'ALL_SEASON' AFTER `gender`;

UPDATE `clothes`
SET `season` = 'ALL_SEASON'
WHERE `clothes_id` > 0
  AND (`season` IS NULL OR TRIM(`season`) = '');

-- item_type 기반 시즌 분류 (TOP/BOTTOM/OUTER) — migration-clothes-season-backfill.sql 실행
