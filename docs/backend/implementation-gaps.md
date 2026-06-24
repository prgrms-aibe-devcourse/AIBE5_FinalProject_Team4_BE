---
doc_type: be_implementation_gaps
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-24
---

# BE 구현 정합성 현황

이 문서는 현재 BE 코드와 `docs/` 공식 기준 사이의 차이를 정리합니다. 차이는 곧바로 오류라는 뜻이 아니라, 실제 구현 또는 문서 기준 확정 단계에서 맞춰야 할 기준을 명확히 하기 위한 기록입니다.

기능 요구사항과 세부기능 ID의 원본은 [requirements-definition.md](../requirements/requirements-definition.md)입니다. [feature-index.md](../requirements/feature-index.md)는 요구사항 정의서의 세부기능 ID를 API, 데이터, 화면과 연결하는 빠른 참조 문서입니다.

코드가 이 문서의 목표 기준과 다르게 변경되거나, 목표 기준 자체가 바뀌면 관련 기준 문서를 같은 PR에서 수정합니다.

## 기록 기준

이 문서는 아래 경우만 기록합니다.

- 기준 문서와 현재 BE 코드 구현이 서로 다르게 읽히는 경우
- 현재 BE API 경로, 응답 DTO, DB 조회 범위, 도메인 계산 기준이 기준 문서의 기능 설명보다 좁거나 다른 경우
- FE, BE 또는 코드리뷰 담당자가 문서를 보고 현재 BE 코드를 잘못 이해할 가능성이 있는 경우
- 기준 문서가 확정 기준인지, 현재 구현 상태인지 구분이 필요한 경우
- 코드가 변경되면서 기존 gap이 해소되거나 새 gap이 생긴 경우

아직 구현되지 않은 MVP 예정 기능은 이 문서에 gap으로 기록하지 않습니다. 개발 중인 기능은 기준 문서에 명확히 정의되어 있으면 됩니다.

단, 현재 코드가 공식 기준과 다른 API 응답 범위, DTO 필드 의미, DB 조회 조건, 정렬/계산 정책을 이미 사용자 기능처럼 제공한다면 이 문서에 기록합니다.

패키지나 파일이 존재하더라도 실제 구현이 비어 있거나 placeholder 수준이면 구현 완료로 보지 않습니다. 기능 완료 여부는 기준 문서, API 계약, 실제 컨트롤러/서비스 구현, 이슈/PR 상태를 함께 확인합니다.

해소된 항목은 이 문서에 `해소`, `완료`, `resolved` 상태로 남기지 않고 삭제합니다. 일부만 해소된 경우에는 아직 남은 차이만 좁혀서 다시 작성합니다.

## 현재 코드와 목표 기준 요약

| 영역 | 현재 코드에 남아 있는 형태 | 목표 기준 | 관련 문서 |
| --- | --- | --- | --- |
| 배포/인프라 자동화 범위 | GitHub Actions는 테스트/빌드 CI를 수행. EC2/RDS/S3 운영 배포 가이드, prod compose, S3 저장소 구현은 존재하지만 GitHub Actions 기반 CD workflow는 아직 없음 | 시스템 아키텍처는 운영 배포 구조와 현재 자동화 수준을 구분 | [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md), [aws-setup.md](../deploy/aws-setup.md) |
| AI MD 추천 검증 범위 | `RECO-005` API는 완성형 코디 검증과 스타일 가중 상품 후보 구성을 구현했지만, 외부 상품 포함 저장·저장 실패 및 다중 네이버 검색 조합 경로 테스트가 부족 | 현재 기능 오류가 아니라 후속 테스트 보강 대상. 외부 상품 혼합 코디 저장, 4개 미만 응답, 저장 실패/롤백, 다중 검색 결과 병합 경로를 서비스 테스트로 고정 | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) |
| 탈퇴 30일 경과 후 개인정보 삭제/익명화 | 회원탈퇴 시 `withdrawn_at`을 기록하고 30일 이내 복구 가능한 상태로 관리하지만, 30일 경과 후 개인정보 삭제/익명화 자동 처리는 별도 구현 없음 | 탈퇴 철회 기간이 지나면 약관과 개인정보 처리방침 기준에 따라 개인정보를 삭제하거나 식별할 수 없게 처리 | [data-lifecycle.md](../database/data-lifecycle.md), [legal/README.md](../legal/README.md), [api-contract.md](../api/api-contract.md) |

