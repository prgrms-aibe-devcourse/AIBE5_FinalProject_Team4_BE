# 추천 기능 아키텍처 · 점수표 가이드

옷장난감 BE의 **추천 유형별 Service 분리**와 **유형별 전용 점수표** 정리 문서입니다.  
PRD·FE·BE 연동 및 팀 합의용으로 사용합니다.

> **원칙:** `ClothesColor` / `StyleCode` / `ClothesItemType` **enum은 공유**하지만,  
> **Compatibility(어울림)** 와 **Similarity(유사)** 는 **점수표·가중치를 반드시 분리**합니다.

---

## 1. 아키텍처 개요

```
Layer 1  후보 선정     내 옷장 / 위시리스트 / 네이버 API
    ↓
Layer 2  점수·랭킹     유형별 Scoring Engine (점수표 §3~§8)
    ↓
Layer 3  설명 (선택)   Gemini MD 코멘트 (점수 없음)
```

### 1.1 추천 유형 · API · Engine 매핑

| PRD 기능 | API (안) | Scoring Engine | 점수표 섹션 | BE 상태 |
|----------|----------|----------------|-------------|---------|
| **어울리는 옷 추천** | `GET .../clothes/{id}/recommendations` | `CompatibilityEngine` | **§3** | ✅ 일부 구현 |
| **OOTD (오늘의 코디)** | `GET .../outfits/today` | `OutfitCompositionEngine` | **§4** | ❌ |
| **유사 상품 추천** | `GET .../products/{id}/similar` | `SimilarityEngine` | **§5** | ❌ |
| **취향 기반 상품 추천** | `GET .../products/recommendations` | `TasteMatchEngine` | **§6** | ❌ |
| **취향 분석** | `GET .../style-profile` | *(집계, 점수 아님)* | **§7** | ❌ |
| **AI MD 추천** | `POST .../md-comment` | *(설명만)* | **§8** | ❌ |
| **날씨/지역 가중** | query param | `WeatherBooster` | **§9** | ❌ |

### 1.2 점수표 파일 매핑 (코드 목표 구조)

| Engine | Java 클래스 (안) | 용도 |
|--------|------------------|------|
| Compatibility | `ColorCompatibilityTable` | 코디 색 조합 ✅ |
| Compatibility | `StyleCompatibilityTable` | 코디 스타일 조합 ⏳ |
| Compatibility | `ItemTypeCompatibilityTable` | 코디 itemType ✅ |
| Similarity | `SimilarityColorTable` | 닮은 색 ❌ |
| Similarity | `SimilarityStyleTable` | 닮은 스타일 ❌ |
| Similarity | `SimilarityItemTypeTable` | 닮은 소분류 ❌ |
| Taste | `TasteProfileMatcher` | 취향 프로필 vs 상품 ❌ |
| Weather | `WeatherItemTypeBooster` | 기온·강수 보정 ❌ |

---

## 2. Compatibility vs Similarity 한눈에

| | **어울림 (§3, §4)** | **유사 (§5)** |
|--|---------------------|---------------|
| 질문 | 같이 입을까? | 비슷한가? |
| 카테고리 | 보통 **다름** | 보통 **같음** |
| 색 | 조화 (보색·중립) ↑ | **동일·근접** ↑ |
| 스타일 | 섞여도 되는 톤 | **거의 동일** ↑ |
| itemType | 코디 페어 (숏츠+스니커즈) | **같은 타입** ↑ |

---

# §3 어울리는 옷 추천 (Compatibility Engine)

**목적:** 보유 옷 1벌(anchor)과 **다른 카테고리** 옷장 아이템의 코디 어울림.  
**코드:** `ClothesRecommendationService` + `ColorCompatibilityTable` + `ItemTypeCompatibilityTable`

### 3.1 후보 필터

- anchor: 사용자 **OWNED** 1벌
- 후보: **OWNED**, `category ≠ anchor.category`, `clothesId ≠ anchor`

