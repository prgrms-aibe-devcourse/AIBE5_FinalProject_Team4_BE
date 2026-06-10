-- outfits 소프트 삭제 (코디 삭제 시 실제 데이터 유지)
-- ddl-auto: update 환경에서는 앱 재시작 시 자동 추가될 수 있습니다.

ALTER TABLE `outfits`
    ADD COLUMN `deleted_at` DATETIME(6) NULL DEFAULT NULL;