## 요구사항 ID 연결표

| 세부기능 ID | API/도메인 | 현재 주요 코드 | 기준 문서 | 현재 구현 상태 |
| --- | --- | --- | --- | --- |
| `RECO-004` | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations` | `ClothesRecommendationService` | [requirements-definition.md](../requirements/requirements-definition.md), [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) | 점수 내림차순 정렬. 동점 시 `brandName != UNKNOWN` 우선 |
| `DEPLOY-001`~`DEPLOY-005` | 배포/인프라 자동화 범위 | `.github/workflows/ci.yml`, `deploy/`, `S3ImageStorageService`, `docker-compose.yml` | [requirements-definition.md](../requirements/requirements-definition.md), [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md), [aws-setup.md](../deploy/aws-setup.md) | EC2/RDS/S3 운영 배포 문서와 S3 저장소 구현은 존재. 현재 GitHub Actions는 CI 중심이며 자동 CD workflow는 후속 정리 필요 |
| `RECO-005` | AI MD 추천 API | `RecommendationController`, `AiMdRecommendationService`, `AiMdPersona` | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) | persona 조회, 스타일 가중 상품 후보 조회, 완성형 코디 추천/저장 구현. 가중치 변환, 동일 상품 판별, 필수 카테고리 후보 필터링 테스트는 존재하며, 외부 API 다중 호출 병합과 저장 실패 경로 테스트 보강 필요 |
| 회원탈퇴 | 탈퇴 후 데이터 보존/삭제 | `UserController`, `UserService`, `User` | [data-lifecycle.md](../database/data-lifecycle.md), [legal/README.md](../legal/README.md), [api-contract.md](../api/api-contract.md) | 탈퇴 시 `withdrawn_at` 기록과 30일 이내 복구 흐름은 구현. 30일 경과 후 개인정보 삭제/익명화 자동 처리 기준은 후속 구현 필요 |

## BE 코드와 공식 기준 확인 필요

### 회원탈퇴 30일 경과 후 개인정보 삭제/익명화

회원탈퇴 시 계정은 탈퇴 상태가 되고 `withdrawn_at`에 탈퇴 시각을 기록합니다. 탈퇴한 적이 없는 회원의 `withdrawn_at`은 `null`입니다.

현재 구현은 30일 이내 복구 가능한 탈퇴 상태를 관리하는 데 초점이 있습니다. 다만 약관과 개인정보 처리방침 기준상 탈퇴 철회 기간이 지나면 개인정보를 삭제하거나 식별할 수 없게 처리하는 후속 작업이 필요합니다.

후속 구현에서는 아래 기준을 확인합니다.

- 30일 경과 회원을 찾는 기준
- 이메일, 닉네임, 프로필 이미지, 자기소개, 외부 링크, 소셜 계정 연결 등 개인정보성 필드 삭제 또는 익명화 범위
- 옷, 코디, 피드, 추천 학습 데이터처럼 서비스 품질을 위해 보존할 수 있는 비식별 데이터 범위
- 자동 배치, 스케줄러, 관리자 수동 처리 중 운영 방식

이 작업은 현재 로그인/온보딩/마이페이지 보완 범위에서는 구현하지 않고, 후속 이슈에서 처리합니다.

### `DEPLOY-001`~`DEPLOY-005` 배포/인프라 자동화 범위

[system-architecture.md](../architecture/system-architecture.md)는 MVP와 운영 배포까지 고려한 시스템 구성을 설명합니다. 다만 자동 코드리뷰나 문서 검토 시 운영 배포 구조와 현재 자동화 수준을 구분해서 읽어야 합니다.

현재 BE 코드와 레포 설정 기준으로는 아래 상태입니다.

- GitHub Actions는 테스트와 빌드 CI를 수행합니다.
- Docker Compose는 BE 애플리케이션 실행이 아니라 로컬 MySQL/Redis 개발 인프라 실행에 사용합니다.
- `deploy/`, [aws-setup.md](../deploy/aws-setup.md), `application-prod.yml.example`, `docker-compose.prod.yml`에는 EC2/RDS/S3 운영 배포 기준이 정리되어 있습니다.
- AWS S3는 운영 기준 이미지 저장소이며, `STORAGE_BACKEND=s3/local` 환경 변수로 `app.storage.backend` 값을 전환할 수 있습니다.
- `S3ImageStorageService`와 로컬 저장소 구현은 존재합니다.
- GitHub Actions 기반 자동 CD workflow는 아직 구현되지 않았습니다.

자동 코드리뷰와 문서 검토 시 `system-architecture.md`만 보고 현재 구현이 누락되었다고 판단하지 않고, 이 문서의 gap 항목을 함께 확인합니다.

자동 CD workflow가 구현되거나 운영 배포 방식이 변경되면 [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md), 루트 [README](../../README.md), 이 문서를 같은 PR에서 함께 갱신합니다.

### `RECO-005` AI MD 추천 검증 범위

AI MD 추천 API는 사용자 성별에 맞는 MD 목록 조회, MD별 상품 추천, MD별 코디 후보 추천, 선택 코디 저장을 제공합니다.

```text
src/main/java/com/closetnangam/be/domain/recommendation/controller/RecommendationController.java
- GET /api/v1/users/{userId}/recommendations/ai-md/personas
- GET /api/v1/users/{userId}/recommendations/ai-md/{mdId}/products
- POST /api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits
- POST /api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits/save

