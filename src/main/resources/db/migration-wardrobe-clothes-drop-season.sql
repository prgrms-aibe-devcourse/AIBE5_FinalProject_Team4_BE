-- WARDROBE_CLOTHES.season 제거: 시즌은 CLOTHES.season(마스터)만 사용합니다.
ALTER TABLE wardrobe_clothes DROP COLUMN season;
