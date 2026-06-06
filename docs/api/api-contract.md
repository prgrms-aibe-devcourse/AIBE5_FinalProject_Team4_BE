---
doc_type: be_api_contract
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-03
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
- 보호 API는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.
- 사용자별 리소스는 JWT의 사용자 ID와 path의 `userId`가 일치해야 합니다.

## 이미지 업로드 기준

- 옷 사진과 구매내역 캡처는 multipart form-data로 업로드합니다.
- 요청 part 이름은 `file`입니다.
- 이미지 파일 크기는 10MB 이하입니다.
- 이미지 저장은 AWS S3 기준으로 관리합니다.

## 엔드포인트 인덱스

### 인증

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/oauth2/authorization/{provider}` | OAuth 로그인 시작 |

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
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 보유 옷 기준 추천 조회 |

### 미보유 옷

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 목록 조회 |
| GET | `/api/users/{userId}/wishlist-clothes/favorites` | 즐겨찾기 미보유 옷 조회 |
| POST | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 저장 |
| PATCH | `/api/clothes/{clothesId}/convert-to-owned` | 미보유 옷을 보유 옷으로 전환 |

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
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/save` | 구매내역 기반 옷 저장 |

### 외부 상품

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/naver/search` | 네이버쇼핑 상품 검색 |
| POST | `/api/v1/external/clothes/naver` | 네이버쇼핑 상품을 공통 옷 정보로 저장 |

### 추천

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/similar-products` | 유사 상품 추천 조회 |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 보유 옷 기준 추천 조회 |
| GET | `/api/v1/recommendations/{wardrobeId}?currentTemp={temp}` | 취향 기반 상품 추천 |

#### 취향 기반 상품 추천 응답 (RecommendResponse)

```json
{
  "success": true,
  "data": [
    {
      "title": "상품명",
      "link": "https://...",
      "imageUrl": "https://...",
      "price": "25000",
      "score": "95",
      "reason": "현재 기온(20도)에 적합하며, 선호하시는 미니멀 스타일의 슬랙스입니다."
    }
  ],
  "message": null
}
```

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

### 이미지

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/images/clothes/{userId}/{filename}` | 옷 이미지 조회 |
| GET | `/api/v1/images/purchase-captures/{userId}/{filename}` | 구매내역 캡처 이미지 조회 |

## API 변경 규칙

- API 경로, 요청 필드, 응답 필드가 바뀌면 이 문서를 같은 PR에서 수정합니다.
- FE가 사용하는 API 변경은 FE 담당자와 동기화 이슈 또는 PR에서 확인합니다.
- 공통 응답 형식에 필드가 추가되면 성공/실패 응답 예시를 함께 수정합니다.
