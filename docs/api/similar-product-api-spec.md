---
doc_type: be_similar_product_api_spec
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-16
---

# 유사상품 추천 API 명세서

이 문서는 FE에서 사용자의 옷장 등록 옷 선택 모달, 유사상품 추천 목록, 외부 구매 링크 및 상품 저장 화면을 구현할 때 사용하는 연동 명세입니다.
공통 정책의 원본은 [api-contract.md](./api-contract.md)이며, 공통 옷 응답 타입은 [AI MD API 명세서](./ai-md-api-spec.md)와 동일합니다.

## 1. 기능 개요

사용자가 자신의 옷장에 등록된 옷 중 하나를 선택하면 BE가 해당 옷의 속성으로 네이버쇼핑 검색어를 구성하고 유사한 상품을 최대 50개 반환합니다.

```text
옷장 등록 옷 목록 조회
→ 기준 옷 선택 모달 표시
→ 사용자가 옷 1개 선택
→ 유사상품 추천 API 호출
→ 기준 옷과 추천 상품 카드 표시
→ 외부 구매 페이지 이동 또는 미보유 옷 저장
```

추천 조회만으로 상품이 저장되지는 않습니다.

## 2. 공통 기준

### Base URL

```text
{BE_BASE_URL}
```

### 인증

모든 API 요청에 로그인 사용자의 JWT가 필요합니다.

```http
Authorization: Bearer {accessToken}
```

JWT 사용자 ID와 path의 `userId`가 다르면 `403 Forbidden`을 반환합니다.

### 공통 응답

```ts
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
}
```

## 3. 기준 옷 목록 조회

기준 옷 선택 모달을 열 때 사용합니다.

```http
GET /api/v1/users/{userId}/clothes
```

### 응답

```ts
type SimilarProductBaseClothesResponse = ApiResponse<Clothes[]>;
```

