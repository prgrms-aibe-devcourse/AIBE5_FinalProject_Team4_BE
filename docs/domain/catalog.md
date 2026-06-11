---
doc_type: shared
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-10
---

# 카탈로그 사용 가이드

이 문서는 옷 등록, 추천, 필터, 색상 표시에서 사용하는 공용 카탈로그 기준입니다. FE/BE 모두 이 문서의 `code` 값을 기준으로 연동합니다. 단, 옷 대상 성별(`gender`)은 사용자 화면 표시/필터 대상이 아니라 내부 분류/추천용 code입니다.

## 문서 위치 기준

- BE 레포: 카탈로그 원본 문서입니다.
- FE 레포: 동일본을 둡니다.
- FE 전용 UI 표현, 컴포넌트, mock 데이터 위치는 FE 문서에서 별도로 정리합니다.

## 개요

| 구분 | 저장 위치 | 설명 |
| --- | --- | --- |
| `category` | `CLOTHES.category` | 대분류: 상의, 하의, 아우터, 신발 |
| `itemType` | `CLOTHES.item_type` | 소분류: 반팔, 데님, 패딩 등 |
| `season` | `CLOTHES.season` | 옷 자체의 대상 계절 code. 옷 등록 시 1개 선택하며 생성 후 변경하지 않음 |
| `gender` | `CLOTHES.gender` | 옷 분류/추천용 대상 성별 code |
| `primaryColor` | `CLOTHING_COLORS.color_code` | 대표 색상 1개 |
| `secondaryColors` | `CLOTHING_COLORS.color_code` | 보조 색상 목록 |
| `styles` | `STYLES`, `CLOTHING_STYLES` | 스타일 코드 목록 |

## 저장과 표시 규칙

- DB/API 저장값은 영문 `code`를 사용합니다.
- 화면 표시에는 한글 `name` 또는 `label`을 사용합니다.
- `gender`는 사용자 화면 표시 대상이 아니며, 내부 분류/추천과 저장 요청에 사용하는 code입니다.
- `season`은 옷 등록 시 1개 선택하며, 생성된 옷의 계절은 변경하지 않습니다.
- 색상 HEX 값은 UI 표시용이며 DB 저장값으로 사용하지 않습니다.
- `itemType`은 선택한 `category` 하위 코드만 사용할 수 있습니다.
- 대표 색상과 보조 색상은 중복될 수 없습니다.
- 보조 색상끼리 중복될 수 없습니다.
- 스타일은 최소 1개 이상 선택합니다.
- 스타일끼리 중복될 수 없습니다.
- 별도의 임의 개수 제한은 두지 않고, 중복 없이 카탈로그 전체 범위까지 선택할 수 있습니다.
- 현재 API validation 기준은 보조 색상 최대 12개, 스타일 최대 10개입니다.

## 필드명 기준

| 구분 | 기준 |
| --- | --- |
| DB 컬럼 | `category`, `item_type`, `color_code`, `style_code`처럼 snake_case를 사용합니다. |
| API 요청/응답 DTO | `itemType`, `season`, `gender`, `primaryColor`, `secondaryColors`처럼 camelCase를 사용합니다. |
| AI 응답 JSON | `itemType`, `season`, `gender`, `primaryColor`, `secondaryColors`, `styles`를 사용합니다. |

`GET /api/v1/categories`의 `guide.example` 안에는 현재 구현상 `item_type`이 포함될 수 있습니다. 실제 옷 등록 저장 요청과 AI 응답 기준은 `itemType`입니다.

