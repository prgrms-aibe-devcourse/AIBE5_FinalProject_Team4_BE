---
doc_type: be_doc_index
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-14
---

# 프로젝트 문서

이 폴더는 옷장난감 BE 레포의 공식 프로젝트 문서입니다. 기획, 기능 범위, 도메인 규칙, 기능 정책, DB 구조, API 계약, 패키지 구조를 같은 기준으로 이해하기 위해 작성합니다.

## 문서 원칙

- 공통 기획/기능/도메인 문서는 BE 레포를 기준으로 관리합니다.
- FE 레포에는 공통 문서의 동일본을 둡니다.
- 기능 요구사항과 세부기능 ID의 공식 기준은 [requirements/requirements-definition.md](./requirements/requirements-definition.md)입니다.
- API 계약, DB, ERD 상세는 BE 문서를 원본으로 봅니다.
- FE 화면 흐름, FE API 사용 방식, mock 정책, 로딩/에러/빈 상태는 FE 문서에서 상세화합니다.
- 코드와 문서가 다르면 PR에서 코드가 문서를 따를지, 문서 기준을 수정할지 명시합니다.
- 현재 코드와 공식 문서 기준의 차이는 [backend/implementation-gaps.md](./backend/implementation-gaps.md)에 기록합니다. 차이가 해소되거나 새로 생기면 해당 문서도 함께 수정합니다.
- 코드 변경으로 문서 기준 자체가 바뀌어야 한다면, 코드만 변경하지 않고 관련 기준 문서를 같은 PR에서 함께 갱신합니다.

## 문서 목록

| 문서 | 성격 | 역할 |
| --- | --- | --- |
| [planning/project-plan.md](./planning/project-plan.md) | 공통 | 서비스 기획, 문제 정의, MVP 범위, 주요 화면 구조 |
| [requirements/requirements-definition.md](./requirements/requirements-definition.md) | 공통 | 요구사항 ID, 세부기능 ID, 기능 요구, 정책/제약 기준 |
| [requirements/feature-index.md](./requirements/feature-index.md) | 공통 | 요구사항 정의서의 세부기능 ID와 API/화면/도메인 연결 기준 |
| [domain/glossary.md](./domain/glossary.md) | 공통 | 프로젝트에서 사용하는 도메인 용어와 enum 기준 |
| [domain/catalog.md](./domain/catalog.md) | 공통 | 카테고리, 타입, 계절, 색상, 스타일, 외부 출처 code 사용 기준 |
| [domain/invariants.md](./domain/invariants.md) | 공통 | 구현 중 유지해야 하는 핵심 도메인 규칙 |
| [features/recommendation-policy.md](./features/recommendation-policy.md) | 공통 | 추천 점수, 피드백, 추천 제외, 동점 처리 기준 |
| [features/garment-registration.md](./features/garment-registration.md) | 공통 + BE 상세 | 옷 등록 흐름과 BE 저장/API 기준 |
| [architecture/system-architecture.md](./architecture/system-architecture.md) | 공통 + BE 상세 | FE, BE, DB, 외부 API, 저장소, 인프라의 전체 연결 구조 |
| [architecture/information-architecture.md](./architecture/information-architecture.md) | 공통 | 화면, 메뉴, 주요 진입 흐름 |
| [architecture/sequence-diagrams.md](./architecture/sequence-diagrams.md) | 공통 | 로그인, 옷 등록, 옷장, 추천, 보유 전환, 룩피드 주요 기능 흐름 |
| [architecture/tech-stack.md](./architecture/tech-stack.md) | 공통 + BE 상세 | FE/BE 기술 스택, 외부 API, 인프라, 테스트 도구 기준 |
| [database/erd.md](./database/erd.md) | BE | 확정 ERD v2.3, 테이블 역할, 관계 기준 |
| [database/data-lifecycle.md](./database/data-lifecycle.md) | BE | 데이터 생성, 삭제, 보존 기준 |
| [api/api-contract.md](./api/api-contract.md) | BE | API 경로, 응답 형식, 오류 처리, 엔드포인트 기준 |
| [api/ai-md-api-spec.md](./api/ai-md-api-spec.md) | BE + FE 연동 | AI MD 선택, 코디·상품 추천, 선택 저장을 위한 상세 API 명세 |
| [api/similar-product-api-spec.md](./api/similar-product-api-spec.md) | BE + FE 연동 | 보유 옷 선택, 유사상품 조회, 외부 구매 및 미보유 상품 저장 연동 명세 |
| [architecture/package-structure.md](./architecture/package-structure.md) | BE | BE 패키지 구조와 책임 경계 |
| [backend/implementation-gaps.md](./backend/implementation-gaps.md) | BE | 현재 BE 코드와 공식 문서 기준의 차이 |

