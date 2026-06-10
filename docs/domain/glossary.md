---
doc_type: shared
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-10
---

# 도메인 용어집

이 문서는 옷장난감에서 사용하는 핵심 도메인 용어와 코드값을 정리합니다. 같은 단어가 기획, API, DB, 코드에서 다른 의미로 사용되지 않도록 이 문서를 기준으로 맞춥니다.

## 핵심 용어

| 용어 | 의미 | 주요 테이블/코드 |
| --- | --- | --- |
| 사용자 | 서비스를 이용하는 회원입니다. 소셜 로그인 계정, 프로필, 스타일 점수, 옷장, 코디북을 가집니다. | `USERS` |
| 사용자 성별 | 사용자 프로필의 성별 정보입니다. 온보딩/마이페이지에서 입력하며 사용자 맞춤 추천과 AI MD 선택에 활용합니다. | `USERS.gender` |
| 소셜 계정 | 사용자가 로그인에 사용하는 OAuth 계정입니다. 하나의 사용자는 여러 소셜 계정을 연결할 수 있습니다. | `SOCIAL_ACCOUNTS` |
| 옷장 | 사용자별 디지털 옷장입니다. 사용자는 1개의 옷장을 가집니다. | `WARDROBES` |
| 옷장 등록 옷 | 특정 사용자의 옷장과 공통 옷 정보를 연결한 사용자별 옷 데이터입니다. 보유/미보유, 사이즈, 즐겨찾기 등은 여기에 저장합니다. | `WARDROBE_CLOTHES` |
| 공통 옷 정보 | 옷 자체의 공통 정보입니다. 상품명, 브랜드, 품번, 대표 이미지, 카테고리, 타입, 계절, 대상 성별, 출처 정보를 저장합니다. | `CLOTHES` |
| 옷 계절 | 옷 자체가 주로 착용되는 계절 code입니다. 옷 등록 시 1개 선택하며, 생성된 옷의 계절은 변경하지 않습니다. 사용자별 옷장 정보가 아니라 공통 옷 정보입니다. | `CLOTHES.season` |
| 옷 대상 성별 | 옷 자체가 대상으로 하는 성별 code입니다. 사용자 프로필 성별과 다른 개념이며, 사용자 화면에는 노출하지 않고 상품/옷 등록 저장, AI 분류, 추천에 사용합니다. | `CLOTHES.gender`, `GenderCode` |
| 옷 정보 출처 | 공통 옷 정보가 어떤 방식으로 생성되었는지 나타냅니다. | `CLOTHES.clothes_info_source`, `ClothesInfoSource` |
| 보유 상태 | 사용자가 해당 옷을 실제 보유했는지, 관심 상품으로 저장했는지 나타냅니다. | `WARDROBE_CLOTHES.ownership_status`, `OwnershipStatus` |
| 스타일 | 서비스에서 정의한 패션 스타일 카탈로그입니다. | `STYLES`, `StyleCode` |
| 사용자 스타일 점수 | 온보딩/마이페이지 선택, 옷장에 등록한 옷, 추천 피드백을 합산한 사용자별 스타일 점수입니다. | `USER_STYLES` |
| 옷 스타일 | 옷 하나에 연결된 스타일 태그입니다. 대표 스타일 1개와 보조 스타일 여러 개를 가질 수 있습니다. | `CLOTHING_STYLES` |
| 옷 색상 | 옷 하나에 연결된 색상 태그입니다. 대표 색상 1개와 보조 색상 여러 개를 가질 수 있습니다. | `CLOTHING_COLORS` |
| 추천 피드백 | 추천된 옷에 대한 싫어요 또는 추천 제외 기록입니다. 추천 점수와 제외 정책에 사용합니다. | `RECOMMENDATION_FEEDBACKS` |
| 코디북 | 사용자별 코디 저장 공간입니다. 사용자는 1개의 코디북을 가집니다. | `OUTFIT_BOOKS` |
| 코디 | 여러 옷을 조합한 착장입니다. 보유 옷과 미보유 옷 모두 코디 구성에 포함될 수 있습니다. | `OUTFITS`, `OUTFIT_ITEMS` |
| 룩피드 | 사용자가 코디를 기반으로 게시글을 올리고 반응할 수 있는 커뮤니티 기능입니다. | `FEED_POSTS`, `FEED_COMMENTS`, `FEED_LIKES` |
| 외부 출처 | 외부 쇼핑몰 또는 사용자가 직접 입력한 상품 출처입니다. | `ExternalSource` |

## 주요 코드값

### 옷 정보 출처