## API

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/v1/categories` | 불필요 | 전체 카탈로그와 사용 가이드 조회 |
| GET | `/api/v1/categories/guide` | 불필요 | 사용 가이드 조회 |
| GET | `/api/v1/categories/ai-guide` | 불필요 | AI 분류용 텍스트 가이드 조회 |
| GET | `/api/v1/categories/external-sources` | 불필요 | 외부 쇼핑 출처 목록 조회 |

`/api/categories` 경로도 현재 구현에 남아 있지만, 신규 연동은 `/api/v1/categories`를 우선 사용합니다.

## 응답 구조

`GET /api/v1/categories`는 공통 `ApiResponse<T>` 안에 카탈로그 데이터를 담아 반환합니다.

```json
{
  "success": true,
  "data": {
    "guide": {
      "summary": "공용 카테고리 코드 사용 안내. DB 저장·AI 분류·화면 표시 시 아래 규칙을 따릅니다.",
      "fields": [
        {
          "key": "category",
          "label": "대분류",
          "description": "의류 종류 대분류 코드입니다. TOP, BOTTOM, OUTER, SHOES 중 하나를 사용합니다.",
          "example": "TOP"
        }
      ],
      "notes": [
        "API 응답의 code 값을 서버 저장값으로 사용하세요. name(한글)은 화면 라벨용입니다.",
        "colors.hex는 UI 표시용입니다. DB에는 colors.code만 저장합니다."
      ],
      "example": {
        "clothesRegistration": {
          "category": "TOP",
          "item_type": "SHORT_SLEEVE",
          "gender": "UNISEX",
          "primaryColor": "WHITE",
          "secondaryColors": ["NAVY"],
          "styles": ["CASUAL", "MINIMAL"]
        },
        "colorDisplay": {
          "code": "NAVY",
          "name": "네이비",
          "hex": "#1F3A5F"
        }
      }
    },
    "categories": [
      {
        "code": "TOP",
        "name": "상의",
        "itemTypes": [
          {
            "code": "SHORT_SLEEVE",
            "name": "반팔",
            "description": "반소매 티셔츠"
          }
        ]
      }
    ],
    "styles": [
      {
        "id": 1,
        "code": "CASUAL",
        "name": "캐주얼",
        "description": "편안하고 일상적인 스타일"
      }
    ],
    "colors": [
      {
        "code": "NAVY",
        "name": "네이비",
        "hex": "#1F3A5F"
      }
    ]
  },
  "message": null
}
```

## 옷 등록 입력 예시

```json
{
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "season": "SUMMER",
  "gender": "UNISEX",
  "primaryColor": "WHITE",
  "secondaryColors": ["NAVY"],
  "styles": ["CASUAL", "MINIMAL"]
}
```

## 색상 표시 예시

```json
{
  "code": "NAVY",
  "name": "네이비",
  "hex": "#1F3A5F"
}
```

FE 색상 swatch는 `hex`를 사용해 표시하고, 저장/요청에는 `code`를 사용합니다. `WHITE`처럼 밝은 색상은 FE에서 border 등 UI 보정이 필요할 수 있습니다.

## 프론트엔드 연동 예시

FE는 앱 초기화 또는 옷 등록 화면 진입 시 카탈로그를 조회해 드롭다운, 필터, 색상 swatch를 구성합니다.

```ts
// 1. 앱 초기화 또는 옷 등록 화면 진입 시 카탈로그 로드
const response = await fetch('/api/v1/categories').then((res) => res.json());

const categories = response.data.categories;
const styles = response.data.styles;
const colors = response.data.colors;

// 2. code -> hex 매핑
const colorMap = Object.fromEntries(
  colors.map((color) => [color.code, color.hex])
);
```

상품 상세에서 색상명을 텍스트로 표시하기보다 색상 swatch로 표시할 수 있습니다.

```tsx
<div
  style={{
    backgroundColor: colorMap[product.primaryColor],
    border: product.primaryColor === 'WHITE' ? '1px solid #E0E0E0' : 'none',
  }}
  title={colors.find((color) => color.code === product.primaryColor)?.name}
/>
```

개발 환경 proxy 예시:

```ts
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
```

FE 연동 규칙:

- 요청에는 `name`이나 `hex`가 아니라 `code`를 보냅니다.
- `itemType` 선택지는 사용자가 선택한 `category`의 `itemTypes`에서만 노출합니다.
- `WHITE`처럼 밝은 색상은 색상 swatch에서 border 처리가 필요할 수 있습니다.
- mock 데이터도 이 문서의 code 목록과 일치해야 합니다.

## AI/Gemini 연동 예시

Gemini 등 AI 분류 결과는 카탈로그 code만 사용해야 합니다. BE는 카탈로그 가이드를 프롬프트에 포함하고, 아래 JSON 형식의 응답을 기대합니다.

```java
String guide = categoryCatalogService.getAiClassificationGuide();
// 프롬프트에 guide 포함 후 JSON 응답 요청

