---
doc_type: shared
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-23
---

# 기능 인덱스

이 문서는 [요구사항 정의서](./requirements-definition.md)의 세부기능 ID를 API, 데이터, FE 화면과 연결하기 위한 빠른 참조 문서입니다.

기능 범위, 우선순위, 세부 설명의 원본은 [requirements-definition.md](./requirements-definition.md)입니다. 이 문서는 별도의 기능 ID를 새로 정의하지 않으며, 요구사항 정의서의 세부기능 ID를 그대로 사용합니다.

구현 진행 상태는 이슈와 PR에서 관리합니다. 테스트용 API나 개발 편의 기능은 공식 사용자 기능으로 보지 않으며, 필요한 경우 [api-contract.md](../api/api-contract.md) 또는 [implementation-gaps.md](../backend/implementation-gaps.md)에서 경계를 명확히 합니다.

## 기능 연결표

| 세부기능 ID | 구분 | 기능 | 설명 | 주요 BE API/데이터 | FE 화면/라우트 |
| --- | --- | --- | --- | --- | --- |
| `AUTH-001`~`AUTH-007` | 로그인 | 소셜 로그인/인증 처리 | 카카오, 구글, 네이버 OAuth 로그인, JWT 발급/저장, 사용자 ID 검증 | `USERS`, `SOCIAL_ACCOUNTS`, `/oauth2/**`, JWT | 로그인 모달 |
| `ONBOARD-001`~`ONBOARD-010` | 온보딩 | 최초 사용자 정보/스타일 입력 | 닉네임, 생년월일, 성별, 지역, 스타일 선택, 사용자 공간 생성, 첫 옷 등록 안내 | `USERS`, `USER_STYLES`, `WARDROBES`, `OUTFIT_BOOKS` | 온보딩 |
| `MYPAGE-001`~`MYPAGE-005` | 마이페이지 | 프로필/계정 관리 | 내 정보 조회/수정, 소셜 계정 확인, 룩피드 프로필 외부 링크 관리, 로그아웃, 회원 탈퇴 | `USERS`, `SOCIAL_ACCOUNTS`, `USER_EXTERNAL_LINK`, `USER_STYLES` | 마이페이지, 룩피드 프로필 |
| `STYLE-001` | 스타일 | 선호 점수 반영 | 온보딩/마이페이지에서 선택한 선호 스타일을 추천에 반영 | `USER_STYLES.preference_weight` | 온보딩, 마이페이지 |
| `STYLE-002` | 스타일 | 옷장 점수 반영 | 옷장에 등록한 옷의 대표/보조 스타일을 추천에 반영 | `USER_STYLES.wardrobe_weight`, `CLOTHING_STYLES` | 옷장, 추천 |
| `STYLE-003` | 스타일 | 피드백 점수 반영 | 추천 싫어요와 추천 제외 피드백을 추천에 반영 | `USER_STYLES.feedback_weight`, `RECOMMENDATION_FEEDBACKS` | 추천 상세 |
| `WARDROBE-001` | 옷장 | 단일 옷장 | 사용자별 하나의 디지털 옷장 제공 | `WARDROBES`, `GET /api/v1/wardrobes/users/{userId}` | 옷장 |
| `WARDROBE-002` | 옷장 | 보유/미보유 통계 조회 | 보유 옷과 미보유 옷 수를 함께 집계하고 옷 종류별 통계 조회 | `GET /api/v1/wardrobes/users/{userId}/statistics`, `WARDROBE_CLOTHES` | 옷장 요약 |
| `WARDROBE-003`~`WARDROBE-010` | 옷장 | 보유/미보유 옷 관리 | 보유 목록, 상세, 수정, 삭제, 즐겨찾기, 미보유 목록, 보유 전환. 옷 수정은 생성된 공통 옷의 계절을 변경하지 않음 | `CLOTHES`, `WARDROBE_CLOTHES`, `GET /api/v1/users/{userId}/clothes`, `PATCH /api/v1/clothes/{clothesId}`, `DELETE /api/v1/clothes/{clothesId}`, `/api/users/{userId}/wishlist-clothes` | 옷장, 옷 상세 |
| `WARDROBE-011`~`WARDROBE-016` | 옷 등록 | 구매내역 기반 등록 | 구매내역 캡처 업로드, AI 상품 추출, 결과 편집, 계절 선택, 보유 저장 | `/api/v1/users/{userId}/clothes/purchase-captures/**`, `PURCHASE_HISTORY` | 옷 등록 |
| `WARDROBE-017`~`WARDROBE-030` | 옷 등록 | 사진 기반 등록 | 옷 사진 업로드, AI 판별, 결과 표시/수정, 계절 선택, 최종 저장, 성공/실패 알림 | `/api/v1/users/{userId}/clothes/photos/**`, `PHOTO` | 옷 등록 |
| `CATALOG-001` | 카탈로그 | 공통 카탈로그 | 카테고리, 타입, 색상, 스타일, 외부 쇼핑몰 출처 code와 표시값 제공. 계절 code 기준은 카탈로그 문서에서 관리 | `GET /api/v1/categories`, `StyleCode`, `ColorCode`, `GenderCode` | 옷 등록, 필터 |
| `RECO-001` | 추천 | OOTD 코디 | 사용자 기준 코디 추천. 날씨, 계절, 온도는 보조 조건으로 반영 | `GET /api/v1/ootd/{wardrobeId}`, `WeatherCompatibilityTable` | 추천 메인 |
| `RECO-002` | 추천 | 취향 분석/스타일 기반 추천 | 사용자 취향, 옷장, 지역, 날씨 등 데이터를 추천 근거로 활용 | `GET /api/v1/recommendations/{wardrobeId}`, `USER_STYLES`, `RECOMMENDATION_FEEDBACKS` | 추천 메인 |
| `RECO-003` | 추천 | 유사 상품 탐색 | 선택한 상품이나 옷과 유사한 상품을 이미지, 카테고리, 색상, 스타일 기준으로 탐색 | `GET /api/v1/users/{userId}/clothes/{clothesId}/similar-products` | 옷 상세, 추천 상세 |
| `RECO-004` | 추천 | 어울리는 옷 추천 | 사용자 옷장 데이터를 기준으로 함께 입기 좋은 상의, 하의, 아우터, 신발 추천 (`limitPerCategory` query, 기본 `5`, 허용 `1`~`50`) | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 추천 상세 |
| `RECO-005` | 추천 | AI MD 추천 | 성별과 관계없이 선택한 AI MD가 사용자 성별 기준 옷/상품 추천과 이유를 제공 | `/api/v1/users/{userId}/recommendations/ai-md/**`, `OUTFITS`, `OUTFIT_ITEMS`, Gemini, 네이버쇼핑 | 추천 상세, 코디북 |
| `RECO-006` | 추천 | 외부 API 오류 안내 | 날씨, 상품, AI 등 외부 API 오류 시 기본 추천 또는 대체 행동 안내 | 외부 API 오류 응답, 공통 오류 응답 | 추천 메인, 추천 상세 |
| `RECO-007`~`RECO-011` | 추천 상세 | 추천 결과 상세/액션 | 추천 상품/코디 상세, 코디 저장, 미보유 저장, 구매 링크 | `OUTFITS`, `OUTFIT_ITEMS`, `POST /api/users/{userId}/wishlist-clothes` | 추천 상세 |
| `RECO-012`~`RECO-013` | 추천 상세 | 추천 피드백/제외 | 추천 결과에 사용자별 긍정/부정 피드백을 저장하고 추천 정책에 반영 | `POST /api/v1/users/{userId}/recommendations/feedback`, `RECOMMENDATION_FEEDBACKS`, `USER_STYLES.feedback_weight` | 추천 상세 |
| `OUTFIT-001`~`OUTFIT-005` | 코디 | 코디 저장/조회/수정/삭제/상태 처리 | 직접 저장하거나 추천받은 코디를 저장, 조회, 수정, 삭제하고 빈 상태를 안내 | `OUTFITS`, `OUTFIT_ITEMS` | 코디북, 코디 상세 |
| `OUTFITBOOK-001`~`OUTFITBOOK-003` | 코디북 | 코디북 관리 | 사용자별 단일 코디북, 코디 목록 조회, 코디북 상세 | `OUTFIT_BOOKS`, `/api/v1/outfit-books` | 코디북 |
| `EXT-001`~`EXT-002` | 외부 연동 | 네이버쇼핑 상품 연동 | 외부 상품 상세 조회, 상품 저장 | `POST /api/v1/external/clothes/naver` | 추천 상세 |
| `EXT-003`~`EXT-004` | 외부 연동 | 날씨 보조 정보 | 사용자 지역 기반 현재 날씨 조회. 독립 추천 기능이 아니라 추천 보조 조건 | `GET /api/weather` | 추천 메인 |
| `EXT-005`~`EXT-006` | 외부 연동 | AI 의류 분석/추천 설명 | 옷 사진 분석, 스타일링 설명 또는 추천 이유 생성 | Gemini API | 옷 등록, 추천 상세 |
| `SYSTEM-001`~`SYSTEM-004` | 공통 시스템 | 로딩/에러 | 로딩, 404/500/네트워크 오류 안내 | 공통 오류 응답 | 공통 UI |
| `SYSTEM-005` | 공통 시스템 | 통합 필터 | 카탈로그 기준값을 활용한 목록 필터링 | `GET /api/v1/categories`, 카탈로그 code | 옷장, 추천, 목록 필터 |
| `SYSTEM-006`~`SYSTEM-007` | 공통 시스템 | 서비스 안내 | 서비스 소개와 도움말/가이드 투어 안내. 페이지별 투어 완료 상태를 BE에서 관리하며 로그인 후 미완료 시 자동 노출 | `GET /api/v1/users/profile` (guideTourCompleted* 필드), `PATCH /api/v1/users/guide-tour`, `USERS.guide_tour_completed_home/wardrobe/feed/mypage/outfit_book`, `docs/planning/project-plan.md`, `docs/legal/terms.md` | 홈, 옷장, 피드, 마이페이지, 코디북 |
| `SYSTEM-008`~`SYSTEM-009` | 공통 시스템 | 필수 약관 동의 | 이용약관과 개인정보 처리방침을 확인하고 회원가입 시 자동 동의 기준으로 처리 | `GET /api/v1/legal/terms`, `GET /api/v1/legal/privacy-policy`, `docs/legal/terms.md`, `docs/legal/privacy-policy.md` | 로그인 모달, 온보딩 |
| `SYSTEM-010` | 공통 시스템 | 마케팅 정보 수신 동의 | 선택 동의로 관리하며 온보딩/마이페이지에서 조회 및 변경 | `GET /api/v1/users/{userId}/marketing-consent`, `PATCH /api/v1/users/{userId}/marketing-consent`, `GET /api/v1/legal/marketing-consent`, `USERS.marketing_agreed`, `USERS.marketing_agreed_at`, `docs/legal/marketing-consent.md` | 온보딩, 마이페이지 |
| `SYSTEM-011` | 공통 시스템 | 작성 중 이탈 확인 | 기본 정보 수정, 선호 스타일 수정, 프로필 이미지 변경, 룩피드 프로필 수정, 피드 작성/수정, 코디 작성/수정 등 작성 중 이탈 시 작성 내용 손실을 안내 | 공통 UI guard | 마이페이지, 피드, 코디, 작성/수정 화면 |
| `DATA-001` | 데이터 관리 | 초기 상품 데이터 | 추천 기능 시연을 위한 DB/API 초기 데이터 준비 | 샘플 상품 데이터 | 추천 |
| `FEED-001`~`FEED-008` | 룩피드 | 피드/반응/빈 상태 | 코디 공유, 상세, 좋아요, 댓글, 대댓글, 팔로우, 빈 상태 | `FEED_POSTS`, `FEED_POST_IMAGES`, `FEED_LIKES`, `FEED_COMMENTS`, `FEED_POST_SAVES`, `USER_FOLLOWS`, `POST /api/v1/feed/posts/{postId}/saves` | 룩피드 |
| `OUTFIT-001` | 코디 | 코디 저장 | 룩피드·추천 등에서 코디북 저장 | `POST /api/v1/feed/posts/{postId}/saves`, `POST /api/v1/outfit-books/{bookId}/outfits` | 룩피드, 코디북 |

## ID 운영 규칙

- 요구사항 ID와 세부기능 ID의 원본은 [requirements-definition.md](./requirements-definition.md)입니다.
- 이 문서는 세부기능 ID를 API, 데이터, 화면과 연결하는 인덱스 역할만 합니다.
- 신규 기능 또는 기능 범위 변경은 요구사항 정의서를 먼저 수정하거나 같은 PR에서 함께 수정합니다.
- 한 기능이 여러 API를 사용하더라도 사용자 가치와 요구사항 ID가 같으면 같은 세부기능 ID 또는 ID 범위로 묶습니다.
- FE 문서에는 같은 세부기능 ID를 사용해 화면/라우트/컴포넌트 정보를 연결합니다.
