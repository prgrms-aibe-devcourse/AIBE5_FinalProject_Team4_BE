---
doc_type: be_implementation_gaps
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-14
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
| 옷장 통계 범위 | `/statistics` API가 `OWNED` 상태의 보유 옷만 계산하고 `totalOwnedCount`를 반환 | 옷장 전체 요약은 `OWNED`와 `WISHLIST`를 함께 고려 | [requirements-definition.md](../requirements/requirements-definition.md), [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) |
| `USER_STYLES.wardrobe_weight` 산정 범위 | `/statistics` API 호출 시 보유 옷 기준 스타일 가중치를 계산해 `USER_STYLES.wardrobe_weight`에 동기화 | `wardrobe_weight`는 사용자의 옷장에 등록된 옷 스타일 기반 점수라는 기준을 따름 | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) |
| `CLOTHES.season` 수정 범위와 계절 기준 | 현재 `season`은 `CLOTHES`에 저장되지만 옷 수정 요청에서 변경 가능. 일부 Swagger/OpenAPI 설명은 `season`을 옷장 정보처럼 설명함. `GET /api/v1/categories` 일반 응답은 계절 목록을 별도 필드로 제공하지 않고, 추천/날씨 계산은 `ClothesSeason` 기준으로 통합됨 | `season`은 `CLOTHES` 공통 정보이며 옷 등록 시 1개 선택하고 생성 후 변경하지 않음 | [requirements-definition.md](../requirements/requirements-definition.md), [erd.md](../database/erd.md), [catalog.md](../domain/catalog.md), [invariants.md](../domain/invariants.md), [api-contract.md](../api/api-contract.md) |
| 이미지 저장 방식 | 현재 이미지 업로드/조회 구현은 로컬 파일 저장소와 `/api/v1/images/**` 조회 endpoint를 사용 | 운영 기준은 AWS S3 저장과 이미지 URL 관리 | [requirements-definition.md](../requirements/requirements-definition.md), [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md), [garment-registration.md](../features/garment-registration.md), [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md) |
| 배포/인프라 목표 구조 | GitHub Actions는 테스트/빌드 CI를 수행하고, Docker Compose는 로컬 MySQL/Redis 개발 인프라를 실행. AWS 배포와 CD 자동화는 진행 예정 | 시스템 아키텍처는 AWS EC2/RDS/S3와 GitHub Actions 기반 배포까지 포함한 목표 구조 | [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md) |
| 추천 응답 형식 | `RECO-002` 추천 응답의 `price`는 "0" 고정, `score`는 0~1 문자열, `reason`은 기술적 매칭 결과 반환 | 실제 가격, 백분율 점수, 사용자 친화적 자연어 추천 이유 제공 | [requirements-definition.md](../requirements/requirements-definition.md), [api-contract.md](../api/api-contract.md), [recommendation-policy.md](../features/recommendation-policy.md) |
| AI MD 추천 검증 범위 | `RECO-006` API는 완성형 코디 검증과 스타일 가중 상품 후보 구성을 구현했지만, 외부 상품 포함 저장·저장 실패 및 다중 네이버 검색 조합 경로 테스트가 부족 | 외부 상품 혼합 코디 저장, 4개 미만 응답, 저장 실패/롤백, 다중 검색 결과 병합 경로를 서비스 테스트로 고정 | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) |
| 개발/임시 API 경계 | local mock token API가 코드에 존재 | 공식 서비스 API는 [api-contract.md](../api/api-contract.md)의 엔드포인트 인덱스를 기준으로 판단 | [requirements-definition.md](../requirements/requirements-definition.md), [api-contract.md](../api/api-contract.md), [feature-index.md](../requirements/feature-index.md) |

## 요구사항 ID 연결표