### 3.2 최종 점수 (구현됨)

```
compatibilityScore = round(100 × (
  0.35 × colorScore
+ 0.30 × styleScore
+ 0.20 × itemTypeScore
+ 0.15 × seasonScore
))
```

### 3.3 §3-A 색상 — ColorCompatibilityTable (코디 · ✅ 코드)

출처: 「옷 색깔 조합 3초 코디」. 방향성 테이블 + **양방향 max**.

| 등급 | 점수 | 설명 |
|------|------|------|
| GOOD | 1.0 | 잘 어울림 |
| SAME | 0.7 | 동일 색 |
| SOSO | 0.5 | 무난 |
| DEFAULT | 0.1 | 차트 외 |
| null | 0.5 | 정보 없음 |

**WHITE·BLACK:** 모든 색 GOOD.

| MAIN | GOOD | SOSO |
|------|------|------|
| PINK | WHITE, GRAY, BEIGE, RED, ORANGE, BLACK | LIGHT_BLUE, NAVY |
| RED | WHITE, PINK, BEIGE, BROWN, GRAY, BLACK | LIGHT_BLUE |
| ORANGE | GREEN, WHITE, BROWN, BEIGE, BLACK | YELLOW, LIGHT_BLUE, GRAY |
| BEIGE | LIGHT_BLUE, WHITE, BROWN, NAVY, BLACK | ORANGE, YELLOW, GREEN |
| YELLOW | WHITE, GRAY, BLACK | GREEN, BROWN |
| GREEN | WHITE, BROWN, BEIGE, GRAY, RED, BLACK | LIGHT_BLUE, YELLOW |
| LIGHT_BLUE | BEIGE, WHITE, BROWN, GRAY, NAVY, BLACK | ORANGE, PINK |
| NAVY | WHITE, BROWN, LIGHT_BLUE, GRAY, BLACK | YELLOW, PINK |
| PURPLE | GRAY, WHITE, BLACK | LIGHT_BLUE |
| BROWN | BEIGE, WHITE, LIGHT_BLUE, BLACK, NAVY | ORANGE, PINK |
| GRAY | BLACK, WHITE, LIGHT_BLUE, PINK, RED, NAVY | PURPLE |

```
harmony = max(chart(A→B), chart(B→A))
pairScore = harmony × weight(A) × weight(B)   // primary=1.0, secondary=0.6
colorScore = max(all pairs)
```

### 3.4 §3-B 스타일 — StyleCompatibilityTable (코디 · ⏳ 팀 검토)

**현재 코드:** Jaccard `0.2 + 0.8 × (|교집합|/|합집합|)`  
**확정 후:** 아래 매트릭스로 교체 (복수 태그 → **조합 max**)

| 등급 | 점수 |
|------|------|
| GOOD / SAME | 1.0 |
| NEUTRAL | 0.7 |
| SOSO | 0.5 |
| CLASH | 0.3 |

**GOOD:** CASUAL↔STREET/MINIMAL/SPORTY/CITYBOY/RETRO · MINIMAL↔CHIC/CLASSIC/CITYBOY · CHIC↔CLASSIC · STREET↔RETRO/WORKWEAR/CITYBOY/SPORTY · WORKWEAR↔GORPCORE/STREET · GORPCORE↔SPORTY · RETRO↔CLASSIC · SPORTY↔CASUAL …

**SOSO:** MINIMAL↔RETRO/SPORTY · CLASSIC↔WORKWEAR/RETRO · GORPCORE↔CASUAL/CITYBOY · CHIC↔RETRO · CITYBOY↔SPORTY …

**CLASH:** CLASSIC↔SPORTY/STREET/GORPCORE · CHIC↔SPORTY/GORPCORE/WORKWEAR · MINIMAL↔GORPCORE/WORKWEAR

### 3.5 §3-C itemType — ItemTypeCompatibilityTable (코디 · ✅ 코드)