src/main/java/com/closetnangam/be/domain/recommendation/service/AiMdRecommendationService.java
- Gemini 응답을 기반으로 상품 추천 최대 40개 구성
- 상품 추천 후보는 네이버쇼핑 결과와 `EXTERNAL_SHOPPING` 공용 `CLOTHES` 내부 후보를 함께 사용
- 내부 후보는 사용자 또는 선택한 MD 성별과 `UNISEX` 상품만 사용. 유사상품 추천의 `OTHER`/성별 없음 사용자는 내부 후보 성별 제한 없음
- Gemini 응답을 기반으로 저장 전 코디 후보 4개 구성
- 사용자가 선택한 코디 후보 1개 저장
- 보유 옷과 외부/내부 추천 상품을 합쳐 TOP, BOTTOM, SHOES가 모두 포함된 완성형 코디 검증
- 코디별 보유 옷 포함은 선택 사항이며, 외부/내부 추천 상품만으로 완성된 코디도 유효
- OUTER와 보유 옷은 선택 사항
```

현재 구현은 외부 상품을 선택하지 않은 보유 옷 단독 코디와 보유 옷이 없는 외부/내부 추천 상품 단독 코디를 모두 유효한 응답으로 처리합니다. 단, 최종 구성에 `TOP`, `BOTTOM`, `SHOES`가 모두 있어야 합니다. Gemini가 `wardrobeClothesIds` 또는 `externalProductIds`를 생략하거나 null로 반환하면 빈 목록으로 정규화하며, 이후 전체 구성으로 완성형 코디 여부를 검증합니다.

현재 아래 경로는 테스트가 존재합니다.

- `externalProductIds`가 null이거나 생략된 응답을 빈 목록으로 정규화
- 존재하지 않는 `wardrobeClothesId` 후보를 제외하고 실제 사용자 옷장에 매핑되는 후보를 선택
- 옷장 등록 옷 없이 외부/내부 추천 상품만으로 구성된 완성형 코디를 허용
- 상의만 포함하고 하의 또는 신발이 없는 후보를 완성형 코디에서 제외
- 저장 가능한 후보가 4개를 초과하면 앞에서부터 4개만 확정
- 코디 프롬프트가 필수 구성, 전체 아이템 추천 사유, MD별 말투를 요구하는지 검증
- 추천 사유 fallback과 각 persona의 내부 화법 지침이 서로 구분되는지 검증
- 사용자 스타일 `combinedWeight`와 MD 친화도가 상품 후보 조회 가중치에 반영되는지 검증
- 상품명이 같고 `productId`가 다른 후보를 동일 상품으로 판별하는지 검증
- 상품 추천 프롬프트가 스타일 빈도와 브랜드·카테고리 다양성을 요구하는지 검증
- 상품 추천 1차 선별에서 같은 브랜드 최대 2개, 같은 카테고리 최대 4개 제한 검증

다만 이 기능은 Gemini와 네이버쇼핑 응답을 조합하는 흐름이라, 아래 경로는 추가 서비스 단위 테스트로 고정할 필요가 있습니다.

- 상품 추천 응답에 `candidateSource=INTERNAL`, `clothesId`가 있는 내부 후보와 `candidateSource=NAVER`, `clothesId=null`인 네이버 후보가 함께 포함되는 경로
- 내부 후보의 nullable 가격(`lowestPrice`, `highestPrice`)과 빈 구매 링크(`link=""`)가 응답 계약대로 유지되는 경로
- 내부 후보 조회에서 사용자/MD 성별과 `UNISEX`만 포함하는 경로 및 유사상품 추천 `OTHER`/성별 없음 사용자의 전체 성별 허용 경로
- 외부 상품 없이 보유 옷만으로 TOP, BOTTOM, SHOES를 완성한 후보의 추천 및 저장
- 외부 상품을 1개 이상 포함한 코디 후보 선택 저장
- Gemini가 4개 미만 코디를 반환했을 때 실패 처리
- Gemini가 존재하지 않는 `productId`를 반환했을 때 필터링/검증 처리
- 여러 네이버 검색 페이지와 내부 DB 후보를 합치고 동일 상품을 제거하는 전체 서비스 경로
- 재추천 요청에서 검색 조합이 달라지면서도 고가중치 스타일 빈도가 유지되는 통계적 경로
- 저장 요청에서 TOP, BOTTOM, SHOES 중 하나가 누락됐을 때 `400 Bad Request`를 반환하는 컨트롤러 경로
- 선택 코디 저장 중 외부 상품 생성, `OUTFITS`, `OUTFIT_ITEMS` 저장 실패 시 롤백 처리

남은 경로의 테스트가 추가되기 전까지는 해당 부분을 로컬/CI의 Spring context 테스트와 수동 API 테스트로 확인한 상태로 봅니다.

## 우선 정리 대상

| 우선순위 | 대상 | 이유 |
| --- | --- | --- |
| 1 | `RECO-005` AI MD 추천 검증 범위 | 현재 기능 오류가 아니라 Gemini 응답 변형과 코디 저장 롤백 경로의 후속 테스트 보강 대상 |
| 2 | 회원탈퇴 30일 경과 후 개인정보 삭제/익명화 | 약관/개인정보 처리방침과 데이터 생명주기 기준에 영향 |
| 3 | 배포/인프라 자동화 범위 | 운영 배포 문서/설정과 GitHub Actions 자동화 수준을 혼동할 가능성 |

## 문서 변경 기준

- 이 표에 적힌 현재 구현 차이가 실제 코드 수정으로 해소되면 이 문서도 함께 수정합니다.
- 코드 변경으로 기준 문서와 구현 차이가 새로 생기면 같은 PR에서 이 문서를 갱신합니다.
- 현재 코드를 우선 기준으로 확정하기로 결정한 경우, 코드만 유지하지 않고 관련 기준 문서도 같은 PR에서 함께 수정합니다.
- API 경로, 요청 필드, 응답 필드, 오류 처리 기준이 바뀌면 [api-contract.md](../api/api-contract.md)를 같은 PR에서 수정합니다.
- 기능 범위나 세부기능 ID 기준이 바뀌면 [requirements-definition.md](../requirements/requirements-definition.md)를 먼저 확인하고, [feature-index.md](../requirements/feature-index.md)를 같은 PR에서 수정합니다.
- DB 테이블, 컬럼 의미, 삭제/보존 정책이 바뀌면 [erd.md](../database/erd.md)와 [data-lifecycle.md](../database/data-lifecycle.md)를 함께 확인합니다.
- 도메인 규칙, enum, catalog code, 점수 정책이 바뀌면 [glossary.md](../domain/glossary.md), [catalog.md](../domain/catalog.md), [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md)를 함께 확인합니다.
- 패키지 책임이나 주요 코드 위치 기준이 바뀌면 [package-structure.md](../architecture/package-structure.md)를 같은 PR에서 수정합니다.
- 시스템 구성, 기술 스택, 화면 구조, 주요 기능 흐름이 바뀌면 [system-architecture.md](../architecture/system-architecture.md), [information-architecture.md](../architecture/information-architecture.md), [sequence-diagrams.md](../architecture/sequence-diagrams.md), [tech-stack.md](../architecture/tech-stack.md)를 함께 확인합니다.
- 공통 문서가 변경되면 FE 레포의 동일 문서도 함께 확인합니다.
