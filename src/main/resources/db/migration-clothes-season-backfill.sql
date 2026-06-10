-- =====================================================================
-- CLOTHES.season item_type 기반 백필 (TOP / BOTTOM / OUTER)
--
-- 전제: clothes.season 컬럼 존재
-- 대상: season IS NULL 또는 ALL_SEASON 인 행
-- SHOES는 별도 정책 없으면 ALL_SEASON 유지
-- =====================================================================

-- TOP
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

-- BOTTOM
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

-- OUTER
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