| 세부기능 ID | API/도메인 | 현재 주요 코드 | 기준 문서 | 현재 구현 상태 |
| --- | --- | --- | --- | --- |
| `WARDROBE-002` | `GET /api/v1/wardrobes/users/{userId}/statistics` | `WardrobeStatisticsService`, `WardrobeStatisticsResponse` | [requirements-definition.md](../requirements/requirements-definition.md), [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) | 보유 옷 기준 통계만 반환. 옷장 전체 요약은 미보유 API 조합 또는 BE 계약 확정 필요 |
| `STYLE-002` | `USER_STYLES.wardrobe_weight` | `WardrobeStatisticsService`, `UserStyle.syncWardrobeWeight`, `WardrobeStatisticsResponse.userStylePayloads` | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md), [erd.md](../database/erd.md) | 통계 API 호출 시 보유 옷 기준 스타일 가중치를 저장. 옷장 전체 등록 기준 반영 여부 확인 필요 |
| `WARDROBE-016`, `WARDROBE-028`, `CATALOG-001` | 옷 계절 수정 기준 | `Clothes`, `ClothesService`, `ClothesUpdateRequest`, `PhotoClothesRegistrationController`, `PurchaseCaptureRegistrationController`, `CategoryCatalogService`, `ClothesSeason` | [requirements-definition.md](../requirements/requirements-definition.md), [feature-index.md](../requirements/feature-index.md), [erd.md](../database/erd.md), [catalog.md](../domain/catalog.md), [api-contract.md](../api/api-contract.md) | `CLOTHES.season` 저장과 AI 분석 필드 반영은 완료. 옷 수정 요청의 `season` 변경 가능성, 일부 OpenAPI 설명, 카탈로그 일반 응답/계절 호환 계산 기준 확인 필요 |
| `RECO-005` | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations` | `ClothesRecommendationService` | [requirements-definition.md](../requirements/requirements-definition.md), [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) | 점수 내림차순 정렬. 동점 시 `brandName != UNKNOWN` 우선 |
| `DEPLOY-004` | 이미지 저장과 조회 | `LocalImageStorageService`, `ImageController`, `StorageProperties` | [requirements-definition.md](../requirements/requirements-definition.md), [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md), [garment-registration.md](../features/garment-registration.md), [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md) | 현재 로컬 저장소 기반. 운영 기준인 AWS S3 전환 여부 확인 필요 |
| `DEPLOY-001`~`DEPLOY-005` | 배포/인프라 목표 구조 | `.github/workflows/ci.yml`, `docker-compose.yml` | [requirements-definition.md](../requirements/requirements-definition.md), [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md) | 시스템 아키텍처는 목표 구조 기준. 현재 GitHub Actions는 CI, Docker Compose는 로컬 MySQL/Redis 실행, AWS 배포/CD 자동화는 진행 예정 |
| `RECO-002` | `GET /api/v1/recommendations/{wardrobeId}` | `StyleProductRecommender`, `RecommendResponse` | [requirements-definition.md](../requirements/requirements-definition.md), [api-contract.md](../api/api-contract.md), [recommendation-policy.md](../features/recommendation-policy.md) | `price` placeholder("0"), 0~1 점수 형식, 기술적 추천 이유 제공. 기준 문서와 응답 형식 차이 존재 |
| `RECO-006` | AI MD 추천 API | `RecommendationController`, `AiMdRecommendationService`, `AiMdPersona` | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) | persona 조회, 스타일 가중 다중 상품 검색, 완성형 코디 추천/저장 구현. 가중치 변환, 동일 상품 판별, 필수 카테고리 후보 필터링 테스트는 존재하며, 외부 API 다중 호출 병합과 저장 실패 경로 테스트 보강 필요 |
| 개발/임시 API | `GET /api/v1/auth/mock-token` | `MockAuthController` | [api-contract.md](../api/api-contract.md), [feature-index.md](../requirements/feature-index.md) | 공식 사용자 기능으로 보지 않음. local 개발 경계 확인 필요 |

## BE 코드와 공식 기준 확인 필요

### `WARDROBE-002` 옷장 통계

요구사항 정의서에서 `WARDROBE-002`는 사용자 옷장에 등록된 보유/미보유 옷 통계 조회입니다. 옷장 전체 요약을 표시할 때는 `OWNED`와 `WISHLIST`를 모두 고려하는 것이 기준입니다.

현재 BE 구현은 `/api/v1/wardrobes/users/{userId}/statistics`에서 `OWNED` 상태의 보유 옷만 계산합니다.

```text
src/main/java/com/closetnangam/be/domain/wardrobe/service/WardrobeStatisticsService.java
- findOwnedForStatistics(userId, OwnershipStatus.OWNED)
```

현재 응답 DTO도 `totalOwnedCount`, `itemTypes`, `userStylePayloads` 중심입니다.

```text
src/main/java/com/closetnangam/be/domain/wardrobe/dto/response/WardrobeStatisticsResponse.java
- totalOwnedCount
- itemTypes
- userStylePayloads
```

따라서 `totalOwnedCount`는 옷장 전체 개수가 아니라 보유 옷 개수로 사용해야 합니다. FE가 옷장 전체 등록 수 또는 미보유 옷 수를 표시해야 하는 경우 아래 중 하나가 필요합니다.

- BE 통계 API 응답에 보유/미보유/전체 개수를 분리한 필드를 추가합니다.
- FE가 보유 옷 API와 미보유 옷 API 응답을 조합해 옷장 전체 요약을 계산합니다.

현재 API 구현 변경은 이 문서의 범위가 아닙니다. API 계약을 변경하거나 응답 필드를 추가하는 작업은 담당자 확인 후 별도 이슈 또는 PR로 진행합니다.

### `STYLE-002` USER_STYLES.wardrobe_weight

공식 기준 문서에서 `USER_STYLES.wardrobe_weight`는 사용자의 옷장에 등록된 옷 스타일 기반 점수입니다.

현재 `/statistics` API의 `userStylePayloads`는 보유 옷 기준으로 계산되며, 같은 요청 안에서 `USER_STYLES.wardrobe_weight`에 동기화됩니다. 따라서 현재 구현에서는 미보유 옷의 스타일이 `wardrobe_weight`에 반영되지 않습니다.

확인이 필요한 기준은 아래와 같습니다.

- `wardrobe_weight`를 옷장 전체 등록 기준으로 계산할지
- `wardrobe_weight`를 보유 옷 기준으로만 계산할지
- 전체 옷장 요약과 스타일 가중치 산정 범위를 서로 다르게 둘지
- GET 통계 조회 API가 `USER_STYLES.wardrobe_weight` 저장까지 수행하는 현재 구조를 유지할지

현재까지의 기준 문서 표현은 옷장 등록 기준에 가깝습니다. 담당자와 논의하여 현재 코드 기준을 공식 기준으로 확정한다면 [domain/invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md), [database/erd.md](../database/erd.md), [api-contract.md](../api/api-contract.md)를 같은 PR에서 함께 수정합니다.

### `WARDROBE-016`, `WARDROBE-028`, `CATALOG-001` 옷 계절 수정 기준

ERD v2.3과 공식 기준 문서에서 `season`은 `CLOTHES.season`에 저장하는 공통 옷 정보입니다. 옷마다 계절은 1개만 부여하며, 옷 등록 시 선택한 뒤 생성된 옷의 계절은 변경하지 않습니다.

최신 `develop` 기준으로 아래 항목은 공식 기준에 맞게 반영되어 있습니다.

```text
src/main/java/com/closetnangam/be/domain/clothes/entity/Clothes.java
- private ClothesSeason season

