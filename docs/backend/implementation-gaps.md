---
doc_type: be_implementation_gaps
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-08
---

# BE 구현 정합성 현황

이 문서는 현재 BE 코드와 `docs/` 공식 기준 사이의 차이를 정리합니다. 차이는 곧바로 오류라는 뜻이 아니라, 실제 구현 또는 문서 기준 확정 단계에서 맞춰야 할 기준을 명확히 하기 위한 기록입니다.

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

## 현재 코드와 목표 기준 요약

| 영역 | 현재 코드에 남아 있는 형태 | 목표 기준 | 관련 문서 |
| --- | --- | --- | --- |
| 옷장 통계 범위 | `/statistics` API가 `OWNED` 상태의 보유 옷만 계산하고 `totalOwnedCount`를 반환 | 옷장 전체 요약은 `OWNED`와 `WISHLIST`를 함께 고려 | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) |
| `USER_STYLES.wardrobe_weight` 산정 범위 | `/statistics` API의 `userStylePayloads`는 보유 옷 기준 스타일 가중치 후보값으로 계산 | `wardrobe_weight`는 사용자의 옷장에 등록된 옷 스타일 기반 점수라는 기준을 따름 | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) |
| 추천 동점 처리 | 보유 옷 기준 추천 API가 점수 내림차순으로만 정렬하고 동점 그룹 랜덤 처리는 하지 않음 | 같은 점수 그룹 안에서는 랜덤 노출 | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) |
| 이미지 저장 방식 | 현재 이미지 업로드/조회 구현은 로컬 파일 저장소와 `/api/v1/images/**` 조회 endpoint를 사용 | 운영 기준은 AWS S3 저장과 이미지 URL 관리 | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md), [garment-registration.md](../features/garment-registration.md) |
| 추천 응답 형식 | `RECO-002` 추천 응답의 `price`는 "0" 고정, `score`는 0~1 문자열, `reason`은 기술적 매칭 결과 반환 | 실제 가격, 백분율 점수, 사용자 친화적 자연어 추천 이유 제공 | [api-contract.md](../api/api-contract.md), [home-recommendation.md](../features/home-recommendation.md) |
| AI MD 추천 검증 범위 | `RECO-006` API는 구현되어 있으나 Gemini 응답 변형과 저장 롤백 경로에 대한 직접 테스트가 부족 | AI 응답 null/누락 필드, 보유 옷만 포함한 코디, 외부 상품 혼합 코디를 서비스 테스트로 고정 | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) |
| 개발/임시 API 경계 | local mock token API와 임시 `/login` endpoint가 코드에 존재 | 공식 서비스 API는 [api-contract.md](../api/api-contract.md)의 엔드포인트 인덱스를 기준으로 판단 | [api-contract.md](../api/api-contract.md), [feature-index.md](../requirements/feature-index.md) |

## Feature ID 연결표