## 문서 확인 순서

처음 문서를 읽거나 BE 기능을 작성, 수정, 삭제, 검토할 때는 아래 순서를 따릅니다.

1. [기획서](./planning/project-plan.md)에서 서비스 목표와 MVP 범위를 확인합니다.
2. [요구사항 정의서](./requirements/requirements-definition.md)에서 요구사항 ID, 세부기능 ID, 기능 요구, 정책/제약을 확인합니다.
3. [기능 인덱스](./requirements/feature-index.md)에서 세부기능 ID와 API, 데이터, FE 화면 연결 기준을 확인합니다.
4. [시스템 아키텍처](./architecture/system-architecture.md), [정보 구조도](./architecture/information-architecture.md), [시퀀스 다이어그램](./architecture/sequence-diagrams.md), [기술 스택](./architecture/tech-stack.md)에서 전체 구성, 화면 흐름, 주요 기능 동작 순서, 사용 기술을 확인합니다.
5. [도메인 용어집](./domain/glossary.md), [카탈로그 사용 가이드](./domain/catalog.md), [도메인 규칙](./domain/invariants.md)에서 데이터 의미, enum, code, 불변 규칙을 확인합니다.
6. [BE 구현 정합성 현황](./backend/implementation-gaps.md)에서 현재 코드와 공식 기준의 차이, 개발/임시 API 경계를 확인합니다.
7. [추천 정책](./features/recommendation-policy.md)과 [옷 등록 플로우](./features/garment-registration.md)에서 주요 기능 정책을 확인합니다.
8. [API 계약](./api/api-contract.md)에서 경로, 요청/응답 DTO, 공통 응답, 인증/인가, 오류 처리 기준을 확인합니다.
9. [ERD](./database/erd.md)와 [데이터 생명주기](./database/data-lifecycle.md)에서 테이블 책임, 관계, 삭제/보존 기준을 확인합니다.
10. [패키지 구조](./architecture/package-structure.md)에서 controller, service, repository, entity, dto 위치와 책임 경계를 확인합니다.
11. 문서 기준과 코드가 다르면 코드가 문서를 따를지, 문서 기준을 수정할지 PR 안에서 명시합니다.
12. 현재 코드와 공식 기준의 차이가 생기거나 해소되면 [BE 구현 정합성 현황](./backend/implementation-gaps.md)을 같은 PR에서 수정합니다.

## 문서와 코드 정합성 기준

이 문서는 BE 구현의 공식 기준입니다. 구현 코드가 이 문서의 규칙, 계약, 용어, API 경로, DB 책임과 맞지 않으면 문서 기준을 충족하지 않는 구현으로 봅니다.

기존 코드가 임시 구현, local API, 개발 편의 endpoint, 현재 구현 gap을 포함할 수 있더라도 공식 기능 기준은 이 문서를 따릅니다. 코드 변경으로 문서 기준 자체가 바뀌어야 한다면, 코드만 변경하지 않고 해당 기준 문서를 함께 갱신합니다.

현재 코드와 공식 문서 기준의 차이는 [backend/implementation-gaps.md](./backend/implementation-gaps.md)에 기록합니다. 차이가 해소되면 해당 문서도 함께 수정합니다.

## 동기화 기준

공통 문서가 변경되면 FE 레포의 동일 문서도 함께 확인합니다. 기획, 기능 ID, 도메인 용어, enum 값은 BE/FE 공통 문서에서 같은 기준을 유지해야 합니다.

## FE 동기화 권장

FE 레포에는 아래 문서의 동일본을 둡니다.

```text
docs/planning/project-plan.md
docs/requirements/requirements-definition.md
docs/requirements/feature-index.md
docs/domain/glossary.md
docs/domain/catalog.md
docs/domain/invariants.md
docs/features/recommendation-policy.md
docs/features/garment-registration.md
docs/architecture/system-architecture.md
docs/architecture/information-architecture.md
docs/architecture/sequence-diagrams.md
docs/architecture/tech-stack.md
```

FE 전용 문서는 화면 흐름, API 사용 방식, mock 정책, 로딩/에러/빈 상태를 중심으로 별도 작성합니다.