src/main/java/com/closetnangam/be/domain/clothes/entity/WardrobeClothes.java
- season 필드 없음

src/main/java/com/closetnangam/be/domain/clothes/dto/response/PhotoClothesDraftResponse.java
src/main/java/com/closetnangam/be/domain/purchase/dto/response/PurchaseCaptureDraftResponse.java
src/main/java/com/closetnangam/be/global/external/gemini/dto/GeminiClothingClassificationResult.java
src/main/java/com/closetnangam/be/global/external/gemini/dto/GeminiPurchaseCaptureExtractionResult.java
- season 필드 포함
```

다만 현재 옷 수정 요청은 여전히 `season`을 받을 수 있고, 서비스에서 `CLOTHES.season`을 갱신합니다.

```text
src/main/java/com/closetnangam/be/domain/clothes/dto/request/ClothesUpdateRequest.java
- String season

src/main/java/com/closetnangam/be/domain/clothes/service/ClothesService.java
- clothes.update(..., ClothesSeason.fromCodeOrDefault(request.season()), ...)
```

현재 Swagger/OpenAPI 설명에도 `season`을 옷장 정보처럼 읽히게 하는 문구가 남아 있습니다.

```text
src/main/java/com/closetnangam/be/domain/clothes/controller/PhotoClothesRegistrationController.java
- "옷장 전용 정보(size, season 등)"

