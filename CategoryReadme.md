# CategoryReadme

옷장난감 프로젝트 **공용 카테고리** 사용 가이드입니다.  
FE, BE, AI(Gemini) 모두 이 문서의 **code 값**을 기준으로 연동합니다.

---

## 1. 개요

| 구분 | 저장 위치 | 설명 |
|------|-----------|------|
| `category` | `clothes.category` | 대분류 (상의/하의/아우터/신발) |
| `item_type` | `clothes.item_type` | 소분류 (반팔, 데님, 패딩 등) |
| `color` | `clothes.color` | 컬러 코드 |
| `styles` | `styles` 테이블 + `clothing_styles` | 스타일 코드 |

- **DB/API 저장값**: `code` (영문 enum name)
- **화면 표시**: `name` (한글 label)
- **컬러 UI**: `hex` (색상 원 표시용, DB 저장 ❌)

---

## 2. API

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/categories` | 불필요 | 전체 목록 + 사용 설명서 |
| GET | `/api/categories/guide` | 불필요 | 사용 설명서만 |
| GET | `/api/categories/ai-guide` | 불필요 | AI 분류용 텍스트 가이드 |

### 2.1 응답 구조 (`GET /api/categories`)

```json
{
  "success": true,
  "data": {
    "guide": { ... },
    "categories": [ ... ],
    "styles": [ ... ],
    "colors": [ ... ]
  }
}
```

### 2.2 옷 등록 예시

```json
{
  "category": "TOP",
  "item_type": "SHORT_SLEEVE",
  "color": "WHITE",
  "styles": ["CASUAL", "MINIMAL"]
}
```

### 2.3 컬러 표시 예시

```json
{
  "code": "NAVY",
  "name": "네이비",
  "hex": "#1F3A5F"
}
```

---

## 3. 연동 규칙

1. **서버 저장 시 `code`만 사용** (`name`, `hex`는 저장하지 않음)
2. **`item_type`은 선택한 `category` 하위 코드만** 사용 가능
3. **컬러 UI**는 `hex`로 색 원(swatch) 표시, `name`은 tooltip/접근성용
4. **`WHITE`** 색상 원은 밝은 배경에서 `border` 필요
5. **카테고리 목록 변경 시** BE enum 수정 → 이 문서/API 동시 갱신

---

## 4. 대분류 (category)

| code | name |
|------|------|
| `TOP` | 상의 |
| `BOTTOM` | 하의 |
| `OUTER` | 아우터 |
| `SHOES` | 신발 |

---

## 5. 소분류 (item_type)

### 5.1 TOP (상의)

| code | name | description |
|------|------|-------------|
| `LONG_SLEEVE` | 롱슬리브 | 긴소매 티셔츠 |
| `SHORT_SLEEVE` | 반팔 | 반소매 티셔츠 |
| `SHIRT` | 셔츠 | 셔츠 |
| `HOODIE` | 후드 | 후드 |
| `SWEAT` | 스웨트 | 스웨트 |
| `COLLAR_TEE` | 카라 티셔츠 | 카라 티셔츠 |
| `SLEEVELESS` | 민소매 | 민소매 |
| `KNIT` | 니트 | 니트 |

### 5.2 BOTTOM (하의)

| code | name | description |
|------|------|-------------|
| `DENIM` | 데님 | 데님 |
| `TRAINING` | 트레이닝 | 트레이닝 |
| `COTTON` | 코튼 | 면 |
| `SLACKS` | 슬랙스 | 슬랙스 |
| `SHORTS` | 숏츠 | 숏츠 |
| `CARGO` | 카고 | 카고 |
| `SKIRT` | 스커트 | 스커트 |

### 5.3 OUTER (아우터)

| code | name | description |
|------|------|-------------|
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

### 5.4 SHOES (신발)

| code | name | description |
|------|------|-------------|
| `SNEAKERS` | 스니커즈 | 스니커즈 |
| `SPORTS_SHOES` | 스포츠화 | 운동화, 등산화 등 |
| `LOAFER` | 로퍼 | 로퍼 |
| `DERBY` | 더비 | 더비 |
| `BOOTS` | 부츠 | 부츠 |
| `SANDALS_SLIPPERS` | 샌들/슬리퍼 | 샌들/슬리퍼 |
| `FLAT` | 플랫 | 플랫 |
| `HEEL` | 힐 | 힐 |

---

## 6. 스타일 (styles)

| code | name | description |
|------|------|-------------|
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

> `styles` 테이블은 앱 기동 시 위 10개가 자동 시드됩니다.

---

## 7. 컬러 (colors)

| code | name | hex |
|------|------|-----|
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

---

## 8. 프론트엔드 (Vite) 연동

```ts
// 1. 앱 초기화 시 카탈로그 로드
const { data } = await fetch('/api/categories').then(r => r.json());

// 2. code → hex 매핑 (상품 상세 컬러 swatch)
const colorMap = Object.fromEntries(
  data.colors.map((c) => [c.code, c.hex])
);

// 3. 상품 상세 - 이름 대신 색 원 표시
<div
  style={{
    backgroundColor: colorMap[product.color],
    border: product.color === 'WHITE' ? '1px solid #E0E0E0' : 'none',
  }}
  title={data.colors.find(c => c.code === product.color)?.name}
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

---

## 9. AI (Gemini) 연동

```java
// GeminiService 예시
String guide = categoryCatalogService.getAiClassificationGuide();
// 프롬프트에 guide 포함 후 JSON 응답 요청

categoryCatalogService.validateClothesClassification(
    aiResult.getCategory(),
    aiResult.getItemType(),
    aiResult.getColor()
);
```

AI 응답은 반드시 위 **code 목록**만 사용해야 합니다.

---

## 10. 백엔드 코드 위치

```
src/main/java/com/closetnangam/be/domain/catalog/
├── enums/
│   ├── ClothesCategory.java
│   ├── ClothesItemType.java
│   ├── ClothesColor.java
│   └── StyleCode.java
├── entity/Style.java
├── controller/CategoryController.java
└── service/CategoryCatalogService.java
```

카테고리 추가/변경 시 **enum 수정 → API 자동 반영 → 이 문서 갱신** 순서로 진행하세요.
