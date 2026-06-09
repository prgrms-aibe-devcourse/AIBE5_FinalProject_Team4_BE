-- CLOTHES.target_gender / clothing_ai_photos.draft_gender
-- local은 ddl-auto: update 이므로 앱 재시작만으로 컬럼이 이미 추가됐을 수 있습니다.
-- Error 1060 (Duplicate column) → ADD는 건너뛰고 아래 UPDATE만 실행하세요.

-- 확인: SHOW COLUMNS FROM clothes LIKE 'target_gender';
-- 확인: SHOW COLUMNS FROM clothing_ai_photos LIKE 'draft_gender';

-- ALTER TABLE `clothes`
--     ADD COLUMN `target_gender` VARCHAR(20) NOT NULL DEFAULT 'UNISEX' AFTER `item_type`;

-- Error 1175 (safe update mode): WHERE에 PK(clothes_id) 조건 포함
UPDATE `clothes`
SET `target_gender` = 'UNISEX'
WHERE `clothes_id` > 0
  AND (`target_gender` IS NULL OR TRIM(`target_gender`) = '');

-- ALTER TABLE `clothing_ai_photos`
--     ADD COLUMN `draft_gender` VARCHAR(20) NULL AFTER `draft_item_type`;