categoryCatalogService.validateCategoryAndItemType(
        aiResult.category(),
        aiResult.itemType()
);
categoryCatalogService.validateClothesColors(
        aiResult.primaryColor(),
        aiResult.secondaryColors()
);
categoryCatalogService.validateStyleCodes(aiResult.styles());
categoryCatalogService.validateGenderCode(
        categoryCatalogService.resolveGenderOrDefault(aiResult.gender()).name()
);
```

사진 기반 옷 분류 응답:

```json
{
  "name": "화이트 반팔 티셔츠",
  "brandName": "UNKNOWN",
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "season": "SUMMER",
  "gender": "UNISEX",
  "primaryColor": "WHITE",
  "secondaryColors": ["NAVY"],
  "styles": ["CASUAL"]
}
```

구매내역 캡처 분석 응답:

```json
{
  "name": "화이트 반팔 티셔츠",
  "brandName": "브랜드명",
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "season": "SUMMER",
  "gender": "UNISEX",
  "primaryColor": "WHITE",
  "secondaryColors": [],
  "styles": ["CASUAL"],
  "optionText": "M / 네이비",
  "suggestedExternalSource": "MUSINSA"
}
```

AI 응답 규칙:

- `category`, `itemType`, `season`, `gender`, `primaryColor`, `secondaryColors`, `styles`는 이 문서의 code 목록만 사용합니다.
- `season`은 `SPRING`, `SUMMER`, `FALL`, `WINTER`, `ALL_SEASON` 중 하나입니다. 옷 등록 저장 요청에서 선택·확정하며 생성된 옷의 계절은 변경하지 않습니다.
- `gender`는 `MALE`, `FEMALE`, `UNISEX` 중 하나입니다. 사용자 화면에 표시하지 않고, 모델·상품명·옷 종류 기반 내부 분류/추천용 code로 사용합니다. 불확실하면 `UNISEX`를 사용합니다.
- 사진·구매내역 등록 draft의 `gender` 기본값은 AI 추정이 아니라 **로그인 사용자 프로필 성별**(`MALE`/`FEMALE`, `OTHER`→`UNISEX`)입니다. FE는 사용자 화면에 표시하지 않더라도 저장 요청에는 해당 code를 포함합니다.
- `itemType`은 선택한 `category` 하위 코드여야 합니다.
- `secondaryColors`가 없으면 빈 배열 `[]`을 사용합니다.
- `suggestedExternalSource`가 불확실하면 `null`을 사용합니다.
- 직접입력 쇼핑몰명은 `suggestedExternalSource`에 넣지 않습니다.

## 대분류

| code | name |
| --- | --- |
| `TOP` | 상의 |
| `BOTTOM` | 하의 |
| `OUTER` | 아우터 |
| `SHOES` | 신발 |

## 대상 성별

| code | meaning |
| --- | --- |
| `MALE` | 남성 대상 옷 |
| `FEMALE` | 여성 대상 옷 |
| `UNISEX` | 남녀 공용 또는 대상 성별을 특정하기 어려운 옷 |

## 계절

`season`은 `CLOTHES.season`에 저장하는 옷 자체의 대상 계절 code입니다. 옷마다 1개만 부여하며, 옷 등록 시 선택하고 생성 후에는 변경하지 않습니다.

| code | name | meaning |
| --- | --- | --- |
| `SPRING` | 봄 | 봄에 주로 착용하는 옷 |
| `SUMMER` | 여름 | 여름에 주로 착용하는 옷 |
| `FALL` | 가을 | 가을에 주로 착용하는 옷 |
| `WINTER` | 겨울 | 겨울에 주로 착용하는 옷 |
| `ALL_SEASON` | 사계절 | 특정 계절에 한정하지 않는 옷 |

## 소분류

### 상의

| code | name | description |
| --- | --- | --- |
| `LONG_SLEEVE` | 롱슬리브 | 긴소매 티셔츠 |
| `SHORT_SLEEVE` | 반팔 | 반소매 티셔츠 |
| `SHIRT` | 셔츠 | 셔츠 |
| `HOODIE` | 후드 | 후드 |
| `SWEAT` | 스웨트 | 스웨트 |
| `COLLAR_TEE` | 카라 티셔츠 | 카라 티셔츠 |
| `SLEEVELESS` | 민소매 | 민소매 |
| `KNIT` | 니트 | 니트 |

### 하의

| code | name | description |
| --- | --- | --- |
| `DENIM` | 데님 | 데님 |
| `TRAINING` | 트레이닝 | 트레이닝 |
| `COTTON` | 코튼 | 면 |
| `SLACKS` | 슬랙스 | 슬랙스 |
| `SHORTS` | 숏츠 | 숏츠 |
| `CARGO` | 카고 | 카고 |
| `SKIRT` | 스커트 | 스커트 |

### 아우터

| code | name | description |
| --- | --- | --- |
| `WINDBREAKER` | 윈드브레이커 | 바람막이 |
| `HOOD_ZIPUP` | 후드집업 | 후드집업 |
| `TRAINING_JACKET` | 트레이닝 자켓 | 트레이닝 자켓 |
| `BLOUSON` | 블루종 | 블루종 |
| `MA1` | MA-1 | MA-1 |
| `VARSITY_JACKET` | 바시티 자켓 | 바시티 자켓 |
| `LEATHER_JACKET` | 레더 자켓 | 가죽 |
| `SHEARLING` | 무스탕 | 무스탕 |
| `FLEECE_JACKET` | 플리스 자켓 | 후리스 |
| `VEST` | 베스트 | 조끼 |
| `WORK_JACKET` | 워크자켓 | 워크자켓 |
| `DENIM_JACKET` | 데님자켓 | 데님자켓 |
| `BLAZER` | 블레이저 | 블레이저 |
| `COACH_JACKET` | 코치자켓 | 코치자켓 |
| `PADDING` | 패딩 | 패딩 |
| `LIGHT_PADDING` | 경량패딩 | 경량패딩 |
| `SINGLE_COAT` | 싱글코트 | 싱글코트 |
| `DOUBLE_COAT` | 더블코트 | 더블코트 |
| `BALMACAAN_COAT` | 발마칸 코트 | 발마칸 코트 |
| `TTEOKBOKKI_COAT` | 떡볶이 코트 | 떡볶이 코트 |

### 신발

| code | name | description |
| --- | --- | --- |
| `SNEAKERS` | 스니커즈 | 스니커즈 |
| `SPORTS_SHOES` | 스포츠화 | 운동화, 등산화 등 |
| `LOAFER` | 로퍼 | 로퍼 |
| `DERBY` | 더비 | 더비 |
| `BOOTS` | 부츠 | 부츠 |
| `SANDALS_SLIPPERS` | 샌들/슬리퍼 | 샌들/슬리퍼 |
| `FLAT` | 플랫 | 플랫 |
| `HEEL` | 힐 | 힐 |

## 스타일

| code | name | description |
| --- | --- | --- |
| `CASUAL` | 캐주얼 | 편안하고 일상적인 스타일 |
| `STREET` | 스트릿 | 스트릿 패션 중심의 스타일 |
| `MINIMAL` | 미니멀 | 단순하고 깔끔한 스타일 |
| `SPORTY` | 스포티 | 스포츠웨어 기반의 활동적인 스타일 |
| `CLASSIC` | 클래식 | 전통적이고 정돈된 스타일 |
| `CHIC` | 시크 | 세련되고 도시적인 스타일 |
| `WORKWEAR` | 워크웨어 | 작업복·유틸리티 중심의 스타일 |
| `CITYBOY` | 시티보이 | 도심형 캐주얼 스타일 |
| `GORPCORE` | 고프코어 | 아웃도어·기능성 중심의 스타일 |
| `RETRO` | 레트로 | 복고풍을 연상시키는 스타일 |

`STYLES` 테이블은 애플리케이션 기동 시 위 스타일 목록을 기준으로 시드합니다.

## 색상

| code | name | hex |
| --- | --- | --- |
| `PINK` | 핑크 | `#FFB6C1` |
| `RED` | 레드 | `#E53935` |
| `ORANGE` | 오렌지 | `#FF9800` |
| `BEIGE` | 베이지 | `#D2B48C` |
| `YELLOW` | 옐로우 | `#FDD835` |
| `GREEN` | 그린 | `#43A047` |
| `LIGHT_BLUE` | 라이트블루 | `#81D4FA` |
| `NAVY` | 네이비 | `#1F3A5F` |
| `PURPLE` | 퍼플 | `#8E24AA` |
| `BROWN` | 브라운 | `#795548` |
| `GRAY` | 그레이 | `#9E9E9E` |
| `WHITE` | 화이트 | `#FFFFFF` |
| `BLACK` | 블랙 | `#212121` |