| F-ID | API/도메인 | 현재 주요 코드 | 기준 문서 | 현재 구현 상태 |
| --- | --- | --- | --- | --- |
| `WARD-002` | `GET /api/v1/wardrobes/users/{userId}/statistics` | `WardrobeStatisticsService`, `WardrobeStatisticsResponse` | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) | 보유 옷 기준 통계만 반환. 옷장 전체 요약은 미보유 API 조합 또는 BE 계약 확정 필요 |
| `STYLE-002` | `USER_STYLES.wardrobe_weight` | `WardrobeStatisticsService`, `WardrobeStatisticsResponse.userStylePayloads` | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md), [erd.md](../database/erd.md) | 보유 옷 기준 스타일 가중치 후보값 계산. 옷장 전체 등록 기준 반영 여부 확인 필요 |
| `RECO-004` | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations` | `ClothesRecommendationService` | [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md) | 점수 내림차순 정렬. 동점 그룹 랜덤 노출 기준 반영 여부 확인 필요 |
| `EXT-002` | 이미지 저장과 조회 | `LocalImageStorageService`, `ImageController`, `StorageProperties` | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md), [garment-registration.md](../features/garment-registration.md) | 현재 로컬 저장소 기반. 운영 기준인 AWS S3 전환 여부 확인 필요 |
| `RECO-002` | `GET /api/v1/recommendations/{wardrobeId}` | `StyleProductRecommender`, `RecommendResponse` | [api-contract.md](../api/api-contract.md), [home-recommendation.md](../features/home-recommendation.md) | `price` placeholder("0"), 0~1 점수 형식, 기술적 추천 이유 제공. 기준 문서와 응답 형식 차이 존재 |
| `RECO-006` | AI MD 추천 API | `RecommendationController`, `AiMdRecommendationService`, `AiMdPersona` | [feature-index.md](../requirements/feature-index.md), [api-contract.md](../api/api-contract.md) | persona 조회, 상품 추천, 코디 추천/저장 구현. Gemini 응답 변형과 저장 실패 경로에 대한 직접 테스트 보강 필요 |
| 개발/임시 API | `GET /api/v1/auth/mock-token`, `GET /login` | `MockAuthController`, `WeatherController` | [api-contract.md](../api/api-contract.md), [feature-index.md](../requirements/feature-index.md) | 공식 사용자 기능으로 보지 않음. local 또는 임시 개발 경계 확인 필요 |

## BE 코드와 공식 기준 확인 필요

### `WARD-002` 옷장 통계

공통 기능 정의에서 `WARD-002`는 사용자 옷장에 등록된 보유/미보유 옷 통계 조회입니다. 옷장 전체 요약을 표시할 때는 `OWNED`와 `WISHLIST`를 모두 고려하는 것이 기준입니다.

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

현재 `/statistics` API의 `userStylePayloads`는 보유 옷 기준으로 계산됩니다. 따라서 이 값을 그대로 `USER_STYLES.wardrobe_weight`에 반영할 경우, 미보유 옷의 스타일은 반영되지 않습니다.

확인이 필요한 기준은 아래와 같습니다.

- `wardrobe_weight`를 옷장 전체 등록 기준으로 계산할지
- `wardrobe_weight`를 보유 옷 기준으로만 계산할지
- 전체 옷장 요약과 스타일 가중치 산정 범위를 서로 다르게 둘지

현재까지의 기준 문서 표현은 옷장 등록 기준에 가깝습니다. 담당자와 논의하여 현재 코드 기준을 공식 기준으로 확정한다면 [domain/invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md), [database/erd.md](../database/erd.md)를 같은 PR에서 함께 수정합니다.

### `RECO-004` 추천 동점 처리

공식 기준 문서에서 추천 점수가 같은 후보는 같은 점수 그룹 안에서 랜덤 노출합니다.

현재 BE의 보유 옷 기준 추천 API는 후보를 점수 내림차순으로 정렬한 뒤 `limit`을 적용합니다.

```text
src/main/java/com/closetnangam/be/domain/clothes/service/ClothesRecommendationService.java
- sorted(Comparator.comparingInt(RecommendedItem::compatibilityScore).reversed())
- limit(limit)
```

현재 구현에는 같은 점수 그룹을 섞는 처리가 없습니다. 따라서 같은 점수 후보의 노출 순서는 후보 조회 순서와 정렬 안정성에 영향을 받을 수 있습니다.

이 항목은 이미 구현된 아래 API에 대한 정합성 확인입니다.

```text
GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations
```

담당자와 논의하여 현재처럼 결정적 정렬을 공식 기준으로 확정한다면 [domain/invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md)를 같은 PR에서 수정합니다. 동점 랜덤 노출 기준을 유지한다면 구현 PR에서 이 문서를 함께 갱신합니다.

### `EXT-002` 이미지 저장 방식

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

따라서 현재 코드의 이미지 저장 방식은 운영 기준인 AWS S3가 아니라 로컬 개발 저장소 기준으로 이해합니다. S3 저장소로 전환하거나 로컬 저장소를 공식 기준으로 확정한다면 [api-contract.md](../api/api-contract.md), [domain/invariants.md](../domain/invariants.md), [garment-registration.md](../features/garment-registration.md), [data-lifecycle.md](../database/data-lifecycle.md)를 같은 PR에서 함께 수정합니다.

### `RECO-002` 취향 기반 상품 추천 응답 형식

공식 기준 문서 및 [api-contract.md](../api/api-contract.md)에서는 추천 상품의 가격(`price`), 백분율 점수(`score`), 그리고 사용자 친화적인 자연어 추천 이유(`reason`)를 예시로 제시합니다.

현재 BE 구현(`StyleProductRecommender.java`)은 아래와 같은 placeholder 및 기술적 데이터를 반환합니다.

- `price`: 항상 `"0"` 반환 (현재 상품 엔티티에 가격 정보가 없음)
- `score`: `0.00` ~ `1.00` 사이의 점수를 문자열로 반환 (예: `"0.85"`)
- `reason`: `"Style Match: 0.8, Weather Match: 1.0"` 형태의 기술적 매칭 점수 요약 반환

FE는 이 응답을 UI에 그대로 노출하기보다는, 아래와 같은 처리가 필요하거나 BE의 향후 개선을 기다려야 합니다.

- 가격 정보가 `"0"`인 경우 처리 (예: 노출 제외 또는 placeholder 문구)
- 점수를 백분율로 환산하여 표시 (예: `score * 100`)
- 기술적 추천 이유를 사용자에게 적절히 가공하여 표시

향후 실제 가격 데이터 연동 및 자연어 추천 생성 로직이 도입될 때 이 gap을 해소할 예정입니다.

### `RECO-006` AI MD 추천 검증 범위

AI MD 추천 API는 사용자 성별에 맞는 MD 목록 조회, MD별 상품 추천, MD별 코디 추천 및 저장을 제공합니다.

```text
src/main/java/com/closetnangam/be/domain/recommendation/controller/RecommendationController.java
- GET /api/v1/users/{userId}/recommendations/ai-md/personas
- GET /api/v1/users/{userId}/recommendations/ai-md/{mdId}/products
- POST /api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits

