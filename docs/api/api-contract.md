---
doc_type: be_api_contract
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-11
---

# API 계약

이 문서는 BE API의 공통 응답 형식, 경로 기준, 오류 처리, 주요 엔드포인트를 정리합니다.
FE에서 사용하는 API 요약은 FE 문서에서 별도로 정리하되, 원본 계약은 이 문서를 기준으로 합니다.

## 경로 기준

- `/api/v1` 경로가 존재하는 API는 `/api/v1` 경로를 사용합니다.
- `/api/v1` 경로가 없는 API는 현재 구현된 `/api` 경로를 사용합니다.
- `/api`에서 `/api/v1`로 통일하는 작업은 별도 PR에서 기존 FE 사용처와 함께 확인합니다.
- Swagger UI는 `/swagger-ui.html`에서 확인합니다.

## 공통 응답 형식

BE API는 기본적으로 `ApiResponse<T>` 형식을 사용합니다.

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

실패 응답은 아래 형식을 사용합니다.

```json
{
  "success": false,
  "data": null,
  "message": "요청 값이 올바르지 않습니다."
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `success` | boolean | 요청 성공 여부 |
| `data` | object/null | 성공 응답 데이터 |
| `message` | string/null | 오류 메시지 또는 안내 메시지 |

현재 공통 응답에는 `errorCode` 필드를 사용하지 않습니다. FE는 우선 HTTP status와 `message`를 기준으로 오류 화면을 분기합니다.

요청/응답 필드명은 Java DTO 기준 camelCase를 사용합니다. DB 컬럼명은 ERD 기준 snake_case를 사용합니다.

## 오류 처리 기준

| HTTP status | 대표 상황 | 응답 기준 |
| --- | --- | --- |
| 400 | 잘못된 요청 값, validation 실패, 이미지 누락, 이미지 용량 초과 | `success=false`, 요청 오류 메시지 |
| 401 | 로그인하지 않은 사용자, 유효하지 않은 토큰 | `success=false`, 인증 오류 메시지 |
| 403 | 인증 사용자와 요청 대상 사용자 불일치, 접근 권한 없음 | `success=false`, 접근 권한 메시지 |
| 404 | 리소스 없음 | `success=false`, 리소스 없음 메시지 |
| 409 | 이미 존재하는 데이터, 상태 충돌 | `success=false`, 충돌 메시지 |
| 502 | 외부 API 호출 실패 | `success=false`, 외부 서비스 오류 메시지 |
| 500 | 서버 내부 오류 | `success=false`, 서버 오류 메시지 |

## 인증 기준

- OAuth 로그인 성공 후 JWT Access Token을 발급합니다.
- Access Token은 `access_token` HttpOnly 쿠키로 전달됩니다.
- Refresh Token은 `refresh_token` HttpOnly 쿠키로 전달되며 `/api/v1/auth` 경로에서만 전송됩니다.
- Access Token이 만료(401)되면 `POST /api/v1/auth/refresh`를 호출해 재발급합니다.
- 로그아웃 시 `POST /api/v1/auth/logout`을 호출해 서버에서 Refresh Token을 삭제합니다.
- 사용자별 리소스는 JWT의 사용자 ID와 path의 `userId`가 일치해야 합니다.

## 이미지 업로드 기준

- 옷 사진과 구매내역 캡처는 multipart form-data로 업로드합니다.
- 요청 part 이름은 `file`입니다.
- 이미지 파일 크기는 10MB 이하입니다.
- 이미지 저장은 AWS S3 기준으로 관리합니다.

## 엔드포인트 인덱스

### 인증

| Method | Path                               | 설명              |
|--------|------------------------------------|-----------------|
| GET    | `/oauth2/authorization/{provider}` | OAuth 로그인 시작    |
| POST   | `/api/v1/auth/refresh`             | Access Token 재발급 |
| POST   | `/api/v1/auth/logout`              | 로그아웃            |


### 카탈로그

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/categories` | 카테고리, 타입, 색상, 스타일 카탈로그 조회 |
| GET | `/api/v1/categories/guide` | 카테고리 사용 가이드 조회 |
| GET | `/api/v1/categories/ai-guide` | AI 분석용 카탈로그 가이드 조회 |
| GET | `/api/v1/categories/external-sources` | 외부 출처 목록 조회 |

