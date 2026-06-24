---
doc_type: be_architecture
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-23
---

# 패키지 구조

이 문서는 BE 코드의 패키지 책임과 구현 위치를 정리합니다. 새 기능을 추가할 때 기존 도메인 경계를 우선 따릅니다.

## 루트 구조

```text
src/main/java/com/closetnangam/be/
├── ClosetnangamApplication.java
├── domain/
└── global/
```

| 패키지 | 역할 |
| --- | --- |
| `domain` | 사용자, 옷장, 옷, 추천, 코디, 피드 등 비즈니스 도메인 |
| `global` | 인증, 공통 응답/예외, 설정, 외부 연동, 저장소 등 전역 인프라 |

## 도메인 패키지

```text
domain/
├── ai/
├── catalog/
├── clothes/
├── feed/
├── outfit/
├── purchase/
├── recommendation/
├── user/
└── wardrobe/
```

| 패키지 | 책임 |
| --- | --- |
| `domain.ai` | AI 분석 상태, 의류 이미지 분석 이력, AI 분석 서비스 |
| `domain.catalog` | 카테고리, 옷 타입, 색상, 스타일, 외부 출처 카탈로그 |
| `domain.clothes` | 공통 옷 정보, 옷장 등록 옷, 사진 기반 등록, 미보유 옷, 옷 추천 보조 로직 |
| `domain.feed` | 룩피드 게시글, 댓글/대댓글, 좋아요, 팔로우, 피드 서비스 |
| `domain.outfit` | 코디북, 코디, 코디 구성 옷 |
| `domain.purchase` | 구매내역 캡처 업로드, AI 분석, 구매내역 기반 옷 등록 |
| `domain.recommendation` | 유사 상품 추천, 외부 상품 추천 provider |
| `domain.user` | 사용자, 소셜 계정, 프로필, 사용자 스타일 선호 |
| `domain.wardrobe` | 사용자 옷장 생성, 조회, 통계 |

## 전역 패키지

```text
global/
├── auth/
├── common/
├── config/
├── external/
└── storage/
```

| 패키지 | 책임 |
| --- | --- |
| `global.auth` | JWT, OAuth2 로그인, 인증 사용자 처리 |
| `global.common` | 공통 응답, 공통 예외, 공통 validation |
| `global.config` | Security, Swagger, Redis, RestTemplate, WebMvc, Storage 설정 |
| `global.external` | 네이버쇼핑, 날씨, Gemini 등 외부 서비스 연동 |
| `global.storage` | 이미지 저장과 이미지 접근 처리 |

## 계층 구조

도메인 패키지는 필요에 따라 아래 하위 패키지를 사용합니다.

| 하위 패키지 | 역할 |
| --- | --- |
| `controller` | HTTP 요청/응답, 인증 사용자 검증, service 호출 |
| `service` | 유스케이스 처리, 트랜잭션, 도메인 규칙 적용 |
| `repository` | JPA repository |
| `entity` | JPA entity와 도메인 상태 |
| `dto` | API 요청/응답 DTO |
| `enums` | 도메인 코드값 |
| `helper`, `scoring`, `constant` | 특정 도메인 내부에서만 쓰는 보조 로직 |

## 구현 원칙

- 사용자별 리소스는 controller 또는 service 초입에서 인증 사용자와 요청 대상 사용자를 검증합니다.
- 공통 응답은 `ApiResponse<T>`를 사용합니다.
- 도메인 enum은 [도메인 용어집](../domain/glossary.md)의 코드값과 일치해야 합니다.
- 공통 옷 정보와 사용자 옷장 정보는 분리합니다.
- 외부 API 호출 실패는 일반 서버 오류와 구분해 처리합니다.
- 파일 업로드와 이미지 접근 정책은 `global.storage`를 기준으로 모읍니다.
- 새로운 외부 연동은 `global.external`에 두고, 도메인 저장 유스케이스는 해당 `domain` service에서 처리합니다.

## 패키지별 변경 기준

| 변경 내용 | 우선 위치 |
| --- | --- |
| 카테고리/색상/스타일 값 추가 | `domain.catalog.enums`, `domain.catalog.service` |
| 옷 등록/수정/삭제 규칙 | `domain.clothes.service` |
| 사용자 옷장 생성/통계 | `domain.wardrobe.service` |
| 구매내역 캡처 분석 | `domain.purchase.service` |
| 유사 상품 추천 | `domain.recommendation.service` |
| 이미지 저장 방식 | `global.storage`, `global.config` |
| 인증/인가 | `global.auth`, `global.config.SecurityConfig` |
| 공통 오류 처리 | `global.common.exception` |