## 외부 출처

| code | name |
| --- | --- |
| `NAVER_SHOPPING` | 네이버쇼핑 |
| `COUPANG` | 쿠팡 |
| `MUSINSA` | 무신사 |
| `ABLY` | 에이블리 |
| `ZIGZAG` | 지그재그 |
| `TWENTYNINE_CM` | 29CM |
| `WCONCEPT` | W컨셉 |
| `BRANDI` | 브랜디 |
| `UNIQLO` | 유니클로 |
| `SPAO` | 스파오 |
| `EIGHT_SECONDS` | 에잇세컨즈 |
| `HM` | H&M |
| `CUSTOM` | 직접입력 |

## 변경 규칙

- 카탈로그 값이 추가/수정/삭제되면 enum, API 응답, 이 문서를 같은 PR에서 함께 갱신합니다.
- FE 레포의 카탈로그 표시/필터/mock 데이터도 같은 기준으로 동기화합니다.
- 카탈로그 code를 변경할 때는 기존 저장 데이터, API 요청/응답, FE 표시 매핑 영향을 함께 확인합니다.

## 백엔드 코드 위치

```text
src/main/java/com/closetnangam/be/domain/catalog/
├── constants/CatalogLimits.java
├── controller/CategoryController.java
├── dto/response/
├── entity/Style.java
├── enums/
│   ├── ClothesCategory.java
│   ├── ClothesColor.java
│   ├── ClothesItemType.java
│   ├── ExternalSource.java
│   └── StyleCode.java
├── repository/StyleRepository.java
└── service/CategoryCatalogService.java
```

AI 연동 관련 코드는 아래 위치를 함께 확인합니다.

```text
src/main/java/com/closetnangam/be/global/external/gemini/
├── GeminiService.java
└── dto/
    ├── GeminiClothingClassificationResult.java
    └── GeminiPurchaseCaptureExtractionResult.java
```
