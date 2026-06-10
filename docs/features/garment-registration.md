---
doc_type: shared_with_be_details
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-09
---

# 옷 등록 플로우 가이드

이 문서는 옷장난감의 옷 등록 플로우를 정리합니다. 사진 기반 등록, 구매내역 기반 등록, 외부 쇼핑몰 상품 저장은 FE/BE가 모두 같은 흐름으로 이해해야 하며, BE 문서에서는 저장 구조와 API 기준까지 함께 정리합니다.

## 문서 위치 기준

- BE 레포: 공통 등록 흐름과 BE 저장/API 기준을 정리합니다.
- FE 레포: 동일 흐름을 기준으로 화면 단계, 로딩/에러/빈 상태, mock 정책을 상세화합니다.

## 옷 정보 출처

| 코드 | 의미 | 대표 흐름 |
| --- | --- | --- |
| `PHOTO` | 사진 기반 등록 | 사용자가 옷 사진을 업로드하고 AI 분석 결과를 바탕으로 저장 |
| `PURCHASE_HISTORY` | 구매내역 기반 등록 | 사용자가 구매내역 캡처를 업로드하고 분석 결과를 바탕으로 저장 |
| `EXTERNAL_SHOPPING` | 외부 쇼핑몰 등록 | 네이버쇼핑 등 외부 상품 정보를 저장 |

보유/미보유 상태는 `CLOTHES`가 아니라 `WARDROBE_CLOTHES.ownership_status`에 저장합니다.

## 공통 저장 구조

| 데이터 | 저장 위치 | 설명 |
| --- | --- | --- |
| 상품명, 브랜드, 품번, 이미지, 카테고리, 타입, 대상 성별 code | `CLOTHES` | 옷 자체의 공통 정보. 대상 성별 code는 사용자 화면에 노출하지 않음 |
| 옷 정보 출처 | `CLOTHES.clothes_info_source` | `PHOTO`, `PURCHASE_HISTORY`, `EXTERNAL_SHOPPING` |
| 보유/미보유 | `WARDROBE_CLOTHES.ownership_status` | `OWNED`, `WISHLIST` |
| 사용자별 사이즈, 계절, 즐겨찾기 | `WARDROBE_CLOTHES` | 사용자 옷장 기준 정보 |
| 대표/보조 색상 | `CLOTHING_COLORS` | 색상 code와 role |
| 대표/보조 스타일 | `CLOTHING_STYLES` | 스타일 code와 role |

## 사진 기반 등록

```text
1. 사용자가 옷 사진 업로드
2. BE가 이미지를 저장
3. AI 분석 요청
4. 분석 초안 조회
5. 사용자가 초안 확인/수정
6. 최종 저장
```

주요 API:

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/users/{userId}/clothes/photos` | 옷 사진 업로드 |
| POST | `/api/v1/users/{userId}/clothes/photos/{photoId}/analyze` | 업로드 사진 AI 분석 |
| GET | `/api/v1/users/{userId}/clothes/photos/{photoId}/draft` | 사진 분석 기반 등록 초안 조회 |
| POST | `/api/v1/users/{userId}/clothes/photos/{photoId}/save` | 사진 기반 옷 저장 |

저장 요청 예시:

```json
{
  "name": "화이트 반팔 티셔츠",
  "brandName": "UNKNOWN",
  "productCode": "UNKNOWN-001",
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "gender": "UNISEX",
  "primaryColor": "WHITE",
  "secondaryColors": ["NAVY"],
  "styles": ["CASUAL", "MINIMAL"],
  "size": "L",
  "season": "SUMMER",
  "favorite": false,
  "isVerified": false
}
```

## 구매내역 기반 등록

```text
1. 사용자가 구매내역 캡처 이미지 업로드
2. BE가 이미지를 저장
3. AI가 상품명, 브랜드, 카테고리, 타입, 색상, 스타일, 외부 출처 후보 분석
4. 분석 초안 조회
5. 사용자가 초안 확인/수정
6. 최종 저장
```

주요 API:

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures` | 구매내역 캡처 업로드 |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/analyze` | 구매내역 캡처 AI 분석 |
| GET | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/draft` | 구매내역 기반 등록 초안 조회 |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/save` | 구매내역 기반 옷 저장 (상품별 순차 저장) |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/items/{itemIndex}/skip` | 복수 상품 캡처에서 특정 상품 건너뛰기 |

한 캡처에 여러 상품이 있으면 `itemIndex`별로 저장·건너뛰기를 반복합니다. 모든 상품이 `SAVED` 또는 `SKIPPED`가 되면 캡처가 완료됩니다. draft/analyze 응답의 `items[]`에는 `itemIndex`, `gender`, `imageUrl`, `status`가 포함되고, `pendingItemCount`·`captureCompleted`로 진행 상태를 확인합니다.

저장 요청 예시 (단일 상품 — `itemIndex` 생략 시 0번 상품):

```json
{
  "name": "화이트 반팔 티셔츠",
  "brandName": "브랜드명",
  "productCode": "PRODUCT-001",
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "gender": "UNISEX",
  "primaryColor": "WHITE",
  "secondaryColors": [],
  "styles": ["CASUAL"],
  "externalSource": "MUSINSA",
  "size": "L",
  "season": "SUMMER",
  "favorite": false,
  "isVerified": false
}
```