`Clothes`의 전체 타입은 [AI MD API 명세서의 Clothes 타입](./ai-md-api-spec.md#주요-응답-타입)을 참고합니다.

모달에서 사용하는 주요 필드는 다음과 같습니다.

```ts
export interface SimilarProductBaseClothesOption {
  clothesId: number;
  wardrobeClothesId: number;
  name: string;
  brandName: string;
  imageUrl: string;
  category: "TOP" | "BOTTOM" | "OUTER" | "SHOES";
  itemType: string;
  gender: "MALE" | "FEMALE" | "UNISEX";
  primaryColor: string | null;
  styles: StyleTag[];
  ownershipStatus: "OWNED" | "WISHLIST";
}
```

### FE 처리 기준

- 유사상품 추천의 기준 옷은 사용자의 활성 옷장 항목이면 됩니다. `ownershipStatus`가 `OWNED` 또는 `WISHLIST`인 옷을 모두 선택지로 사용할 수 있습니다.
- `WISHLIST`는 아직 실제 보유하지 않은 관심 상품이지만, 비슷한 상품을 찾는 기준 옷으로는 유효합니다.
- 추천 요청에는 `wardrobeClothesId`가 아니라 `clothesId`를 사용합니다.
- 목록이 비어 있으면 옷 등록 화면으로 유도합니다.
- 옷 이미지, 이름, 브랜드, 카테고리 정도를 선택 카드에 표시하는 것을 권장합니다.

## 4. 유사상품 추천 조회

```http
GET /api/v1/users/{userId}/clothes/{clothesId}/similar-products
```

### Path parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `userId` | `number` | Y | 로그인 사용자 ID |
| `clothesId` | `number` | Y | 기준 옷 목록 응답의 `clothesId`. 해당 사용자의 활성 `OWNED` 또는 `WISHLIST` 옷장 항목이어야 함 |

Request body와 query parameter는 없습니다.

### 호출 예시

```http
GET /api/v1/users/1/clothes/15/similar-products
Authorization: Bearer {accessToken}
```

### 응답 타입

```ts
export interface NaverShoppingProduct {
  title: string;
  link: string;
  image: string;
  lowestPrice: number | null;
  highestPrice: number | null;
  mallName: string;
  productId: string;
  productType: string;
  brand: string;
  maker: string;
  category1: string;
  category2: string;
  category3: string;
  category4: string;
  clothesId: number | null;
  candidateSource: "NAVER" | "INTERNAL";
}

export interface SimilarProductRecommendation {
  baseClothes: Clothes;
  query: string;
  products: NaverShoppingProduct[];
}

export type SimilarProductRecommendationResponse =
  ApiResponse<SimilarProductRecommendation>;
```

### 응답 예시

```json
{
  "success": true,
  "data": {
    "baseClothes": {
      "clothesId": 15,
      "wardrobeClothesId": 21,
      "wardrobeId": 1,
      "userId": 1,
      "name": "Multi Stripe Long Sleeve",
      "brandName": "ourselves",
      "productCode": "TEST-001",
      "imageUrl": "https://example.com/stripe-long-sleeve.jpg",
      "category": "TOP",
      "itemType": "LONG_SLEEVE",
      "gender": "UNISEX",
      "primaryColor": "BLACK",
      "styles": [
        {
          "styleId": 2,
          "code": "STREET",
          "name": "스트릿",
          "styleRole": "PRIMARY",
          "sortOrder": 1
        }
      ],
      "ownershipStatus": "OWNED"
    },
    "query": "남성 블랙 스트라이프 스트릿 롱슬리브",
    "products": [
      {
        "title": "블랙 스트라이프 오버핏 롱슬리브",
        "link": "https://search.shopping.naver.com/...",
        "image": "https://shopping-phinf.pstatic.net/...",
        "lowestPrice": 32900,
        "highestPrice": null,
        "mallName": "네이버",
        "productId": "123456789",
        "productType": "1",
        "brand": "다른 브랜드",
        "maker": "",
        "category1": "패션의류",
        "category2": "남성의류",
        "category3": "티셔츠",
        "category4": "",
        "clothesId": null,
        "candidateSource": "NAVER"
      },
      {
        "title": "공용 DB 스트라이프 롱슬리브",
        "link": "",
        "image": "https://cdn.example.com/clothes/502.jpg",
        "lowestPrice": null,
        "highestPrice": null,
        "mallName": "NAVER",
        "productId": "CLOTHES_502",
        "productType": "INTERNAL",
        "brand": "브랜드",
        "maker": "브랜드",
        "category1": "패션의류",
        "category2": "남성의류",
        "category3": "TOP",
        "category4": "롱슬리브",
        "clothesId": 502,
        "candidateSource": "INTERNAL"
      }
    ]
  },
  "message": null
}
```

실제 `baseClothes`에는 `Clothes` 타입의 전체 필드가 포함됩니다. 위 예시는 화면 구현에 필요한 주요 필드만 표시했습니다.

`products`에는 네이버쇼핑 실시간 후보와 내부 `CLOTHES` 공용 후보가 함께 포함됩니다.

- `candidateSource="NAVER"`: 네이버쇼핑 API에서 온 후보입니다. `clothesId`는 `null`일 수 있습니다. 위시리스트 저장은 기존 네이버 상품 저장 플로우를 사용하고, 저장 전 추천 피드백은 보낼 수 없습니다.
- `candidateSource="INTERNAL"`: `CLOTHES.clothes_info_source=EXTERNAL_SHOPPING`인 내부 공용 후보입니다. `clothesId`가 있으므로 `POST /api/users/{userId}/wishlist-clothes/{clothesId}`와 `POST /api/v1/users/{userId}/recommendations/feedback`에 바로 사용할 수 있습니다.
- 내부 후보는 가격 정보가 없어 `lowestPrice`/`highestPrice`가 `null`일 수 있고, 구매 링크가 없는 경우 `link=""`일 수 있습니다. FE는 가격 미표시와 구매 버튼 비활성화를 처리해야 합니다.
- 내부 후보도 기준 사용자의 성별과 `UNISEX` 상품만 포함합니다. 사용자 성별이 `OTHER`이거나 없으면 내부 후보 성별을 제한하지 않습니다.

## 5. 추천 기준

BE는 동일 브랜드나 동일 상품 재검색을 줄이기 위해 브랜드명과 원본 상품명 전체를 검색어에서 제외합니다.

검색어에는 다음 속성을 사용합니다.

| 순서 | 속성 | 예시 |
| --- | --- | --- |
| 1 | 사용자 성별 | `남성`, `여성` |
| 2 | 기준 옷 대표 색상 | `블랙` |
| 3 | 상품명에서 추출한 디자인·핏 | `스트라이프`, `와이드`, `오버핏`, `크롭` |
| 4 | 대표 스타일 | `스트릿`, `미니멀`, `캐주얼` |
| 5 | 아이템 타입 | `롱슬리브`, `슬랙스`, `스니커즈` |

예시:

```text
브랜드: ourselves
상품명: Multi Stripe Long Sleeve
색상: BLACK
사용자 성별: MALE
대표 스타일: STREET
아이템 타입: LONG_SLEEVE

실제 검색어: 남성 블랙 스트라이프 스트릿 롱슬리브
```

현재 디자인 키워드 추출은 AI 이미지 유사도 분석이 아니라 규칙 기반입니다. 따라서 색상, 패턴, 핏, 스타일이 비슷한 다른 브랜드 상품을 찾는 기능이며 완전한 시각적 유사도 검색은 아닙니다.

`query`는 검색 품질 확인과 디버깅을 위한 값입니다. 일반 사용자 화면에 반드시 노출할 필요는 없습니다.

## 6. 결과 목록 처리

- 유사상품 추천 결과는 최대 50개입니다.
- 별도 pagination 또는 더보기 parameter는 현재 제공하지 않습니다.
- 결과가 없으면 `200 OK`와 `products: []`가 반환될 수 있습니다.
- 상품 카드에는 이미지, 상품명, 브랜드 또는 쇼핑몰을 표시합니다.
- `lowestPrice`가 `number`이면 최저가를 통화 형식으로 표시하고, `null`이면 가격 영역을 숨기거나 "가격 정보 없음"으로 처리합니다.
- 구매 버튼은 `product.link`가 비어 있지 않을 때만 노출하거나 활성화합니다.
- 외부 링크는 새 창 또는 인앱 브라우저로 여는 것을 권장합니다.
- `candidateSource="INTERNAL"`이고 `clothesId`가 있으면 위시리스트 연결, 추천 싫어요, 추천 제외 액션을 바로 수행할 수 있습니다.

```ts
const formattedPrice =
  product.lowestPrice == null
    ? null
    : new Intl.NumberFormat("ko-KR").format(product.lowestPrice);

const canPurchase = product.link.trim().length > 0;
const canSubmitFeedback = product.candidateSource === "INTERNAL" && product.clothesId != null;
```

## 7. 추천 상품 선택 및 저장

추천 상품은 자유롭게 복수 선택할 수 있습니다. 다만 유사상품 추천 API는 조회만 담당하며 저장 API는 별도입니다.

### 내부 후보 저장

`candidateSource="INTERNAL"`이고 `clothesId`가 있는 후보는 이미 공용 `CLOTHES`에 존재하므로 신규 생성 API를 호출하지 않습니다. 기존 옷 연결 API를 사용합니다.

```http
POST /api/users/{userId}/wishlist-clothes/{clothesId}
```

이 후보는 추천 피드백 API에도 같은 `clothesId`를 사용할 수 있습니다.

```http
POST /api/v1/users/{userId}/recommendations/feedback
Content-Type: application/json

{
  "clothesId": 502,
  "feedbackType": "DISLIKE"
}
```

### 네이버 후보 저장

`candidateSource="NAVER"`이고 `clothesId=null`인 후보는 아직 공용 `CLOTHES`가 없을 수 있으므로 기존 네이버 상품 신규 저장 플로우를 사용합니다.

```http
POST /api/users/{userId}/wishlist-clothes
Content-Type: application/json
```

### 저장 요청 타입

```ts
export interface WishlistClothesCreateRequest {
  name: string;
  brandName: string;
  productCode: string;
  imageUrl: string;
  category: "TOP" | "BOTTOM" | "OUTER" | "SHOES";
  itemType: string;
  gender: "MALE" | "FEMALE" | "UNISEX";
  primaryColor: string;
  secondaryColors: string[];
  styles: string[];
  size: string;
  season?: string | null;
  externalSource: string;
  externalProductId: string;
  externalProductUrl: string;
}
```

### 현재 연동 제약

네이버쇼핑 추천 응답에는 아래 저장 필수값이 포함되지 않습니다.

- 서비스 카탈로그 기준 `category`
- `itemType`
- `gender`
- `primaryColor`
- `styles`
- `size`
- `season`

따라서 FE가 네이버 응답만으로 임의의 저장 요청을 만들면 안 됩니다. 현재 선택지는 다음 두 가지입니다.

1. 저장 전 상품 정보 확인 모달에서 사용자가 필수 분류값을 선택합니다.
2. BE에 유사상품 추천 결과 전용 저장 API를 추가하여 분류 및 저장을 서버가 처리합니다.

전용 저장 API가 추가되기 전까지는 1번 방식이 현재 API 계약에 맞습니다.

### 네이버 필드 매핑

| 저장 요청 | 추천 응답 | 비고 |
| --- | --- | --- |
| `name` | `title` | 정제된 상품명 |
| `brandName` | `brand` | 비어 있으면 사용자 입력 필요 |
| `productCode` | `productId` | 현재 별도 상품 코드가 없으면 사용 가능 |
| `imageUrl` | `image` | HTTPS URL |
| `externalSource` | 고정값 `NAVER_SHOPPING` | 프로젝트 카탈로그의 네이버쇼핑 출처 code |
| `externalProductId` | `productId` | 네이버 상품 ID |
| `externalProductUrl` | `link` | 네이버 외부 상품 URL |

### 복수 저장

저장 API는 한 번에 상품 1개만 저장합니다. 여러 상품을 선택하면 상품별로 각각 호출합니다.

```ts
const results = await Promise.allSettled(
  selectedProducts.map((product) =>
    saveWishlistProduct(userId, toWishlistRequest(product, formValues)),
  ),
);
```

상품별 성공·실패를 독립적으로 표시해야 합니다.

## 8. 에러 처리

| HTTP | 발생 조건 | FE 처리 권장 |
| --- | --- | --- |
| `400` | 선택한 `clothesId`가 사용자 옷장에 없거나 삭제됨 | 옷 목록 새로고침 후 다시 선택 |
| `401` | JWT 없음 또는 만료 | 로그인 또는 토큰 갱신 |
| `403` | JWT 사용자와 path `userId` 불일치 | 접근 불가 안내 |
| `502` | 네이버쇼핑 API 장애, 인증 실패 또는 응답 오류 | 재시도 버튼과 외부 연동 오류 안내 |

대표 오류 응답:

```json
{
  "success": false,
  "data": null,
  "message": "해당 사용자의 옷을 찾을 수 없습니다."
}
```

외부 API 오류 응답:

```json
{
  "success": false,
  "data": null,
  "message": "외부 서비스 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
}
```

## 9. 로딩 및 빈 상태

### 기준 옷 없음

```text
유사한 상품을 찾으려면 먼저 보유 옷 또는 미보유 관심 상품을 옷장에 등록해 주세요.
```

옷 등록 화면으로 이동하는 버튼을 제공합니다.

### 추천 요청 중

- 기준 옷 선택을 잠시 비활성화합니다.
- 추천 카드 영역에 skeleton 또는 spinner를 표시합니다.
- 중복 요청을 방지합니다.

### 추천 결과 없음

```text
비슷한 상품을 찾지 못했어요. 다른 옷을 선택해 다시 시도해 주세요.
```

기준 옷 다시 선택 버튼을 제공합니다.

### 이미지 오류

`product.image` 로딩 실패 시 공통 상품 placeholder를 사용합니다.

## 10. FE API 함수 예시

```ts
export async function getOwnedClothes(
  userId: number,
): Promise<ApiResponse<Clothes[]>> {
  return api.get(`/api/v1/users/${userId}/clothes`);
}

export async function getSimilarProducts(
  userId: number,
  clothesId: number,
): Promise<SimilarProductRecommendationResponse> {
  return api.get(
    `/api/v1/users/${userId}/clothes/${clothesId}/similar-products`,
  );
}
```

프로젝트의 HTTP client가 base URL에 `/api/v1`을 이미 포함하고 있다면 경로를 중복해서 붙이지 않도록 주의합니다.

## 11. FE 구현 체크리스트

- [ ] 로그인 사용자의 `userId`만 path에 사용
- [ ] `GET /api/v1/users/{userId}/clothes`로 기준 옷 모달 구성
- [ ] 추천 호출에는 `wardrobeClothesId`가 아닌 `clothesId` 사용
- [ ] 응답의 `baseClothes`를 현재 기준 옷으로 표시
- [ ] `query`를 사용자 화면용 문구로 사용하지 않음
- [ ] `products: []` 빈 상태 처리
- [ ] 외부 구매 링크 이동 처리
- [ ] 상품 복수 선택 상태 관리
- [ ] 저장 전 필수 분류값 확보
- [ ] 복수 저장 시 상품별 저장 API 호출 및 개별 실패 처리
- [ ] `400`, `401`, `403`, `502` 오류 UI 분리