| 등급 | 점수 |
|------|------|
| GOOD | 1.0 |
| NEUTRAL | 0.7 (cohesion group Jaccard) |
| SOSO | 0.5 |
| CLASH | 0.3 |

**Cohesion Group (8):** CASUAL, SMART, FORMAL, ATHLETIC, STREET, OUTDOOR, SUMMER, WINTER — enum 전 itemType 매핑.

**명시 GOOD 예:** SHIRT+SLACKS · HOODIE+DENIM · SHORTS+SNEAKERS · SKIRT+HEEL · SLACKS+LOAFER · SHIRT+BLAZER …

**명시 CLASH 예:** HOODIE+BLAZER · SHORTS+BLAZER · PADDING+SANDALS …

### 3.6 §3-D 시즌 — SeasonScore (코디 · ✅ 코드)

| 조건 | seasonScore |
|------|-------------|
| 동일 | 1.0 |
| 한쪽 null | 0.7 |
| 불일치 | 0.3 |

---

# §4 OOTD · 오늘의 코디 (Outfit Composition Engine)

**목적:** 옷장에서 **TOP+BOTTOM+(OUTER)+SHOES** 세트 1~3개 추천.  
**후보:** 내 옷장 OWNED 전체. **Compatibility 점수표(§3) 재사용 + 조립 규칙 추가.**

### 4.1 코디 세트 점수

```
outfitScore = round(100 × (
  0.55 × avgPairCompatibility   // 세트 내 모든 슬롯 쌍 평균 (§3 엔진)
+ 0.25 × slotCompletenessScore   // §4-B
+ 0.20 × weatherFitScore         // §9 (없으면 season 대체)
))
```

`avgPairCompatibility`: 세트 내 2벌씩 §3 `compatibilityScore` (0~1) 평균.

### 4.2 §4-A 슬롯 완성도 — SlotCompletenessTable

| 포함 슬롯 | slotCompletenessScore |
|-----------|------------------------|
| TOP + BOTTOM + SHOES | 1.0 |
| TOP + BOTTOM (+ OUTER 생략) | 0.9 |
| TOP + BOTTOM + OUTER + SHOES | 1.0 (OUTER 보너스 +0.05 cap) |
| SHOES 없음 | 0.6 |
| TOP 또는 BOTTOM만 | 0.3 (코디 미완성) |

### 4.3 §4-B Occasion 가중 (선택 query: `?occasion=`)

| occasion | itemType/style 가산 예 |
|----------|------------------------|
| WORK | BLAZER, SLACKS, SHIRT, CLASSIC/CHIC +0.1 |
| DATE | SKIRT, HEEL, CHIC/MINIMAL +0.1 |
| SPORT | TRAINING, SPORTS_SHOES, SPORTY +0.1 |
| DAILY | (가산 없음, 기본 §3) |

### 4.4 §4-C 색상 규칙 (코디 세트 전체)

§3-A 재사용 + **세트 전체** 고유 primary 색 4개 초과 시 **−0.05** (너무 많은 색).

---

# §5 유사 상품 추천 (Similarity Engine)

**목적:** 선택한 **내 옷 / 외부 상품**과 **비슷한** 상품 탐색.  
**⚠️ §3 색상표 사용 금지** — 닮음 전용 표 사용.

### 5.1 최종 점수 (안)

```
similarityScore = round(100 × (
  0.30 × categoryScore
+ 0.25 × itemTypeScore
+ 0.25 × colorSimilarityScore
+ 0.15 × styleSimilarityScore
+ 0.05 × brandScore
))
```

### 5.2 §5-A category — CategorySimilarityTable

| 조건 | categoryScore |
|------|---------------|
| 동일 category | 1.0 |
| (유사상품은 cross-category 비추천) | — |
| category 다름 | **후보 제외** (필터) |

### 5.3 §5-B itemType — SimilarityItemTypeTable

| 조건 | itemTypeScore |
|------|---------------|
| 동일 itemType | 1.0 |
| 같은 category, 같은 cohesion group | 0.85 |
| 같은 category, 다른 group | 0.6 |
| (cross-category) | 후보 제외 |