src/main/java/com/closetnangam/be/domain/purchase/controller/PurchaseCaptureRegistrationController.java
- "옷장 정보(size, season 등)"
```

또한 `GET /api/v1/categories` 일반 응답은 계절 code 목록을 별도 필드로 제공하지 않고, 사용 가이드 `fields`/`example`에도 `season`을 포함하지 않습니다. AI 분류용 텍스트 가이드에는 `ClothesSeason` 목록이 포함되어 있습니다.

추천/날씨 계절 호환 계산은 `ClothesSeason` 기준으로 통합되었습니다.

따라서 현재 구현은 아래 기준과 차이가 있습니다.

- 생성된 옷의 계절을 옷 수정 요청에서 변경할 수 있습니다.
- 일부 Swagger/OpenAPI 설명에서 `season`을 옷장 정보처럼 설명합니다.
- `GET /api/v1/categories` 응답은 계절 code 목록을 별도 필드로 제공하지 않습니다.
- 추천/날씨 계절 호환 계산은 `ClothesSeason` 기준으로 수행됩니다.

공식 기준을 유지한다면 구현 PR에서 생성 후 수정 요청이 `season`을 변경하지 않도록 API/DTO/서비스 책임과 OpenAPI 설명을 함께 정리해야 합니다. 이때 카탈로그 일반 응답에서 계절 code를 내려줄지, 문서 기준 code만 사용할지 확정하고, 추천/날씨 계절 호환 계산도 `CLOTHES.season` code 기준으로 정리합니다. 반대로 현재 코드 기준을 공식 기준으로 확정한다면 [erd.md](../database/erd.md), [invariants.md](../domain/invariants.md), [catalog.md](../domain/catalog.md), [garment-registration.md](../features/garment-registration.md), [api-contract.md](../api/api-contract.md)를 같은 PR에서 수정합니다.

### `DEPLOY-004` 이미지 저장 방식

공식 기준 문서에서 옷 사진, 구매내역 캡처, 피드 이미지는 AWS S3 저장 기준으로 관리합니다.

현재 BE 구현은 로컬 파일 저장소를 사용합니다.

```text
src/main/java/com/closetnangam/be/global/storage/LocalImageStorageService.java
- storeClothesPhoto(...)
- storePurchaseCapture(...)

src/main/java/com/closetnangam/be/global/config/StorageProperties.java
- app.storage.local.basePath
- app.storage.local.baseUrl

