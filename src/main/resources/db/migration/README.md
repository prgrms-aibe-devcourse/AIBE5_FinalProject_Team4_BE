# Flyway migrations

기본값은 `FLYWAY_ENABLED=false` 입니다. 로컬은 JPA `ddl-auto: update`로 스키마를 맞춥니다.

## 통합 migration

| 파일 | 내용 |
| --- | --- |
| `V1__local_incremental_schema.sql` | 기존 `db/migration-*.sql` 분산 스크립트를 실행 순서대로 통합 |

포함된 변경: users/social_accounts, clothes_info_source, gender/season, AI draft season, soft delete 등.

## Flyway 활성화 (RDS 등)

1. **이미 스키마가 반영된 DB**(로컬 dump, 수동 실행 완료):
   - `FLYWAY_ENABLED=true` + `baseline-on-migrate: true`(application.yml 기본)
   - V1은 baseline으로만 기록되고 SQL은 실행되지 않음
2. **빈 DB에서 Flyway로만 구축**하려면:
   - JPA `ddl-auto=validate` + V1 실행 (또는 `--no-data` dump를 baseline으로 사용)
3. **이후 변경**은 `V2__...sql`만 **추가** (기존 파일 수정 금지)

## env

```properties
FLYWAY_ENABLED=false   # 기본
JPA_DDL_AUTO=validate  # prod
```

## 파일 네이밍

```
V{version}__{description}.sql
```

예: `V2__add_index_on_clothes_season.sql`