복수 상품 저장 시 선택 필드:

- `itemIndex` (선택, 기본값 0): 저장할 상품 인덱스
- `imageUrl` (선택): 상품별 미리보기 URL. draft `items[].imageUrl`, 요청 값, 없으면 캡처 `previewUrl` 순으로 fallback

분석/초안 응답 공통 필드 (단일·복수 모두):

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `items` | `PurchaseCaptureItemDraft[]` | 상품별 초안. `itemIndex`, `status`(`PENDING`/`SAVED`/`SKIPPED`), `gender`, `imageUrl` 포함 |
| `pendingItemCount` | number | 아직 저장·건너뛰기하지 않은 상품 수 |
| `captureCompleted` | boolean | 모든 상품이 `SAVED` 또는 `SKIPPED`이면 true |

저장 응답(`PurchaseCaptureRegistrationResponse`)도 `itemIndex`, `pendingItemCount`, `captureCompleted`를 포함합니다. 건너뛰기 API는 갱신된 draft 응답을 반환합니다.

## 외부 쇼핑몰 상품 저장

```text
1. 외부 상품 검색 또는 추천 결과 확인
2. 사용자가 상품 저장
3. BE가 외부 상품 정보를 공통 옷 정보로 저장
4. 사용자 옷장에는 미보유 또는 보유 상태로 연결
```

주요 API:

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/naver/search` | 네이버쇼핑 상품 검색 |
| POST | `/api/v1/external/clothes/naver` | 네이버쇼핑 상품을 공통 옷 정보로 저장 |
| POST | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 저장 |

미보유 옷 저장 요청 예시:

```json
{
  "name": "네이비 후드",
  "brandName": "브랜드명",
  "productCode": "PRODUCT-002",
  "imageUrl": "https://example.com/image.jpg",
  "category": "TOP",
  "itemType": "HOODIE",
  "gender": "UNISEX",
  "primaryColor": "NAVY",
  "secondaryColors": [],
  "styles": ["CASUAL", "STREET"],
  "size": "L",
  "season": "FALL",
  "externalSource": "NAVER_SHOPPING",
  "externalProductId": "1234567890",
  "externalProductUrl": "https://example.com/products/1234567890"
}
```

## 이미지 기준

- 옷 사진, 구매내역 캡처, 피드 이미지는 AWS S3 저장 기준으로 관리합니다.
- 현재 로컬 구현은 로컬 저장소를 사용할 수 있지만, 문서 기준은 AWS S3입니다.
- 사용자 옷장에서 옷을 삭제해도 공통 옷 정보와 이미지는 삭제하지 않습니다.

## FE 확인 포인트

- 업로드 중, 분석 중, 분석 실패, 초안 확인, 저장 완료 상태를 구분합니다.
- 분석 결과는 사용자가 수정할 수 있어야 합니다.
- 카테고리, 아이템 타입, 색상, 스타일은 화면 입력/표시와 저장 요청에 사용하며 [카탈로그 사용 가이드](../domain/catalog.md)의 code 값을 사용합니다.
- 대상 성별(`gender`)은 사용자 화면에 표시하거나 사용자가 직접 수정하는 값이 아니라, 내부 분류/추천용 code로 저장 요청에 포함합니다.
- 사진·구매내역 등록 draft/analyze의 `gender` 기본값은 사용자 프로필 성별이며, FE는 별도 표시 없이 저장 요청에 포함합니다.
- 구매내역 캡처 분석에서 일부 값이 불확실할 수 있으므로 수동 보정 UI가 필요합니다.
- AI가 카탈로그 code가 아닌 값을 반환하면 `analysisStatus=FAILED`로 처리하고, 유효한 code만 `SUCCESS` 초안으로 내려갑니다.
- 외부 쇼핑몰 상품 저장은 보유 옷 등록과 미보유 저장을 구분해야 합니다.

## BE 확인 포인트

- 사용자별 리소스는 인증 사용자와 요청 `userId` 일치를 확인합니다.
- `CLOTHES`에는 공통 옷 정보를 저장하고, `WARDROBE_CLOTHES`에는 사용자별 상태를 저장합니다.
- 옷 삭제 시 공통 옷 정보는 삭제하지 않습니다.
- 카탈로그 값 validation은 AI 분석 성공 처리 전·저장 전 모두 수행합니다. 유효하지 않은 code는 `SUCCESS` 초안으로 저장하지 않습니다.
- 외부 출처는 가능한 경우 `external_source`, `external_product_id`, `external_product_url`에 저장합니다.

## 백엔드 코드 위치

```text
src/main/java/com/closetnangam/be/domain/clothes/
├── controller/ClothesController.java
├── controller/PhotoClothesRegistrationController.java
├── controller/WishlistClothesController.java
├── dto/request/
├── entity/Clothes.java
├── entity/WardrobeClothes.java
├── helper/ClothesTagHelper.java
└── service/
```

```text
src/main/java/com/closetnangam/be/domain/purchase/
├── controller/PurchaseCaptureRegistrationController.java
├── dto/request/
├── entity/PurchaseCapture.java
└── service/
```
