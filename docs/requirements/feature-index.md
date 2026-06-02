---
doc_type: shared
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-03
---

# 기능 인덱스

이 문서는 옷장난감의 MVP 기능을 하나의 기능 ID로 추적하기 위한 기준 문서입니다. 기능 ID는 기획, API, DB, FE 화면, PR 리뷰에서 같은 기능을 가리키는 공통 언어로 사용합니다.

구현 진행 상태는 이슈와 PR에서 관리합니다. 이 문서에는 테스트용 API나 개발 편의 기능을 포함하지 않고, 사용자에게 제공되는 서비스 기능과 도메인 기준만 정리합니다.

## 기능 목록

| F-ID | 구분 | 기능 | 설명 | 주요 BE API/데이터 | FE 화면/라우트 |
| --- | --- | --- | --- | --- | --- |
| AUTH-001 | 회원 | 소셜 로그인 | 카카오, 구글, 네이버 OAuth 로그인 후 JWT 발급 | `USERS`, `SOCIAL_ACCOUNTS`, `/oauth2/**` | 로그인 모달 |
| USER-001 | 회원 | 기본 정보 입력 | 최초 로그인 후 닉네임, 생년월일, 성별, 지역 입력 | `USERS` | 온보딩 |
| USER-002 | 회원 | 프로필/마이페이지 | 프로필, 스타일, 소셜 계정, 외부 링크, 회원 탈퇴 관리 | `USERS`, `USER_EXTERNAL_LINK`, `USER_STYLES` | 마이페이지 |
| STYLE-001 | 스타일 | 스타일 카탈로그 | 서비스에서 사용하는 스타일 목록 관리 | `STYLES`, `StyleCode` | 온보딩, 마이페이지, 필터 |
| STYLE-002 | 스타일 | 사용자 스타일 점수 | 온보딩/마이페이지 선택, 옷장 데이터, 추천 피드백 점수 누적 | `USER_STYLES` | 온보딩, 마이페이지 |
| WARD-001 | 옷장 | 사용자별 단일 옷장 | 회원마다 1개의 디지털 옷장 생성 및 조회 | `WARDROBES`, `GET /api/v1/wardrobes/users/{userId}` | 옷장 |
| WARD-002 | 옷장 | 옷장 통계 | 사용자 옷장에 등록된 보유/미보유 옷 통계 조회 | `GET /api/v1/wardrobes/users/{userId}/statistics` | 옷장 요약 |
| CLOTH-001 | 옷 | 보유 옷 등록 | 사용자가 실제 보유한 옷을 옷장에 등록 | `POST /api/v1/users/{userId}/clothes`, `OWNED` | 옷 등록 |
| CLOTH-002 | 옷 | 보유 옷 조회 | 사용자 옷장에 연결된 보유 옷 목록/상세 조회 | `GET /api/v1/users/{userId}/clothes`, `GET /api/v1/clothes/{clothesId}` | 옷장 |
| CLOTH-003 | 옷 | 옷 수정 | 사이즈, 계절, 즐겨찾기 등 사용자별 옷장 정보를 수정 | `PATCH /api/v1/clothes/{clothesId}` | 옷 상세/수정 |
| CLOTH-004 | 옷 | 옷장 연결 삭제 | 사용자 옷장에서 옷 연결만 제거하고 공통 옷 정보는 유지 | `DELETE /api/v1/clothes/{clothesId}`, `WARDROBE_CLOTHES` | 옷 상세 |
| CLOTH-005 | 옷 | 미보유 옷 저장 | 추천/외부 상품을 관심 상품으로 옷장에 저장 | `POST /api/users/{userId}/wishlist-clothes`, `WISHLIST` | 추천 상세, 옷장 |
| CLOTH-006 | 옷 | 미보유 옷 조회 | 미보유 옷 목록 및 즐겨찾기 조회 | `GET /api/users/{userId}/wishlist-clothes` | 옷장 미보유 탭 |
| CLOTH-007 | 옷 | 미보유에서 보유 전환 | 구매한 관심 상품을 보유 옷으로 전환 | `PATCH /api/clothes/{clothesId}/convert-to-owned` | 옷 상세 |
| REG-001 | 옷 등록 | 사진 기반 등록 | 사용자가 옷 사진을 올리고 AI 분석 결과를 바탕으로 옷 등록 | `/api/v1/users/{userId}/clothes/photos/**`, `PHOTO` | 옷 등록 |
| REG-002 | 옷 등록 | 구매내역 기반 등록 | 구매내역 캡처 이미지를 분석해 옷 후보를 만들고 저장 | `/api/v1/users/{userId}/clothes/purchase-captures/**`, `PURCHASE_HISTORY` | 옷 등록 |
| REG-003 | 옷 등록 | 외부 쇼핑몰 상품 저장 | 외부 쇼핑몰 상품 정보를 공통 옷 정보로 저장 | `POST /api/v1/external/clothes/naver`, `EXTERNAL_SHOPPING` | 추천 상세, 외부 검색 |
| CATALOG-001 | 카탈로그 | 카테고리/타입/색상/스타일 조회 | 옷 등록과 필터에 필요한 기준값 제공 | `GET /api/v1/categories` | 옷 등록, 필터 |
| RECO-001 | 추천 | 추천 메인 | 메인 페이지를 추천 중심 화면으로 사용 | 추천 도메인 | 추천 메인 |
| RECO-002 | 추천 | 취향 기반 상품 추천 | 사용자 스타일 점수를 기반으로 상품 추천 | `USER_STYLES`, `RECOMMENDATION_FEEDBACKS` | 추천 메인 |
| RECO-003 | 추천 | 유사 상품 추천 | 선택한 옷과 유사한 상품 추천 | `GET /api/v1/users/{userId}/clothes/{clothesId}/similar-products` | 옷 상세, 추천 상세 |
| RECO-004 | 추천 | 보유 옷 기준 상품 추천 | 보유 옷과 어울리는 상의/하의/아우터/신발 추천 | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 추천 상세 |
| RECO-005 | 추천 | 날씨/계절/지역 기반 추천 | 사용자 지역과 날씨 정보를 추천에 반영 | `GET /api/weather` | 추천 메인 |
| RECO-006 | 추천 | AI MD 추천 | 추천 이유와 스타일링 설명을 AI가 생성 | Gemini 연동 | 추천 상세 |
| RECO-007 | 추천 | 추천 싫어요/제외 | 비선호 상품에 피드백을 남기고 추천에서 제외 | `RECOMMENDATION_FEEDBACKS` | 추천 상세 |
| OUTFIT-001 | 코디북 | 사용자별 단일 코디북 | 회원마다 1개의 코디북 생성 및 관리 | `OUTFIT_BOOKS`, `/api/v1/outfit-books` | 코디북 |
| OUTFIT-002 | 코디 | 코디 저장/조회 | 추천받거나 직접 구성한 코디를 저장하고 조회 | `OUTFITS`, `OUTFIT_ITEMS` | 코디북, 코디 상세 |
| OUTFIT-003 | 코디 | 코디 스타일 | 코디 구성 옷을 기반으로 대표/보조 스타일 관리 | `OUTFIT_STYLES` | 코디 상세 |
| FEED-001 | 룩피드 | 피드 게시글 | 코디 기반 게시글 등록, 목록, 상세 조회 | `FEED_POSTS`, `FEED_POST_IMAGES` | 룩피드 |
| FEED-002 | 룩피드 | 피드 반응 | 좋아요, 댓글/대댓글, 저장, 팔로우 처리 | `FEED_LIKES`, `FEED_COMMENTS`, `FEED_POST_SAVES`, `USER_FOLLOWS` | 룩피드 |
| EXT-001 | 외부 연동 | 네이버쇼핑 검색 | 네이버쇼핑 API를 활용한 상품 검색 | `GET /api/naver/search` | 외부 상품 검색 |
| EXT-002 | 외부 연동 | 이미지 저장 | 옷 사진, 구매내역 캡처, 피드 이미지 저장 | AWS S3 기준, `/api/v1/images/**` | 이미지 업로드/표시 |

## 기능 ID 운영 규칙

- 신규 기능은 기존 구분 prefix를 우선 사용합니다.
- 한 기능이 여러 API를 사용하더라도 사용자 가치가 하나라면 하나의 F-ID로 묶습니다.
- 기능 범위가 바뀌면 이 문서를 먼저 수정하거나 같은 PR에서 함께 수정합니다.
- FE 문서에는 같은 F-ID를 사용해 화면/라우트/컴포넌트 정보를 연결합니다.
