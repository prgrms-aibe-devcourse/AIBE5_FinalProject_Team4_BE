-- 사진 등록 AI 초안에 season 저장
ALTER TABLE clothing_ai_photos ADD COLUMN draft_season VARCHAR(20) NULL AFTER draft_gender;