**같은 category 근접 예 (0.85):** DENIM↔COTTON · SLACKS↔COTTON · SNEAKERS↔SPORTS_SHOES · SINGLE_COAT↔DOUBLE_COAT

### 5.4 §5-C 색상 — SimilarityColorTable (§3-A와 별개)

| 조건 | colorSimilarityScore |
|------|----------------------|
| primary 동일 | 1.0 |
| secondary 1개 이상 동일 | 0.95 |
| **같은 톤군** (§5-D) | 0.80 |
| 한쪽 NEUTRAL(WHITE/BLACK/GRAY/BEIGE) | 0.65 |
| 다른 톤군 | 0.25 |
| 정보 없음 | 0.5 |

### 5.5 §5-D 색상 톤군 (Similarity 전용)

| 톤군 | 색상 |
|------|------|
| NEUTRAL | WHITE, BLACK, GRAY, BEIGE |
| WARM | RED, ORANGE, PINK, YELLOW, BROWN |
| COOL | GREEN, LIGHT_BLUE, NAVY, PURPLE |

### 5.6 §5-E 스타일 — SimilarityStyleTable

**코디(§3-B)와 반대:** 겹칠수록 높음.

```
styleSimilarityScore = 0.2 + 0.8 × Jaccard(anchorStyles, candidateStyles)
```

| Jaccard | 점수 |
|---------|------|
| 1.0 (완전 동일) | 1.0 |
| 0.5 | 0.6 |
| 0 (공통 없음) | 0.2 |

### 5.7 §5-F brand — BrandSimilarityTable

| 조건 | brandScore |
|------|------------|
| brandName 동일 | 1.0 |
| brandName 다름 | 0.5 |
| brandName UNKNOWN | 0.5 |

---

# §6 취향 기반 상품 추천 (Taste Match Engine)

**목적:** 온보딩 + 옷장 집계 **취향 프로필** vs **네이버 쇼핑** 상품.  
**§3·§5와 별개** — “내 취향에 맞는가?”

### 6.1 최종 점수 (안)

```
tasteMatchScore = round(100 × (
  0.35 × styleTasteScore
+ 0.30 × colorTasteScore
+ 0.20 × categoryTasteScore
+ 0.15 × itemTypeTasteScore
))
```

입력: `UserStyleProfile` (§7에서 생성)

### 6.2 §6-A 스타일 — TasteStyleMatchTable

| 조건 | styleTasteScore |
|------|-----------------|
| 상품 style ∈ 프로필 **Top3** | 1.0 |
| 상품 style ∈ 프로필 **Top4~6** | 0.75 |
| 프로필에 없음 | 0.4 |
| CLASH 조합 (§3-B) with dominant style | 0.2 |

### 6.3 §6-B 색상 — TasteColorMatchTable

| 조건 | colorTasteScore |
|------|-----------------|
| primary ∈ 프로필 **선호색 Top3** | 1.0 |
| primary ∈ **선호색 Top4~5** | 0.8 |
| Similarity 톤군(§5-D)만 일치 | 0.6 |
| 불일치 | 0.3 |

### 6.4 §6-C category — TasteCategoryMatchTable

| 조건 | categoryTasteScore |
|------|---------------------|
| category ∈ 프로필 **자주 등록 Top2** | 1.0 |
| category ∈ Top3~4 | 0.7 |
| 그 외 | 0.4 |

### 6.5 §6-D itemType — TasteItemTypeMatchTable

| 조건 | itemTypeTasteScore |
|------|------------------|
| itemType ∈ 프로필 **Top5** | 1.0 |
| 같은 category 내 다른 itemType | 0.6 |
| 그 외 | 0.4 |

---

# §7 취향 분석 (Style Profile — 점수표 아님)

**목적:** 옷장·온보딩 데이터 **집계** → `UserStyleProfile` 생성.  
랭킹 점수가 아니라 **프로필 필드** 정의.

