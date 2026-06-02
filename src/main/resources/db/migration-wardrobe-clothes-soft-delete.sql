-- wardrobe_clothes 소프트 삭제 (사용자 옷장 제거, clothes 마스터 유지)
-- ddl-auto: update 환경에서는 앱 재시작 시 자동 추가될 수 있습니다.

ALTER TABLE `wardrobe_clothes`
    ADD COLUMN `deleted_at` DATETIME(6) NULL DEFAULT NULL;
