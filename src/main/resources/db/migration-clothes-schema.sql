-- CLOTHES 테이블 스키마 정리
-- - source_type(ownership) 제거 → WARDROBE_CLOTHES.ownership_status만 사용
-- - info_source → clothes_info_source 로 컬럼명 통일
--
-- 실행 전 백업 권장. Hibernate ddl-auto=update 환경에서는 컬럼 추가/삭제가 자동 반영되지 않을 수 있어 수동 실행을 권장합니다.

-- 1) info_source → clothes_info_source (이미 clothes_info_source가 있으면 스킵)
ALTER TABLE `clothes`
    CHANGE COLUMN `info_source` `clothes_info_source` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE_HISTORY';

-- 2) source_type 제거 (데이터는 wardrobe_clothes.ownership_status에만 유지)
ALTER TABLE `clothes`
    DROP COLUMN `source_type`;
