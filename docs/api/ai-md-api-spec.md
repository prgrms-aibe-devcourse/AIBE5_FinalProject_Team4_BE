---
doc_type: be_ai_md_api_spec
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-12
---

# AI MD API 명세서

이 문서는 FE에서 AI MD 선택, 코디 추천, 코디 저장, 상품 추천 화면을 구현할 때 사용하는 연동 명세입니다.
공통 API 정책의 원본은 [api-contract.md](./api-contract.md)이며, 이 문서는 AI MD 기능만 FE 관점에서 상세화합니다.

## 1. 공통 기준

### Base URL

```text
{BE_BASE_URL}/api/v1
```

### 인증

모든 AI MD API는 로그인이 필요합니다.

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

성공:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

실패:

```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지"
}
```

## 2. 권장 화면 흐름

```text
1. AI MD 목록 조회
2. 사용자가 MD 선택
3. 코디 추천 또는 상품 추천 선택
4-A. 코디 추천 요청 → 코디 카드 4개 표시
5-A. 사용자가 원하는 코디를 복수 선택
6-A. 선택한 코디마다 저장 API를 1회씩 호출

4-B. 상품 추천 요청 → 상품 카드 최대 40개 표시
5-B. 사용자가 원하는 상품을 복수 선택
6-B. 선택한 상품마다 미보유 옷 저장 API 호출
```

추천 조회만으로는 코디나 상품이 저장되지 않습니다.

## 3. AI MD 목록 조회

```http
GET /api/v1/users/{userId}/recommendations/ai-md/personas
```

사용자 성별에 맞는 MD만 반환합니다.

| 사용자 성별 | 반환 MD |
| --- | --- |
| `MALE` | `taesik`, `junsik` |
| `FEMALE` | `sesoon`, `gahyun`, `seongmi` |

### 응답 타입

```ts
export type AiMdId =
  | "taesik"
  | "junsik"
  | "sesoon"
  | "gahyun"
  | "seongmi";

export type UserGender = "MALE" | "FEMALE";

export interface AiMdPersona {
  id: AiMdId;
  name: string;
  gender: UserGender;
  styleCodes: string[];
  styleNames: string[];
  speechStyle: string;
  description: string;
}
```

### 응답 예시

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
    },
    {
      "id": "junsik",
      "name": "준식이",
      "gender": "MALE",
      "styleCodes": ["MINIMAL", "CLASSIC", "CHIC"],
      "styleNames": ["미니멀", "클래식", "시크"],
      "speechStyle": "댄디한 말투",
      "description": "정돈된 실루엣과 시크한 도시 무드를 좋아하는 남자 MD"
    }
  ],
  "message": null
}
```

FE는 MD ID를 직접 성별로 필터링하지 말고 이 API가 반환한 목록을 그대로 사용합니다.

## 4. AI MD 코디 추천

```http
POST /api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits
```

Request body는 없습니다.

### 동작

- 사용자 옷장 등록 옷(`OWNED`, `WISHLIST`)과 네이버쇼핑 후보를 Gemini에 전달합니다.
- 선택한 MD가 코디 후보 4개를 구성합니다.
- 각 코디에는 사용자 옷장 등록 옷이 최소 1개 포함됩니다.
- 옷장 등록 옷과 외부 상품을 합쳐 `TOP`, `BOTTOM`, `SHOES`가 반드시 포함됩니다.
- `OUTER`와 외부 상품은 선택 사항입니다.
- 옷장 등록 옷만으로 완성된 코디도 가능합니다.
- 이 단계에서는 DB에 아무것도 저장하지 않습니다.

### 주요 응답 타입

```ts
export type ClothesCategory = "TOP" | "BOTTOM" | "OUTER" | "SHOES";

export interface ColorDisplay {
  code: string;
  name: string;
  hex: string;
}

export interface SecondaryColor {
  code: string;
  colorDisplay: ColorDisplay;
  sortOrder: number;
}

export interface StyleTag {
  styleId: number;
  code: string;
  name: string;
  styleRole: "PRIMARY" | "SECONDARY";
  sortOrder: number;
}

