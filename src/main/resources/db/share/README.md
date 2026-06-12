# clothes 후보 풀 DB 공유

RECO-004 외부 쇼핑 후보(`clothes` + 색상/스타일 태그)를 팀원 로컬 DB에 넣을 때 사용합니다.

## 포함 테이블

| 테이블 | 설명 |
|--------|------|
| `clothes` | 공통 옷 마스터 (gender, season 포함) |
| `clothing_colors` | 옷별 색상 태그 |
| `clothing_styles` | 옷별 스타일 태그 |

`styles` 카탈로그 테이블은 포함하지 않습니다. 받는 쪽 DB에 `styles` 시드가 있어야 합니다.

## 사전 준비 (받는 사람)

1. DB 백업
2. 스키마가 JPA `ddl-auto: update`와 다르면 **데이터 import 전** 아래 중 하나로 맞추기 (이미 반영됐으면 스킵)

```
# Flyway 비활성(기본) 로컬: 앱 재시작으로 ddl-auto 맞추거나
# 수동 실행: src/main/resources/db/migration/V1__local_incremental_schema.sql
```

3. (선택) 시즌 백필 — import 파일에 season이 포함되어 있으면 생략 가능 (V1에 item_type 백필 포함)

## 덤프 파일

| 파일 | 내용 | 용도 |
|------|------|------|
| `build/db-share/clothes-pool-data-only.sql` | **데이터만** (약 8MB) | migration으로 스키마 맞춘 뒤 INSERT |
| `build/db-share/clothes-pool-full.sql` | 구조 + 데이터 | 테이블부터 새로 만들 때 |

공통: **`--complete-insert`** — 컬럼 이름 명시 INSERT (순서 달라도 안전)

재생성 (프로젝트 루트):

```powershell
# 데이터만 (권장)
.\src\main\resources\db\share\export-clothes-pool.ps1 -DataOnly

# 구조 + 데이터
.\src\main\resources\db\share\export-clothes-pool.ps1
```

수동 mysqldump (데이터만):

```bash
mysqldump -h 127.0.0.1 -P 3307 -u root -p \
  --complete-insert --no-create-info --single-transaction \
  closetnangamdb clothes clothing_colors clothing_styles > clothes-pool-data-only.sql
```

## import 방법

### 데이터만 (`clothes-pool-data-only.sql`) — 권장

1. migration으로 **스키마 먼저** 맞추기 (위 사전 준비)
2. 기존 데이터 비우기 (FK 순서)

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE clothing_styles;
TRUNCATE TABLE clothing_colors;
TRUNCATE TABLE clothes;
SET FOREIGN_KEY_CHECKS = 1;
```

3. import

```powershell
Get-Content .\build\db-share\clothes-pool-data-only.sql | mysql -h 127.0.0.1 -P 3307 -u root -p closetnangamdb
```

### 구조 + 데이터 (`clothes-pool-full.sql`)

```powershell
# DROP TABLE 포함 — 테이블 전체 교체
Get-Content .\build\db-share\clothes-pool-full.sql | mysql -h 127.0.0.1 -P 3307 -u root -p closetnangamdb
```

## 주의

- **데이터만** import 시 테이블 구조는 받는 쪽 migration과 같아야 합니다.
- **full** 덤프는 `DROP TABLE IF EXISTS` 포함 → 해당 3개 테이블 **전체 교체**
- `wardrobe_clothes` 등 사용자 옷장 데이터와 FK로 연결된 행이 있으면 import 전후 정합성을 확인하세요.
- positional INSERT(`INSERT INTO t VALUES (...)`) 덤프는 사용하지 마세요. 컬럼 순서가 다르면 데이터가 밀립니다.

## 검증 쿼리

```sql
SELECT clothes_info_source, season, COUNT(*) FROM clothes GROUP BY clothes_info_source, season;
SELECT COUNT(*) FROM clothing_colors;
SELECT COUNT(*) FROM clothing_styles;
```