src/main/java/com/closetnangam/be/global/storage/ImageController.java
- GET /api/v1/images/clothes/{userId}/{filename}
- GET /api/v1/images/purchase-captures/{userId}/{filename}
```

따라서 현재 코드의 이미지 저장 방식은 운영 기준인 AWS S3가 아니라 로컬 개발 저장소 기준으로 이해합니다. S3 저장소로 전환하거나 로컬 저장소를 공식 기준으로 확정한다면 [api-contract.md](../api/api-contract.md), [domain/invariants.md](../domain/invariants.md), [garment-registration.md](../features/garment-registration.md), [data-lifecycle.md](../database/data-lifecycle.md), [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md)를 같은 PR에서 함께 수정합니다.

### `DEPLOY-001`~`DEPLOY-005` 목표 배포 구조와 현재 로컬/CI 상태

[system-architecture.md](../architecture/system-architecture.md)는 현재 로컬 구현만이 아니라 MVP와 운영 배포까지 고려한 목표 시스템 구성을 설명합니다. 따라서 AWS EC2, RDS, S3, GitHub Actions 기반 배포 흐름은 목표 구조 기준으로 읽습니다.

현재 BE 코드와 레포 설정 기준으로는 아래 상태입니다.

- GitHub Actions는 테스트와 빌드 CI를 수행합니다.
- EC2 자동 배포 CD workflow는 아직 구현되지 않았습니다.
- Docker Compose는 BE 애플리케이션 실행이 아니라 로컬 MySQL/Redis 개발 인프라 실행에 사용합니다.
- AWS S3는 운영 기준 이미지 저장소이며, 현재 구현은 로컬 이미지 저장소를 사용합니다.

자동 코드리뷰와 문서 검토 시 `system-architecture.md`만 보고 현재 구현이 누락되었다고 판단하지 않고, 이 문서의 gap 항목을 함께 확인합니다.

AWS 배포 또는 CD workflow가 구현되면 [system-architecture.md](../architecture/system-architecture.md), [tech-stack.md](../architecture/tech-stack.md), [project-plan.md](../planning/project-plan.md), 루트 [README](../../README.md), 이 문서를 같은 PR에서 함께 갱신합니다.

### `RECO-002` 취향 기반 상품 추천 응답 형식

공식 기준 문서 및 [api-contract.md](../api/api-contract.md)에서는 추천 상품의 가격(`price`), 백분율 점수(`score`), 그리고 사용자 친화적인 자연어 추천 이유(`reason`)를 예시로 제시합니다.

현재 BE 구현(`StyleProductRecommender.java`)은 아래와 같은 placeholder 및 기술적 데이터를 반환합니다.

- `price`: 항상 `"0"` 반환 (현재 상품 엔티티에 가격 정보가 없음)
- `score`: `0.00` ~ `1.00` 사이의 점수를 문자열로 반환 (예: `"0.85"`)
- `reason`: `"Style Match: 0.8, Weather Match: 1.0"` 형태의 기술적 매칭 점수 요약 반환
- `brandName`, `category`, `primaryColor`, `primaryStyle`: 상품의 기본 메타데이터 정보 포함
- `clothesId`: 피드백 매핑을 위한 내부 옷 ID 포함

FE는 이 응답을 UI에 그대로 노출하기보다는, 아래와 같은 처리가 필요하거나 BE의 향후 개선을 기다려야 합니다.

- 가격 정보가 `"0"`인 경우 처리 (예: 노출 제외 또는 placeholder 문구)
- 점수를 백분율로 환산하여 표시 (예: `score * 100`)
- 기술적 추천 이유를 사용자에게 적절히 가공하여 표시

실제 가격 데이터 연동 및 자연어 추천 생성 로직이 도입되기 전까지 이 항목을 현재 gap으로 유지합니다.

### `RECO-006` AI MD 추천 검증 범위

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
- 내부 후보는 사용자 또는 선택한 MD 성별과 `UNISEX` 상품만 사용
- Gemini 응답을 기반으로 저장 전 코디 후보 4개 구성
- 사용자가 선택한 코디 후보 1개 저장
- 코디별 보유 옷 최소 1개 포함 검증
- 보유 옷과 외부 상품을 합쳐 TOP, BOTTOM, SHOES가 모두 포함된 완성형 코디 검증
- OUTER와 외부 상품은 선택 사항
```

현재 구현은 외부 상품을 선택하지 않은 보유 옷 단독 코디도 유효한 응답으로 처리합니다. 단, 보유 옷만으로 `TOP`, `BOTTOM`, `SHOES`가 모두 구성되어야 합니다. Gemini가 `externalProductIds`를 생략하거나 null로 반환하면 빈 목록으로 정규화하며, 이후 보유 옷과 합친 전체 구성으로 완성형 코디 여부를 검증합니다.

현재 아래 경로는 테스트가 존재합니다.

- `externalProductIds`가 null이거나 생략된 응답을 빈 목록으로 정규화
- 존재하지 않는 `wardrobeClothesId` 후보를 제외하고 실제 사용자 옷장에 매핑되는 후보를 선택
- 상의만 포함하고 하의 또는 신발이 없는 후보를 완성형 코디에서 제외
- 저장 가능한 후보가 4개를 초과하면 앞에서부터 4개만 확정
- 코디 프롬프트가 필수 구성, 전체 아이템 추천 사유, MD별 말투를 요구하는지 검증
- 추천 사유 fallback과 각 persona의 내부 화법 지침이 서로 구분되는지 검증
- 사용자 스타일 `combinedWeight`와 MD 친화도가 상품 검색 가중치에 반영되는지 검증
- 상품명이 같고 `productId`가 다른 후보를 동일 상품으로 판별하는지 검증
- 상품 추천 프롬프트가 스타일 빈도와 브랜드·카테고리 다양성을 요구하는지 검증
- 상품 추천 1차 선별에서 같은 브랜드 최대 2개, 같은 카테고리 최대 4개 제한 검증