export interface Clothes {
  clothesId: number;
  wardrobeClothesId: number | null;
  wardrobeId: number | null;
  userId: number | null;
  name: string;
  brandName: string;
  productCode: string;
  imageUrl: string;
  category: ClothesCategory;
  itemType: string;
  gender: "MALE" | "FEMALE" | "UNISEX";
  primaryColor: string | null;
  primaryColorDisplay: ColorDisplay | null;
  secondaryColors: SecondaryColor[];
  styles: StyleTag[];
  ownershipStatus: "OWNED" | "WISHLIST" | null;
  clothesInfoSource: "PHOTO" | "PURCHASE_HISTORY" | "EXTERNAL_SHOPPING";
  registrationSource: "PHOTO" | "PURCHASE_HISTORY" | "EXTERNAL_SHOPPING" | null;
  externalSource: string;
  externalProductId: string;
  externalProductUrl: string;
  isVerified: boolean;
  isFavorite: boolean | null;
  size: string | null;
  season: string | null;
  userImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NaverShoppingProduct {
  title: string;
  link: string;
  image: string;
  lowestPrice: number;
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
}

export interface AiMdOutfitRecommendation {
  title: string;
  description: string;
  situation: string;
  season: string;
  reason: string;
  stylingTip: string;
  // 기존 FE 계약 유지를 위해 필드명은 ownedItems지만,
  // 각 item의 ownershipStatus는 OWNED 또는 WISHLIST일 수 있습니다.
  ownedItems: Clothes[];
  externalProducts: NaverShoppingProduct[];
}

export interface AiMdOutfitRecommendationData {
  md: AiMdPersona;
  outfits: AiMdOutfitRecommendation[];
}
```

### 응답 예시

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
        "title": "꾸안꾸 스트릿 감성",
        "description": "편안하면서도 신경 쓴 듯한 스트릿 캐주얼 룩이야.",
        "situation": "DAILY",
        "season": "ALL_SEASON",
        "reason": "상의가 중심을 잡고 카고 팬츠와 스니커즈가 스트릿 무드를 완성해.",
        "stylingTip": "소매를 가볍게 걷어 올려 활동적인 느낌을 더해봐.",
        "ownedItems": [
          {
            "clothesId": 1,
            "wardrobeClothesId": 11,
            "name": "스트라이프 롱슬리브",
            "category": "TOP"
          },
          {
            "clothesId": 2,
            "wardrobeClothesId": 12,
            "name": "카고 팬츠",
            "category": "BOTTOM"
          },
          {
            "clothesId": 3,
            "wardrobeClothesId": 13,
            "name": "화이트 스니커즈",
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

실제 `Clothes` 객체에는 타입 정의의 전체 필드가 포함됩니다. 위 예시는 가독성을 위해 일부 필드만 표시했습니다.

## 5. 추천 코디 저장

```http
POST /api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits/save
Content-Type: application/json
```

사용자가 선택한 코디 1개를 저장합니다.

### 요청 타입

```ts
export interface AiMdOutfitSaveRequest {
  title: string;
  description: string;
  situation?: string | null;
  season?: string | null;
  reason?: string | null;
  stylingTip?: string | null;
  wardrobeClothesIds: number[];
  externalProducts: NaverShoppingProduct[];
}
```

### 추천 응답을 저장 요청으로 변환

```ts
export function toAiMdOutfitSaveRequest(
  outfit: AiMdOutfitRecommendation,
): AiMdOutfitSaveRequest {
  return {
    title: outfit.title,
    description: outfit.description,
    situation: outfit.situation,
    season: outfit.season,
    reason: outfit.reason,
    stylingTip: outfit.stylingTip,
    wardrobeClothesIds: outfit.ownedItems.map((item) => {
      if (item.wardrobeClothesId == null) {
        throw new Error("옷장 등록 옷 ID가 없는 코디는 저장할 수 없습니다.");
      }
      return item.wardrobeClothesId;
    }),
    externalProducts: outfit.externalProducts,
  };
}
```

`clothesId`가 아니라 반드시 `wardrobeClothesId`를 전송해야 합니다.

### 저장 검증 조건

| 조건 | 기준 |
| --- | --- |
| 옷장 등록 옷 | 현재 사용자의 `wardrobeClothesId` 최소 1개. `OWNED`, `WISHLIST` 모두 허용 |
| 필수 구성 | 옷장 등록 옷과 외부 상품을 합쳐 `TOP`, `BOTTOM`, `SHOES` 모두 포함 |
| 선택 구성 | `OUTER`, 외부 상품 |
| 외부 상품 없음 | `externalProducts: []` 전송 가능 |
| title | 필수, 최대 100자 |
| description | 필수 |

### 복수 저장

저장 API는 한 번에 코디 1개만 저장합니다. 여러 코디를 선택한 경우 각 코디를 별도 요청으로 저장합니다.

```ts
await Promise.all(
  selectedOutfits.map((outfit) =>
    saveAiMdOutfit(userId, mdId, toAiMdOutfitSaveRequest(outfit)),
  ),
);
```

일부 요청만 실패할 수 있으므로 FE에서는 `Promise.allSettled`로 개별 성공 여부를 표시하는 방식도 권장합니다.

### 요청 예시

```json
{
  "title": "꾸안꾸 스트릿 감성",
  "description": "편안하면서도 신경 쓴 듯한 스트릿 캐주얼 룩이야.",
  "situation": "DAILY",
  "season": "ALL_SEASON",
  "reason": "상의가 중심을 잡고 카고 팬츠와 스니커즈가 스트릿 무드를 완성해.",
  "stylingTip": "소매를 가볍게 걷어 올려 활동적인 느낌을 더해봐.",
  "wardrobeClothesIds": [11, 12, 13],
  "externalProducts": []
}
```

### 저장 응답 타입

```ts
export interface OutfitItem {
  outfitItemId: number;
  itemRole: string;
  layerOrder: number;
  clothes: Clothes;
}

export interface Outfit {
  outfitId: number;
  outfitBookId: number;
  title: string;
  description: string;
  thumbnailUrl: string;
  situation: string;
  season: string;
  favorite: boolean;
  items: OutfitItem[];
  createdAt: string;
  updatedAt: string;
}

export interface SavedAiMdOutfit {
  outfit: Outfit;
  reason: string;
  stylingTip: string;
  ownedItems: Clothes[];
  externalItems: Clothes[];
}
```

외부 상품이 포함된 경우 저장 시 `Clothes`로 생성되고 `externalItems`에 반환됩니다. 사용자의 `WISHLIST`에 자동 등록되는 것은 아닙니다.

## 6. AI MD 상품 추천

```http
GET /api/v1/users/{userId}/recommendations/ai-md/{mdId}/products
```

사용자 스타일 점수, 보유 옷과 선택한 MD 스타일을 기준으로 최대 40개의 네이버쇼핑 상품을 반환합니다.
추천 조회만으로 상품이 저장되지는 않습니다.

### 후보 검색 방식

- `USER_STYLES.combined_weight`가 높은 스타일은 검색어에 더 자주 선택됩니다.
- 점수가 낮더라도 양수인 스타일은 낮은 빈도로 검색 후보에 포함됩니다.
- 사용자 스타일 점수가 없으면 선택한 MD의 스타일 순서를 기본 가중치로 사용합니다.
- 한 번의 요청에서 스타일과 상품 카테고리를 달리한 검색어 8개를 구성합니다.
- 각 검색은 네이버쇼핑 결과 20개를 조회하며, 여러 검색 페이지 중 하나를 무작위로 사용합니다.
- 옷장 색상은 일부 검색어에만 무작위로 포함해 특정 색상에 결과가 고정되는 현상을 줄입니다.
- 최대 160개 원본 결과에서 동일 `productId`와 정규화된 동일 상품명을 제거합니다.
- 후보를 섞은 뒤 최대 120개를 Gemini에 전달하고 최종 40개를 선택합니다.
- 재추천 시 검색 페이지, 스타일 조합, 색상 포함 여부와 후보 순서가 달라질 수 있습니다.
- Gemini에는 동일 모델 반복 방지와 브랜드·카테고리 다양성 조건을 함께 전달합니다.
- 서버의 1차 선별은 같은 브랜드를 최대 2개, 같은 카테고리를 최대 4개로 제한합니다.
- 검색 후보가 한쪽에 치우쳐 40개를 채우지 못할 때만 동일 상품 제외를 유지하며 브랜드·카테고리 제한을 완화합니다.

`query`는 실제 네이버쇼핑 검색에 사용한 여러 검색어를 ` | `로 연결한 디버깅 값입니다. 사용자 화면에 반드시 노출할 필요는 없습니다.

### 응답 타입

```ts
export interface AiMdProductRecommendation {
  product: NaverShoppingProduct;
  reason: string;
}

export interface AiMdProductRecommendationData {
  md: AiMdPersona;
  query: string;
  products: AiMdProductRecommendation[];
}
```

### 응답 예시

```json
{
  "success": true,
  "data": {
    "md": {
      "id": "junsik",
      "name": "준식이",
      "gender": "MALE",
      "styleCodes": ["MINIMAL", "CLASSIC", "CHIC"],
      "styleNames": ["미니멀", "클래식", "시크"],
      "speechStyle": "댄디한 말투",
      "description": "정돈된 실루엣과 시크한 도시 무드를 좋아하는 남자 MD"
    },
    "query": "남성 블랙 미니멀 티셔츠 | 남성 클래식 팬츠 | 남성 시크 자켓",
    "products": [
      {
        "product": {
          "title": "미니멀 싱글 자켓",
          "link": "https://...",
          "image": "https://...",
          "lowestPrice": 59000,
          "highestPrice": null,
          "mallName": "네이버",
          "productId": "123456",
          "productType": "1",
          "brand": "브랜드",
          "maker": "제조사",
          "category1": "패션의류",
          "category2": "남성의류",
          "category3": "재킷",
          "category4": ""
        },
        "reason": "정돈된 실루엣을 유지하면서 도시적인 분위기를 더하기 좋은 상품입니다."
      }
    ]
  },
  "message": null
}
```

### 상품 저장 연동 주의사항

현재 AI MD 상품 추천 API에는 전용 저장 endpoint가 없습니다. 미보유 옷 저장은 아래 API를 사용합니다.

```http
POST /api/users/{userId}/wishlist-clothes
```

다만 이 API는 `category`, `itemType`, `gender`, `primaryColor`, `styles`, `size`, `season` 등
네이버쇼핑 응답에 없는 추가 값을 요구합니다. 따라서 FE는 상품 저장 전에 분류값 입력/확정 UI를 제공하거나,
BE에서 AI MD 추천 상품 전용 저장 API를 추가하는 후속 작업이 필요합니다.

추천 상품의 `productId`, `link`, `image`만으로 위시리스트 저장 요청을 임의 생성하면 안 됩니다.

## 7. 에러 처리

| HTTP | 발생 조건 | FE 처리 권장 |
| --- | --- | --- |
| `400` | 존재하지 않는 MD ID, 성별에 맞지 않는 MD, 코디 필수 구성 누락, 요청 검증 실패 | 요청 상태 유지 후 `message` 표시 |
| `401` | JWT 없음 또는 만료 | 로그인 화면 또는 토큰 갱신 |
| `403` | JWT 사용자와 path `userId` 불일치 | 접근 불가 안내 |
| `404` | 사용자 등 리소스 없음 | 이전 화면 이동 또는 데이터 새로고침 |
| `409` | 옷장 등록 옷 없음, Gemini가 유효 코디 4개를 만들지 못함, AI 사용량 초과 | 옷 등록 유도 또는 재시도 UI |
| `502` | 네이버쇼핑/Gemini 외부 연동 장애 또는 응답 파싱 실패 | 잠시 후 재시도 안내 |

대표 오류 메시지:

```text
사용자 성별에 맞지 않는 AI MD입니다.
AI MD 추천을 받으려면 보유 옷 또는 미보유 관심 상품을 먼저 등록해 주세요.
AI MD가 저장 가능한 코디 4개를 구성하지 못했습니다.
저장할 코디에는 옷장 등록 옷이 최소 1개 포함되어야 합니다.
저장할 코디에는 상의, 하의, 신발이 각각 최소 1개 포함되어야 합니다.
외부 서비스 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.
```

## 8. 로딩과 버튼 상태

AI MD 코디 추천은 네이버쇼핑과 Gemini를 순차 호출하므로 응답이 오래 걸릴 수 있습니다.

- 추천 요청 중 MD 선택과 추천 버튼을 비활성화합니다.
- 코디 추천에는 전용 로딩 문구 또는 skeleton을 표시합니다.
- 저장 버튼은 코디별로 독립된 loading 상태를 가집니다.
- 저장 성공한 카드만 `저장됨` 상태로 변경합니다.
- 재요청 시 이전 결과를 즉시 제거할지 유지할지는 FE 정책으로 결정합니다.
- 동일 요청을 짧은 시간에 반복하면 Gemini 사용량 제한이 발생할 수 있으므로 중복 클릭을 방지합니다.

## 9. FE 구현 체크리스트

- [ ] 로그인 사용자의 `userId`만 path에 사용
- [ ] persona API 응답 목록만 MD 선택지로 노출
- [ ] 추천 조회와 저장 동작을 분리
- [ ] 코디 카드에 `reason`, `stylingTip`, 전체 구성 아이템 표시
- [ ] 옷장 등록 옷(`OWNED`, `WISHLIST`)과 외부 상품을 시각적으로 구분
- [ ] 저장 시 `clothesId`가 아닌 `wardrobeClothesId` 사용
- [ ] 추천 응답의 `externalProducts`를 저장 요청에 그대로 전달
- [ ] 여러 코디 저장 시 코디별 저장 API 호출
- [ ] `400`, `409`, `502` 상태를 서로 다른 UX로 처리
- [ ] 상품 저장 전 추가 분류값 확보 방식 확정
