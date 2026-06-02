---
doc_type: be_doc_index
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-02
---

# 프로젝트 문서

이 폴더는 옷장난감 BE 레포의 공식 프로젝트 문서입니다. 기획, 기능 범위, 도메인 규칙, 기능 정책, DB 구조, API 계약, 패키지 구조를 같은 기준으로 이해하기 위해 작성합니다.

## 문서 원칙

- 공통 기획/기능/도메인 문서는 BE 레포를 기준으로 관리합니다.
- FE 레포에는 공통 문서의 동일본 또는 요약 동기화본을 둘 수 있습니다.
- API 계약, DB, ERD 상세는 BE 문서를 원본으로 봅니다.
- FE 화면 흐름, FE API 사용 방식, mock 정책, 로딩/에러/빈 상태는 FE 문서에서 상세화합니다.
- 코드와 문서가 다르면 PR에서 코드가 문서를 따를지, 문서 기준을 수정할지 명시합니다.

## 문서 목록

| 문서 | 성격 | 역할 |
| --- | --- | --- |
| [planning/project-plan.md](./planning/project-plan.md) | 공통 | 서비스 기획, 문제 정의, MVP 범위, 주요 화면 구조 |
| [requirements/feature-index.md](./requirements/feature-index.md) | 공통 | 기능 ID, 기능 범위, API/화면/도메인 연결 기준 |
| [domain/glossary.md](./domain/glossary.md) | 공통 | 프로젝트에서 사용하는 도메인 용어와 enum 기준 |
| [domain/catalog.md](./domain/catalog.md) | 공통 | 카테고리, 타입, 색상, 스타일, 외부 출처 code 사용 기준 |
| [domain/invariants.md](./domain/invariants.md) | 공통 | 구현 중 유지해야 하는 핵심 도메인 규칙 |
| [features/recommendation-policy.md](./features/recommendation-policy.md) | 공통 | 추천 점수, 피드백, 추천 제외, 동점 처리 기준 |
| [features/garment-registration.md](./features/garment-registration.md) | 공통 + BE 상세 | 옷 등록 흐름과 BE 저장/API 기준 |
| [database/erd.md](./database/erd.md) | BE | 확정 ERD v2.1, 테이블 역할, 관계 기준 |
| [database/data-lifecycle.md](./database/data-lifecycle.md) | BE | 데이터 생성, 삭제, 보존 기준 |
| [api/api-contract.md](./api/api-contract.md) | BE | API 경로, 응답 형식, 오류 처리, 엔드포인트 기준 |
| [architecture/package-structure.md](./architecture/package-structure.md) | BE | BE 패키지 구조와 책임 경계 |

## 읽는 순서

1. [기획서](./planning/project-plan.md)에서 서비스 목표와 MVP 범위를 확인합니다.
2. [기능 인덱스](./requirements/feature-index.md)에서 기능 ID와 구현 범위를 확인합니다.
3. [도메인 용어집](./domain/glossary.md), [카탈로그 사용 가이드](./domain/catalog.md), [도메인 규칙](./domain/invariants.md)에서 데이터 의미와 불변 규칙을 확인합니다.
4. [추천 정책](./features/recommendation-policy.md)과 [옷 등록 플로우](./features/garment-registration.md)에서 주요 기능 정책을 확인합니다.
5. [ERD](./database/erd.md)와 [데이터 생명주기](./database/data-lifecycle.md)에서 테이블 구조와 보존 기준을 확인합니다.
6. [API 계약](./api/api-contract.md)에서 FE/BE 연동 기준을 확인합니다.
7. [패키지 구조](./architecture/package-structure.md)에서 코드 위치와 책임 경계를 확인합니다.

## 동기화 기준

공통 문서가 변경되면 같은 변경 사항이 필요한 FE 문서도 함께 확인합니다. FE 문서가 요약본인 경우에도 기획, 기능 ID, 도메인 용어, enum 값은 BE 문서의 기준과 충돌하지 않아야 합니다.

## FE 동기화 권장

FE 레포에는 아래 문서의 동일본 또는 요약 동기화본을 두는 것을 권장합니다.

```text
docs/planning/project-plan.md
docs/requirements/feature-index.md
docs/domain/glossary.md
docs/domain/catalog.md
docs/domain/invariants.md
docs/features/recommendation-policy.md
docs/features/garment-registration.md
```

FE 전용 문서는 화면 흐름, API 사용 방식, mock 정책, 로딩/에러/빈 상태를 중심으로 별도 작성합니다.