| 필드 | 집계 방법 |
|------|-----------|
| `dominantStyles[]` | 옷장 style 태그 빈도 Top N |
| `preferredColors[]` | primary color 빈도 Top N |
| `preferredCategories[]` | category 빈도 Top N |
| `preferredItemTypes[]` | itemType 빈도 Top N |
| `onboardingStyles[]` | 온보딩 선택 (가중치 ×2) |

→ §6 Taste Engine 입력으로 사용.

---

# §8 AI MD 추천 (설명 Layer — 점수 없음)

**목적:** §3~§6 추천 **결과**에 스타일리스트 코멘트 추가.  
**점수표 없음.** Gemini 입력·출력 규칙만 정의.

| 항목 | 내용 |
|------|------|
| 입력 | anchor 메타, 추천 Top3, breakdown, (선택) 날씨 |
| 출력 | `mdComment` 1~3문장 (색·스타일·itemType 근거 언급) |
| 실패 | `mdComment: null`, 추천 목록 유지 |

---

# §9 날씨 · 지역 가중 (Weather Booster)

**목적:** §3·§4 점수 **보정**. 단독 추천 API 아님.  
`weatherFitScore` 또는 `compatibilityScore`에 **가산** (cap +10~15점).

### 9.1 §9-A 기온 — TemperatureItemTypeTable

| 기온(℃) | 가산 itemType | 감점 itemType |
|----------|---------------|---------------|
| ≥28 | SHORTS, SLEEVELESS, SANDALS, SHORT_SLEEVE | PADDING, SINGLE_COAT, BOOTS |
| 20~27 | LIGHT_PADDING, WINDBREAKER, SNEAKERS | (없음) |
| 10~19 | KNIT, SWEAT, HOODIE, LOAFER | SHORTS, SANDALS |
| ≤9 | PADDING, SINGLE_COAT, DOUBLE_COAT, BOOTS | SHORTS, SLEEVELESS, SANDALS |

| 매칭 | weatherFitScore |
|------|-----------------|
| 가산 목록 포함 | 1.0 |
| 중립 | 0.7 |
| 감점 목록 포함 | 0.3 |

### 9.2 §9-B 강수 — PrecipitationTable

| 조건 | 보정 |
|------|------|
| 비/눈 | WINDBREAKER, HOOD_ZIPUP, BOOTS +0.1 · SANDALS −0.15 |
| API 실패 | §3 seasonScore만 사용, `weatherSummary: null` |

---

## 10. 점수표 ↔ 코드 동기화

| 섹션 | Java (현재/안) | 상태 |
|------|----------------|------|
| §3-A | `ColorCompatibilityTable` | ✅ |
| §3-B | `StyleCompatibilityTable` | ⏳ 문서만 |
| §3-C | `ItemTypeCompatibilityTable` | ✅ |
| §3-D | `ClothesRecommendationService` | ✅ |
| §4 | `OutfitRecommendationService` | ❌ |
| §5 | `Similarity*Table` | ❌ |
| §6 | `TasteProfileMatcher` | ❌ |
| §7 | `UserStyleProfileService` | ❌ |
| §8 | `RecommendationMdService` | ❌ |
| §9 | `WeatherItemTypeBooster` | ❌ |

---

## 11. 외부 API 실패 (중 priority)

| API | fallback |
|-----|----------|
| 날씨 | §3 season만, OOTD 배너 |
| 네이버 | §6·§5 빈 목록 + 안내 |
| Gemini MD | `mdComment: null` |

---

## 12. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-06-01 | 초안: 아키텍처, 색상·스타일(어울림) |
| 2026-06-01 | **추천 유형별 점수표 분리** — Similarity/Taste/OOTD/Weather/MD 추가 |

코드 동기화: `ColorCompatibilityTable.java`, `ItemTypeCompatibilityTable.java`, `ClothesRecommendationService.java`