### 옷장

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/wardrobes/users/{userId}` | 사용자 옷장 조회 |
| POST | `/api/v1/wardrobes/users/{userId}` | 사용자 옷장 생성 |
| GET | `/api/v1/wardrobes/users/{userId}/statistics` | 사용자 옷장 통계 조회 |

### 옷

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/clothes/registration-methods` | 옷 등록 방식 목록 조회 |
| GET | `/api/v1/users/{userId}/clothes` | 사용자 옷 목록 조회 |
| GET | `/api/v1/users/{userId}/clothes/favorites` | 사용자 즐겨찾기 옷 조회 |
| GET | `/api/v1/clothes/{clothesId}` | 옷 상세 조회 |
| POST | `/api/v1/users/{userId}/clothes` | 옷 등록 |
| PATCH | `/api/v1/clothes/{clothesId}/favorite` | 옷 즐겨찾기 변경 |
| PATCH | `/api/v1/clothes/{clothesId}` | 옷 정보 수정 |
| DELETE | `/api/v1/clothes/{clothesId}` | 사용자 옷장에서 옷 연결 삭제 |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 옷장 기반 어울리는 옷 추천 조회 (`limitPerCategory` query, 아래 [옷장 기반 어울리는 옷 추천](#옷장-기반-어울리는-옷-추천-get-recommendations) 참고) |

#### 옷 등록/저장 공통 분류 필드

아래 필드는 보유 옷 등록(`POST /api/v1/users/{userId}/clothes`), 사진 저장, 구매내역 저장, 미보유 저장 요청에 공통으로 포함됩니다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `category` | Y | 대분류 code (`TOP`, `BOTTOM`, `OUTER`, `SHOES`) |
| `itemType` | Y | 소분류 code. 선택한 `category` 하위 값 |
| `season` | N | 옷 자체의 대상 계절 code (`SPRING`, `SUMMER`, `FALL`, `WINTER`, `ALL_SEASON`). 생략 시 `ALL_SEASON`으로 저장하며, 최종 저장 후 변경하지 않음 |
| `gender` | Y | 옷 대상 성별 code (`MALE`, `FEMALE`, `UNISEX`). 사용자 화면 표시 대상 아님 |
| `primaryColor` | Y | 대표 색상 code |
| `secondaryColors` | N | 보조 색상 code 배열 |
| `styles` | Y | 스타일 code 배열 (최소 1개) |

허용 code 목록은 [카탈로그 사용 가이드](../domain/catalog.md)를 따릅니다. 저장 요청 시 validation이 적용됩니다. `gender`는 사용자에게 노출하지 않고 옷 분류/추천과 저장 요청에 사용하는 내부 code입니다.

#### 옷 수정 기준

옷 수정(`PATCH /api/v1/clothes/{clothesId}`)은 생성된 공통 옷의 계절을 변경하는 용도로 사용하지 않습니다. 생성 후 `season`을 변경해야 하는 상황은 공식 기준과 충돌하므로 담당자 확인 후 별도 기준 변경으로 처리합니다.

#### 옷 조회 응답 (`ClothesResponse`)

옷 목록/상세/저장 성공 응답에는 분류 필드와 함께 `season`, `gender`가 포함됩니다. `season`은 `SPRING`, `SUMMER`, `FALL`, `WINTER`, `ALL_SEASON` code이고, `gender`는 `MALE`, `FEMALE`, `UNISEX` enum code입니다. FE는 `gender`를 사용자 화면에 표시하지 않고 내부 분류/추천 처리 기준으로만 사용합니다.

```json
{
  "success": true,
  "data": {
    "clothesId": 1,
    "wardrobeClothesId": 10,
    "name": "화이트 반팔 티셔츠",
    "category": "TOP",
    "itemType": "SHORT_SLEEVE",
    "season": "SUMMER",
    "gender": "UNISEX",
    "primaryColor": "WHITE",
    "secondaryColors": [],
    "styles": [
      { "styleCode": "CASUAL", "styleRole": "PRIMARY" }
    ],
    "ownershipStatus": "OWNED",
    "clothesInfoSource": "PHOTO"
  },
  "message": null
}
```

> **Note**: 응답 예시는 주요 필드만 발췌했습니다. 실제 응답에는 옷장/외부 연동 필드가 추가로 포함됩니다.

### 미보유 옷

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 목록 조회 |
| GET | `/api/users/{userId}/wishlist-clothes/favorites` | 즐겨찾기 미보유 옷 조회 |
| POST | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 저장 (신규 CLOTHES 생성) |
| POST | `/api/users/{userId}/wishlist-clothes/{clothesId}` | 기존 `EXTERNAL_SHOPPING` CLOTHES를 위시리스트에 연결 (추천 상품 저장) |
| PATCH | `/api/v1/clothes/{clothesId}/convert-to-owned` | 미보유 옷을 보유 옷으로 전환 (**공식 경로**) |
| PATCH | `/api/clothes/{clothesId}/convert-to-owned` | 위와 동일 (legacy 호환. 신규 FE는 `/api/v1` 사용) |

### 사진 기반 옷 등록

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/users/{userId}/clothes/photos` | 옷 사진 업로드 |
| POST | `/api/v1/users/{userId}/clothes/photos/{photoId}/analyze` | 업로드 사진 AI 분석 |
| GET | `/api/v1/users/{userId}/clothes/photos/{photoId}/draft` | 사진 분석 기반 등록 초안 조회 |
| POST | `/api/v1/users/{userId}/clothes/photos/{photoId}/save` | 사진 기반 옷 저장 |

### 구매내역 기반 옷 등록

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures` | 구매내역 캡처 업로드 |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/analyze` | 구매내역 캡처 AI 분석 |
| GET | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/draft` | 구매내역 기반 등록 초안 조회 |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/save` | 구매내역 기반 옷 저장 (`itemIndex` 선택, 생략 시 0) |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/items/{itemIndex}/skip` | 구매내역 캡처 상품 건너뛰기 |

단일 상품 draft/analyze 응답은 `name`, `category`, `itemType`, `season`, `gender` 등 flat 필드와 `items[0]` 모두에 분류 값을 포함합니다. 복수 상품 시 flat 분류 필드는 `null`이며 `items[]`(`itemIndex`, `season`, `gender`, `imageUrl`, `status`), `pendingItemCount`, `captureCompleted`를 사용합니다. `season`은 옷 등록 시 확정되는 공통 옷 정보이고, `gender`는 사용자에게 노출하지 않는 내부 code입니다.

#### 구매내역 저장 요청 (`PurchaseCaptureSaveRequest`)

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `name`, `brandName`, `productCode`, `category`, `itemType`, `gender`, `primaryColor`, `styles`, `externalSource`, `size`, `favorite`, `isVerified` | Y | 옷 공통·옷장 정보 |
| `secondaryColors`, `season` | N | 보조 색상, 옷 계절 code. `season` 생략 시 `ALL_SEASON` |
| `itemIndex` | N | 생략 시 0. 복수 상품일 때 저장 대상 인덱스 |
| `imageUrl` | N | 상품별 이미지 URL. 생략 시 draft `items[].imageUrl` 또는 캡처 `previewUrl`로 fallback |

#### 구매내역 상품 상태 (`PurchaseCaptureItemStatus`)

`PENDING`, `SAVED`, `SKIPPED`

### 외부 상품

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/naver/search` | 네이버쇼핑 상품 검색 |
| POST | `/api/v1/external/clothes/naver` | 네이버쇼핑 상품을 공통 옷 정보로 저장 |

### 추천

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/similar-products` | 유사 상품 추천 조회 |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 옷장 기반 어울리는 옷 추천 조회 (`limitPerCategory` query, 아래 [옷장 기반 어울리는 옷 추천](#옷장-기반-어울리는-옷-추천-get-recommendations) 참고) |
| POST | `/api/v1/users/{userId}/recommendations/feedback` | 추천 상품 피드백 제출 (저장/싫어요/추천 제외) |
| GET | `/api/v1/recommendations/{wardrobeId}?currentTemp={temp}` | 취향 기반 상품 추천 |
| GET | `/api/v1/ootd/{wardrobeId}?currentTemp={temp}` | 내 옷장 기반 OOTD 추천 |
| GET | `/api/v1/users/{userId}/recommendations/ai-md/personas` | 사용자 성별에 맞는 AI MD 목록 조회 |
| GET | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/products` | 선택한 AI MD 기준 외부 상품 추천 |
| POST | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits` | 선택한 AI MD 기준 코디 후보 추천 |
| POST | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits/save` | 선택한 AI MD 코디 후보 저장 |

#### 옷장 기반 어울리는 옷 추천 (`GET .../recommendations`)

옷장에 등록한 보유 옷 1벌을 기준으로 **같은 카테고리를 제외한 `EXTERNAL_SHOPPING` 공용 DB 후보**를 점수화해 카테고리별로 반환합니다. `PHOTO`·`PURCHASE_HISTORY` 등 다른 사용자 개인 등록 마스터는 후보에 포함하지 않습니다. 옷장에 이미 등록된 `clothesId`(본인 보유)는 후보에서 제외됩니다.

| Query | 필수 | 설명 |
| --- | --- | --- |
| `limitPerCategory` | N | 카테고리당 최대 추천 수. 기본 `5`, 허용 범위 `1`~`50` |

JWT 사용자와 path의 `userId`가 일치해야 합니다. `clothesId`는 해당 사용자의 보유 옷(`OWNED`)이어야 합니다.

**응답 (`ClothesRecommendationResponse`)**

| 필드 | 설명 |
| --- | --- |
| `anchor` | 기준 옷 요약 (`clothesId`, `name`, `imageUrl`, `userImageUrl`, `category`, `itemType`, `primaryColor`, `primaryColorDisplay`) |
| `recommendations` | 카테고리 code → 추천 목록. key 예: `TOP`, `BOTTOM`, `OUTER`, `SHOES` (기준 옷과 **동일 카테고리 key는 포함되지 않음**) |

**추천 항목 (`RecommendedItem`)**

| 필드 | 설명 |
| --- | --- |
| `clothesId` | 외부 `CLOTHES` ID |
| `wardrobeClothesId` | 옷장 연결 ID. 외부 후보는 보통 `null` |
| `name`, `imageUrl`, `userImageUrl` | 상품명·이미지 |
| `brandName` | 브랜드명 (`UNKNOWN` 가능) |
| `category`, `itemType` | 대·소분류 code |
| `primaryColor`, `primaryColorDisplay`, `secondaryColors` | 색상 |
| `styleCodes` | 스타일 code 배열 |
| `season` | `CLOTHES.season` code. 옷 등록 시 확정하며 `WARDROBE_CLOTHES`에는 저장하지 않음 |
| `gender` | 옷 대상 성별 code (`MALE`, `FEMALE`, `UNISEX`). 사용자 화면 표시 대상 아님 |
| `compatibilityScore` | 어울림 점수 (0~100, 내림차순 정렬) |

```json
{
  "success": true,
  "data": {
    "anchor": {
      "clothesId": 101,
      "name": "화이트 반팔 티셔츠",
      "imageUrl": "https://...",
      "userImageUrl": "https://...",
      "category": "TOP",
      "itemType": "SHORT_SLEEVE",
      "primaryColor": "WHITE",
      "primaryColorDisplay": {
        "code": "WHITE",
        "label": "화이트",
        "hex": "#FFFFFF"
      }
    },
    "recommendations": {
      "BOTTOM": [
        {
          "clothesId": 502,
          "wardrobeClothesId": null,
          "name": "와이드 슬랙스",
          "imageUrl": "https://...",
          "userImageUrl": null,
          "category": "BOTTOM",
          "itemType": "SLACKS",
          "primaryColor": "BLACK",
          "primaryColorDisplay": {
            "code": "BLACK",
            "label": "블랙",
            "hex": "#000000"
          },
          "secondaryColors": [],
          "styleCodes": ["MINIMAL"],
          "season": "ALL_SEASON",
          "compatibilityScore": 87,
          "gender": "UNISEX"
        }
      ],
      "OUTER": [],
      "SHOES": []
    }
  },
  "message": null
}
```

> **Note**: 점수는 색상(35%)·스타일(30%)·itemType(20%)·시즌(15%) 가중 합산입니다. 동점 후보는 BE에서 랜덤 순서가 될 수 있습니다. FE는 사용자 프로필 성별에 맞지 않는 `gender` 후보를 내부적으로 제외할 수 있지만, 해당 값을 사용자 화면에 표시하지 않습니다.

#### 취향 기반 상품 추천 응답 (RecommendResponse)

```json
{
  "success": true,
  "data": [
    {
      "title": "상품명",
      "link": "https://...",
      "imageUrl": "https://...",
      "price": "0",
      "score": "0.95",
      "reason": "Style Match: 0.9, Weather Match: 1.0"
    }
  ],
  "message": null
}
```

> **Note**: 현재 `price`는 placeholder("0")이며, `score`는 0.0~1.0 사이의 문자열, `reason`은 기술적 매칭 결과입니다. 상세 내용은 [implementation-gaps.md](../backend/implementation-gaps.md)를 참고하세요.

#### OOTD 추천 응답 (OotdResponse)

```json
{
  "success": true,
  "data": {
    "combinations": [
      {
        "top": {
          "clothesId": 101,
          "wardrobeClothesId": 1,
          "name": "상의 이름",
          "brand": "브랜드",
          "color": "BLACK",
          "imageUrl": "https://...",
          "externalProductUrl": "https://...",
          "category": "TOP",
          "itemType": "SHORT_SLEEVE",
          "favorite": true
        },
        "bottom": {
          "clothesId": 102,
          "wardrobeClothesId": 2,
          "name": "하의 이름",
          "brand": "브랜드",
          "color": "BLUE",
          "imageUrl": "https://...",
          "externalProductUrl": "https://...",
          "category": "BOTTOM",
          "itemType": "DENIM",
          "favorite": false
        },
        "outer": null,
        "totalScore": 2.5
      }
    ],
    "weatherLabel": "오늘 20°C — 얇은 셔츠·면바지 추천",
    "currentTemp": 20.0
  },
  "message": null
}
```

> **Note**: `outer` 필드는 기온에 따라 외투가 필요 없는 경우(HOT, WARM) `null`로 반환됩니다.

#### 추천 피드백 제출

**POST** `/api/v1/users/{userId}/recommendations/feedback`

- **요청 Body**
```json
{
  "clothesId": 123,
  "feedbackType": "SAVED"
}
```

- **피드백 타입 (`feedbackType`)**
    - `SAVED`: 저장하기 (긍정 - 가중치 미반영)
    - `DISLIKE`: 싫어요 (부정 - 가중치 마이너스 반영)
    - `EXCLUDE`: 추천 제외 (부정 + 후보 제외 - 가중치 마이너스 반영)

- **응답 (성공)**
```json
{
  "success": true,
  "data": null,
  "message": null
}
```
#### AI MD 목록 응답 (AiMdPersonaResponse)

```json
{
  "success": true,
  "data": [
    {
      "id": "taesik",
      "name": "태식이",
      "gender": "MALE",
      "styleCodes": ["STREET", "CASUAL", "GORPCORE", "CHIC"],
      "styleNames": ["스트릿", "캐주얼", "고프코어", "시크"],
      "speechStyle": "장난스럽고 친구같은 반말",
      "description": "힘 빼고 멋내는 스트릿/캐주얼 코디를 잘 잡는 남자 MD"
    }
  ],
  "message": null
}
```

> **Note**: AI MD 목록은 JWT 사용자와 path의 `userId`가 일치해야 조회할 수 있으며, 사용자 성별에 맞는 MD만 반환합니다. 남성 사용자는 `taesik`, `junsik`, 여성 사용자는 `sesoon`, `gahyun`, `seongmi`를 선택할 수 있습니다.

#### AI MD 상품 추천 응답 (AiMdProductRecommendationResponse)

```json
{
  "success": true,
  "data": {
    "md": {
      "id": "taesik",
      "name": "태식이",
      "gender": "MALE",
      "styleCodes": ["STREET", "CASUAL", "GORPCORE", "CHIC"],
      "styleNames": ["스트릿", "캐주얼", "고프코어", "시크"],
      "speechStyle": "장난스럽고 친구같은 반말",
      "description": "힘 빼고 멋내는 스트릿/캐주얼 코디를 잘 잡는 남자 MD"
    },
    "query": "남성 블랙 스트릿 코디 아이템",
    "products": [
      {
        "product": {
          "title": "상품명",
          "link": "https://...",
          "image": "https://...",
          "lowestPrice": 59000,
          "highestPrice": null,
          "mallName": "쇼핑몰명",
          "productId": "123",
          "productType": "1",
          "brand": "브랜드",
          "maker": "제조사",
          "category1": "패션의류",
          "category2": "남성의류",
          "category3": "티셔츠",
          "category4": ""
        },
        "reason": "MD 말투가 반영된 추천 이유"
      }
    ]
  },
  "message": null
}
```

> **Note**: 상품 추천은 사용자 보유 옷과 MD 스타일을 기반으로 네이버쇼핑 후보를 조회한 뒤 Gemini가 최대 10개 상품과 추천 이유를 선별합니다. 이 단계에서는 저장하지 않습니다. 사용자가 상품 카드에서 저장 버튼을 누르면 `POST /api/users/{userId}/wishlist-clothes`로 미보유 옷을 저장합니다. 유사 상품 추천 결과도 같은 저장 API를 사용합니다. 보유 옷이 없으면 `409` 응답과 함께 등록 안내 메시지를 반환합니다.

#### AI MD 코디 추천 응답 (AiMdOutfitRecommendationResponse)

```json
{
  "success": true,
  "data": {
    "md": {
      "id": "taesik",
      "name": "태식이",
      "gender": "MALE",
      "styleCodes": ["STREET", "CASUAL", "GORPCORE", "CHIC"],
      "styleNames": ["스트릿", "캐주얼", "고프코어", "시크"],
      "speechStyle": "장난스럽고 친구같은 반말",
      "description": "힘 빼고 멋내는 스트릿/캐주얼 코디를 잘 잡는 남자 MD"
    },
    "outfits": [
      {
        "title": "코디 제목",
        "description": "코디 설명",
        "situation": "DAILY",
        "season": "ALL_SEASON",
        "reason": "MD 말투가 반영된 코디 추천 이유",
        "stylingTip": "스타일링 팁",
        "ownedItems": [
          {
            "wardrobeClothesId": 1,
            "name": "보유 상의",
            "category": "TOP"
          },
          {
            "wardrobeClothesId": 2,
            "name": "보유 하의",
            "category": "BOTTOM"
          },
          {
            "wardrobeClothesId": 3,
            "name": "보유 신발",
            "category": "SHOES"
          }
        ],
        "externalProducts": []
      }
    ]
  },
  "message": null
}
```

> **Note**: 코디 추천은 Gemini가 4개 코디 후보를 구성하지만 이 단계에서는 `OUTFITS`, `OUTFIT_ITEMS`, 외부 `Clothes`를 저장하지 않습니다. 각 후보는 사용자 보유 옷을 최소 1개 포함해야 하며, 보유 옷과 외부 상품을 합친 전체 구성에 `TOP`, `BOTTOM`, `SHOES`가 각각 최소 1개 있어야 합니다. `OUTER`는 선택 사항입니다. 외부 상품은 필수가 아니므로 보유 옷만으로 필수 세 카테고리가 완성된 후보도 유효합니다. 프론트는 사용자가 선택한 후보만 저장 API로 전달합니다.

#### AI MD 추천 코디 저장 요청/응답

```json
{
  "title": "코디 제목",
  "description": "코디 설명",
  "situation": "DAILY",
  "season": "ALL_SEASON",
  "reason": "MD 말투가 반영된 코디 추천 이유",
  "stylingTip": "스타일링 팁",
  "wardrobeClothesIds": [1, 2, 3],
  "externalProducts": []
}
```

위 예시의 `wardrobeClothesIds`는 각각 `TOP`, `BOTTOM`, `SHOES`인 보유 옷을 의미합니다. 저장 요청도 추천 후보와 동일하게 사용자 보유 옷을 최소 1개 포함하고, `wardrobeClothesIds`와 `externalProducts`를 합쳐 `TOP`, `BOTTOM`, `SHOES`가 모두 구성되어야 합니다. 외부 상품 없이 보유 옷만으로 완성할 수 있으며, 필수 카테고리가 누락되면 `400 Bad Request`를 반환합니다.

저장 성공 시에는 선택된 코디 1개가 `OUTFITS`, `OUTFIT_ITEMS`에 저장되고, 응답은 저장된 `outfit`과 구성 옷 목록을 포함합니다. 저장된 구성 옷은 코디북 조회 응답의 `outfits[].items`에서도 다시 조회할 수 있습니다.

### 날씨

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/weather` | 날씨 정보 조회 |

### 코디북/코디

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/outfit-books` | 코디북 생성 |
| GET | `/api/v1/outfit-books` | 코디북 목록 조회 |
| GET | `/api/v1/outfit-books/{bookId}` | 코디북 상세 조회 |
| POST | `/api/v1/outfit-books/{bookId}/outfits` | 코디 저장 |
| PUT | `/api/v1/outfit-books/{bookId}/outfits/{outfitId}` | 코디 수정 |
| DELETE | `/api/v1/outfit-books/{bookId}/outfits/{outfitId}` | 코디 삭제 |

#### 코디 저장/수정 요청 (`OutfitCreateRequest`, `OutfitUpdateRequest`)

```json
{
  "title": "코디 제목",
  "description": "코디 설명",
  "thumbnailUrl": "https://...",
  "situation": "DAILY",
  "season": "ALL_SEASON",
  "favorite": false,
  "items": [
    {
      "clothesId": 1,
      "itemRole": "TOP",
      "layerOrder": 0
    }
  ]
}
```

> **Note**: 수정(`PUT`) 요청에서 `items`를 생략하면 기존 구성 아이템이 유지됩니다.
> - `items` 생략(null): 기존 구성 아이템 유지, 메타데이터만 수정
> - `items: []` (빈 배열): 기존 구성 아이템 전체 삭제
> - `items: [...]` (목록): 기존 구성 전체 교체

#### 코디북 조회 응답 (OutfitBookResponse)

```json
{
  "success": true,
  "data": {
    "outfitBookId": 1,
    "userId": 1,
    "outfitCount": 1,
    "outfits": [
      {
        "outfitId": 1,
        "outfitBookId": 1,
        "title": "코디 제목",
        "description": "코디 설명",
        "thumbnailUrl": "https://...",
        "situation": "DAILY",
        "season": "ALL_SEASON",
        "favorite": false,
        "items": [
          {
            "outfitItemId": 1,
            "itemRole": "TOP",
            "layerOrder": 0,
            "clothes": {
              "clothesId": 1,
              "wardrobeClothesId": 1,
              "name": "보유 옷 또는 외부 상품명",
              "brandName": "브랜드명",
              "clothesInfoSource": "PHOTO"
            }
          }
        ],
        "createdAt": "2026-06-08T12:00:00",
        "updatedAt": "2026-06-08T12:00:00"
      }
    ],
    "createdAt": null,
    "updatedAt": null
  },
  "message": null
}
```

> **Note**: `items[].clothes`가 사용자 옷장에 연결된 보유 옷이면 `wardrobeClothesId`, `wardrobeId`, `userId`, `size` 등 사용자 옷장 연결 정보가 함께 채워집니다. `season`은 `CLOTHES.season` 기준의 공통 옷 정보입니다. AI MD가 섞은 외부 상품처럼 옷장 연결이 없는 옷은 옷장 연결 필드가 `null`입니다.

### 이미지

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/images/clothes/{userId}/{filename}` | 옷 이미지 조회 |
| GET | `/api/v1/images/purchase-captures/{userId}/{filename}` | 구매내역 캡처 이미지 조회 |

## API 변경 규칙

- API 경로, 요청 필드, 응답 필드가 바뀌면 이 문서를 같은 PR에서 수정합니다.
- FE가 사용하는 API 변경은 FE 담당자와 동기화 이슈 또는 PR에서 확인합니다.
- 공통 응답 형식에 필드가 추가되면 성공/실패 응답 예시를 함께 수정합니다.