다만 이 기능은 Gemini와 네이버쇼핑 응답을 조합하는 흐름이라, 아래 경로는 추가 서비스 단위 테스트로 고정할 필요가 있습니다.

- 상품 추천 응답에 `candidateSource=INTERNAL`, `clothesId`가 있는 내부 후보와 `candidateSource=NAVER`, `clothesId=null`인 네이버 후보가 함께 포함되는 경로
- 내부 후보의 nullable 가격(`lowestPrice`, `highestPrice`)과 빈 구매 링크(`link=""`)가 응답 계약대로 유지되는 경로
- 내부 후보 조회에서 사용자/MD 성별과 `UNISEX`만 포함하는 경로
- 외부 상품 없이 보유 옷만으로 TOP, BOTTOM, SHOES를 완성한 후보의 추천 및 저장
- 외부 상품을 1개 이상 포함한 코디 후보 선택 저장
- Gemini가 4개 미만 코디를 반환했을 때 실패 처리
- Gemini가 존재하지 않는 `productId`를 반환했을 때 필터링/검증 처리
- 여러 네이버 검색 페이지와 내부 DB 후보를 합치고 동일 상품을 제거하는 전체 서비스 경로
- 재추천 요청에서 검색 조합이 달라지면서도 고가중치 스타일 빈도가 유지되는 통계적 경로
- 저장 요청에서 TOP, BOTTOM, SHOES 중 하나가 누락됐을 때 `400 Bad Request`를 반환하는 컨트롤러 경로
- 선택 코디 저장 중 외부 상품 생성, `OUTFITS`, `OUTFIT_ITEMS` 저장 실패 시 롤백 처리

남은 경로의 테스트가 추가되기 전까지는 해당 부분을 로컬/CI의 Spring context 테스트와 수동 API 테스트로 확인한 상태로 봅니다.

### 개발/임시 API와 공식 API 계약 경계

현재 BE 코드에는 공식 API 계약에 포함하지 않은 개발 성격의 엔드포인트가 있습니다.

```text
src/main/java/com/closetnangam/be/global/common/controller/MockAuthController.java
- GET /api/v1/auth/mock-token
- @Profile("local")
```

`/api/v1/auth/mock-token`은 local profile에서 사용하는 테스트용 JWT 발급 API입니다. 공식 로그인 기능이나 사용자 제공 API로 보지 않습니다.

`GET /api/weather`는 추천 보조 정보로 사용하는 공식 날씨 조회 API이며, 개발/임시 API로 분류하지 않습니다.

API 문서 검토 시 공식 서비스 API 여부는 [api-contract.md](../api/api-contract.md)의 엔드포인트 인덱스와 [feature-index.md](../requirements/feature-index.md)를 기준으로 판단합니다. 개발 API를 유지하거나 제거하는 판단은 담당자 확인 후 별도 이슈 또는 PR로 진행합니다.

## 우선 정리 대상

| 우선순위 | 대상 | 이유 |
| --- | --- | --- |
| 1 | `CLOTHES.season` 수정 범위와 계절 기준 | ERD v2.3, 옷 등록/수정 API, 추천 계절 계산 기준에 직접 영향 |
| 2 | `WARDROBE-002` 통계 범위 | FE 옷장 요약과 API 응답 필드 해석에 직접 영향 |
| 3 | `USER_STYLES.wardrobe_weight` 산정 범위 | 사용자 취향 점수와 추천 개인화 기준에 영향 |
| 4 | `DEPLOY-004` 이미지 저장 방식 | 운영 저장소 기준과 현재 로컬 저장 구현 차이에 영향 |
| 5 | `RECO-002` 추천 응답 형식 | FE 추천 UI의 데이터 표시 및 해석 방식에 직접 영향 |
| 6 | `RECO-006` AI MD 추천 검증 범위 | Gemini 응답 변형과 코디 저장 롤백 경로에 영향 |
| 7 | 개발/임시 API 경계 | FE가 local mock endpoint를 공식 서비스 API로 오해할 가능성 |
| 8 | 배포/인프라 목표 구조와 현재 로컬/CI 상태 | AWS 배포 및 CD 구현 시 시스템 문서와 실제 BE 레포 설정 정합성에 영향 |

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