| 코드 | 표시명 | 의미 |
| --- | --- | --- |
| `PHOTO` | 사진 기반 등록 | 사용자가 직접 촬영하거나 업로드한 옷 사진을 기반으로 등록 |
| `PURCHASE_HISTORY` | 구매 내역 기반 등록 | 사용자가 구매내역 캡처 이미지를 업로드해 등록 |
| `EXTERNAL_SHOPPING` | 외부 쇼핑몰 등록 | 네이버쇼핑 등 외부 쇼핑몰 상품 정보를 기반으로 등록 |

### 보유 상태

| 코드 | 표시명 | 의미 |
| --- | --- | --- |
| `OWNED` | 보유 | 사용자가 실제로 보유한 옷 |
| `WISHLIST` | 미보유 | 아직 보유하지 않았지만 관심 상품으로 저장한 옷 |

### 옷 대상 성별

옷 대상 성별은 내부 분류/추천용 code입니다. 사용자 화면 표시명은 관리하지 않으며 FE 화면에도 노출하지 않습니다.

| 코드 | 의미 |
| --- | --- |
| `MALE` | 남성 대상 옷 |
| `FEMALE` | 여성 대상 옷 |
| `UNISEX` | 남녀 공용 또는 대상 성별을 특정하기 어려운 옷 |

### 옷 계절

옷 계절은 `CLOTHES.season`에 저장하는 공통 옷 정보입니다. 옷 등록 시 1개만 선택하며, 생성된 옷의 계절은 수정하지 않습니다.

| 코드 | 표시명 | 의미 |
| --- | --- | --- |
| `SPRING` | 봄 | 봄에 주로 착용하는 옷 |
| `SUMMER` | 여름 | 여름에 주로 착용하는 옷 |
| `FALL` | 가을 | 가을에 주로 착용하는 옷 |
| `WINTER` | 겨울 | 겨울에 주로 착용하는 옷 |
| `ALL_SEASON` | 사계절 | 특정 계절에 한정하지 않는 옷 |

### 스타일/색상 역할

| 코드 | 의미 |
| --- | --- |
| `PRIMARY` | 대표 스타일 또는 대표 색상 |
| `SECONDARY` | 보조 스타일 또는 보조 색상 |

### 옷 카테고리

| 코드 | 표시명 |
| --- | --- |
| `TOP` | 상의 |
| `BOTTOM` | 하의 |
| `OUTER` | 아우터 |
| `SHOES` | 신발 |

### 색상

| 코드 | 표시명 | HEX |
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

### 스타일

| 코드 | 표시명 | 설명 |
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

### 외부 출처

| 코드 | 표시명 | 그룹 |
| --- | --- | --- |
| `NAVER_SHOPPING` | 네이버쇼핑 | 오픈마켓 |
| `COUPANG` | 쿠팡 | 오픈마켓 |
| `MUSINSA` | 무신사 | 패션 플랫폼 |
| `ABLY` | 에이블리 | 패션 플랫폼 |
| `ZIGZAG` | 지그재그 | 패션 플랫폼 |
| `TWENTYNINE_CM` | 29CM | 패션 플랫폼 |
| `WCONCEPT` | W컨셉 | 패션 플랫폼 |
| `BRANDI` | 브랜디 | 패션 플랫폼 |
| `UNIQLO` | 유니클로 | SPA |
| `SPAO` | 스파오 | SPA |
| `EIGHT_SECONDS` | 에잇세컨즈 | SPA |
| `HM` | H&M | SPA |
| `CUSTOM` | 직접입력 | 직접 입력 |

## 옷 타입

| 카테고리 | 코드 |
| --- | --- |
| `TOP` | `LONG_SLEEVE`, `SHORT_SLEEVE`, `SHIRT`, `HOODIE`, `SWEAT`, `COLLAR_TEE`, `SLEEVELESS`, `KNIT` |
| `BOTTOM` | `DENIM`, `TRAINING`, `COTTON`, `SLACKS`, `SHORTS`, `CARGO`, `SKIRT` |
| `OUTER` | `WINDBREAKER`, `HOOD_ZIPUP`, `TRAINING_JACKET`, `BLOUSON`, `MA1`, `VARSITY_JACKET`, `LEATHER_JACKET`, `SHEARLING`, `FLEECE_JACKET`, `VEST`, `WORK_JACKET`, `DENIM_JACKET`, `BLAZER`, `COACH_JACKET`, `PADDING`, `LIGHT_PADDING`, `SINGLE_COAT`, `DOUBLE_COAT`, `BALMACAAN_COAT`, `TTEOKBOKKI_COAT` |
| `SHOES` | `SNEAKERS`, `SPORTS_SHOES`, `LOAFER`, `DERBY`, `BOOTS`, `SANDALS_SLIPPERS`, `FLAT`, `HEEL` |