src/main/java/com/closetnangam/be/domain/recommendation/service/AiMdRecommendationService.java
- Gemini 응답을 기반으로 상품 추천 10개 구성
- Gemini 응답을 기반으로 코디 4개 저장
- 코디별 보유 옷 최소 1개 포함 검증
```

현재 구현은 외부 상품을 선택하지 않은 보유 옷 단독 코디도 유효한 응답으로 처리합니다. Gemini가 `externalProductIds`를 생략하거나 null로 반환해도 빈 목록으로 정규화합니다.

다만 이 기능은 Gemini와 네이버쇼핑 응답을 조합하는 흐름이라, 아래 경로는 서비스 단위 테스트로 고정할 필요가 있습니다.

- `externalProductIds`가 null이거나 생략된 코디 저장
- 외부 상품을 1개 이상 포함한 코디 저장
- Gemini가 4개 미만 코디를 반환했을 때 실패 처리
- Gemini가 존재하지 않는 `wardrobeClothesId` 또는 `productId`를 반환했을 때 필터링/검증 처리

해당 테스트가 추가되기 전까지는 로컬/CI의 Spring context 테스트와 수동 API 테스트만으로 동작을 확인한 상태로 봅니다.

### 개발/임시 API와 공식 API 계약 경계

현재 BE 코드에는 공식 API 계약에 포함하지 않은 개발 또는 임시 성격의 엔드포인트가 있습니다.

```text
src/main/java/com/closetnangam/be/global/common/controller/MockAuthController.java
- GET /api/v1/auth/mock-token
- @Profile("local")

src/main/java/com/closetnangam/be/global/external/weather/controller/WeatherController.java
- GET /login
```

`/api/v1/auth/mock-token`은 local profile에서 사용하는 테스트용 JWT 발급 API입니다. 공식 로그인 기능이나 사용자 제공 API로 보지 않습니다.

`/login`은 임시 로그인 페이지 문자열을 반환하는 endpoint입니다. 공식 API 계약 또는 FE 연동 기준으로 보지 않습니다.

AI 코드리뷰 또는 API 문서 검토 시 공식 서비스 API 여부는 [api-contract.md](../api/api-contract.md)의 엔드포인트 인덱스와 [feature-index.md](../requirements/feature-index.md)를 기준으로 판단합니다. 개발/임시 API를 유지하거나 제거하는 판단은 담당자 확인 후 별도 이슈 또는 PR로 진행합니다.

## 우선 정리 대상

| 우선순위 | 대상 | 이유 |
| --- | --- | --- |
| 1 | `WARD-002` 통계 범위 | FE 옷장 요약과 API 응답 필드 해석에 직접 영향 |
| 2 | `USER_STYLES.wardrobe_weight` 산정 범위 | 사용자 취향 점수와 추천 개인화 기준에 영향 |
| 3 | `RECO-004` 동점 랜덤 노출 | 현재 제공 추천 API의 노출 순서와 추천 정책 기준에 영향 |
| 4 | `EXT-002` 이미지 저장 방식 | 운영 저장소 기준과 현재 로컬 저장 구현 차이에 영향 |
| 5 | `RECO-002` 추천 응답 형식 | FE 추천 UI의 데이터 표시 및 해석 방식에 직접 영향 |
| 6 | `RECO-006` AI MD 추천 검증 범위 | Gemini 응답 변형과 코디 저장 롤백 경로에 영향 |
| 7 | 개발/임시 API 경계 | AI와 FE가 local/mock endpoint를 공식 서비스 API로 오해할 가능성 |

## 문서 변경 기준

- 이 표에 적힌 현재 구현 차이가 실제 코드 수정으로 해소되면 이 문서도 함께 수정합니다.
- 코드 변경으로 기준 문서와 구현 차이가 새로 생기면 같은 PR에서 이 문서를 갱신합니다.
- 현재 코드를 우선 기준으로 확정하기로 결정한 경우, 코드만 유지하지 않고 관련 기준 문서도 같은 PR에서 함께 수정합니다.
- API 경로, 요청 필드, 응답 필드, 오류 처리 기준이 바뀌면 [api-contract.md](../api/api-contract.md)를 같은 PR에서 수정합니다.
- 기능 범위나 F-ID 연결 기준이 바뀌면 [feature-index.md](../requirements/feature-index.md)를 같은 PR에서 수정합니다.
- DB 테이블, 컬럼 의미, 삭제/보존 정책이 바뀌면 [erd.md](../database/erd.md)와 [data-lifecycle.md](../database/data-lifecycle.md)를 함께 확인합니다.
- 도메인 규칙, enum, catalog code, 점수 정책이 바뀌면 [glossary.md](../domain/glossary.md), [catalog.md](../domain/catalog.md), [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md)를 함께 확인합니다.
- 패키지 책임이나 주요 코드 위치 기준이 바뀌면 [package-structure.md](../architecture/package-structure.md)를 같은 PR에서 수정합니다.
- 공통 문서가 변경되면 FE 레포의 동일 문서도 함께 확인합니다.
