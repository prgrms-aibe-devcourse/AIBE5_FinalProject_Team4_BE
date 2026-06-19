---
doc_type: be_api_contract
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-15
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
- Access Token은 `access_token` HttpOnly 쿠키로 전달됩니다.
- Refresh Token은 `refresh_token` HttpOnly 쿠키로 전달되며 `/api/v1/auth` 경로에서만 전송됩니다.
- Access Token이 만료(401)되면 `POST /api/v1/auth/refresh`를 호출해 재발급합니다.
- 로그아웃 시 `POST /api/v1/auth/logout`을 호출해 서버에서 Refresh Token을 삭제합니다.
- 탈퇴 후 30일 이내 계정으로 OAuth 로그인을 시도하면 자동 로그인하지 않고 FE에 복구 확인 상태를 전달합니다. 사용자가 복구를 확정하면 `POST /api/v1/auth/restore-withdrawn`으로 계정을 복구하고 인증 쿠키를 발급합니다.
- 사용자별 리소스는 JWT의 사용자 ID와 path의 `userId`가 일치해야 합니다.

## 이미지 업로드 기준

- 옷 사진, 구매내역 캡처, 피드 이미지, 프로필 이미지는 multipart form-data로 업로드합니다.
- 요청 part 이름은 `file`입니다.
- 이미지 파일 크기는 10MB 이하입니다.
- 이미지 저장은 AWS S3 기준으로 관리합니다.

## 엔드포인트 인덱스

### 인증

| Method | Path                               | 설명              |
|--------|------------------------------------|-----------------|
| GET    | `/oauth2/authorization/{provider}` | OAuth 로그인 시작    |
| POST   | `/api/v1/auth/refresh`             | Access Token 재발급 |
| POST   | `/api/v1/auth/logout`              | 로그아웃            |
| POST   | `/api/v1/auth/restore-withdrawn`   | 탈퇴 계정 복구 확정 |

### 사용자
| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/profile` | 현재 로그인한 사용자 본인 프로필 반환 (응답 필드는 아래 표 참고) |
| GET | `/api/v1/users/profile/{userId}` | 사용자 프로필 상세 조회 |
| GET | `/api/v1/users/nickname/check` | 닉네임 규칙 및 중복 여부 확인 |
| PATCH | `/api/v1/users/profile` | 프로필 저장 (온보딩/마이페이지 공통). 저장 후 본인 프로필 반환 |
| POST | `/api/v1/users/profile/image` | 프로필 이미지 업로드 (`multipart/form-data`, field: `file`) |
| POST | `/api/v1/users/onboarding` | 온보딩 완료 저장. 프로필, 선호 스타일, 마케팅 동의 여부를 하나의 트랜잭션으로 저장 |
| POST | `/api/v1/users/styles` | 스타일 선호도 저장 (사용자별 전체 스타일 row 보장, preference_weight만 갱신) |
| DELETE | `/api/v1/users/me` | 회원 탈퇴 (소프트 삭제, 쿠키 만료) |

#### GET /api/v1/users/profile 응답 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | Long | 사용자 ID |
| `email` | String | 사용자 계정 이메일. 소셜 로그인에서 확인된 이메일을 조회용으로 반환 |
| `nickname` | String? | 온보딩/마이페이지에서 사용자가 설정한 닉네임. 온보딩 완료 전에는 `null`일 수 있음 |
| `onboarded` | boolean | 온보딩 완료 여부. 닉네임, 사용자 성별, 생년월일, 지역 코드, 선호 스타일이 모두 저장되면 true |
| `birthDate` | Date? | 생년월일. 온보딩 완료 전에는 `null`일 수 있음 |
| `gender` | String | 사용자 성별. `MALE` / `FEMALE` / `OTHER` |
| `regionName` | String | 지역명 |
| `regionCode` | String | 지역 코드 |
| `profileImageUrl` | String | 프로필 이미지 URL |
| `profileBio` | String | 한 줄 소개 |
| `externalLinkUrl` | String | 외부 링크 URL |
| `styleCodes` | String[] | 선호 스타일 code 배열 |
| `socialProviders` | String[] | 연결된 소셜 로그인 제공자 목록 |
| `socialAccounts` | Object[] | 연결된 소셜 로그인 제공자와 제공자 이메일 목록. 조회 전용 |

#### GET /api/v1/users/nickname/check 응답 필드

닉네임은 룩피드 프로필 식별에도 사용하므로 전체 회원 기준으로 중복될 수 없습니다. 영문 소문자, 숫자, 마침표(`.`), 밑줄(`_`)만 3~30자로 사용할 수 있으며 처음과 끝은 영문 또는 숫자여야 합니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `nickname` | String | 정규화된 닉네임 |
| `available` | boolean | 사용 가능 여부 |
| `message` | String | 사용 가능 또는 오류 안내 문구 |

#### PATCH /api/v1/users/profile 요청 필드

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `nickname` | Y | 닉네임. 영문 소문자, 숫자, 마침표(`.`), 밑줄(`_`)만 3~30자 |
| `birthDate` | Y | 생년월일 (yyyy-MM-dd) |
| `gender` | Y | `MALE` / `FEMALE` |
| `regionName` | Y | 지역명 (예: 서울) |
| `regionCode` | Y | 지역 코드 |
| `profileImageUrl` | N | 프로필 이미지 URL. 생략 시 기존 값 유지 |
| `profileBio` | N | 한 줄 소개. 생략 시 기존 값 유지 |
| `externalLinkUrl` | N | 외부 링크 URL. 생략 시 기존 값 유지 |

#### PATCH /api/v1/users/profile 응답 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | Long | 사용자 ID |
| `email` | String | 사용자 계정 이메일. 소셜 로그인에서 확인된 이메일을 조회용으로 반환 |
| `nickname` | String | 저장된 닉네임 |
| `onboarded` | boolean | 온보딩 완료 여부. 프로필과 선호 스타일 저장 상태를 함께 기준으로 판단 |
| `birthDate` | Date | 저장된 생년월일 |
| `gender` | String | 저장된 사용자 성별 |
| `regionName` | String | 저장된 지역명 |
| `regionCode` | String | 저장된 지역 코드 |
| `profileImageUrl` | String | 저장된 프로필 이미지 URL |
| `profileBio` | String | 저장된 한 줄 소개 |
| `externalLinkUrl` | String | 저장된 외부 링크 URL |
| `styleCodes` | String[] | 선호 스타일 code 배열 |
| `socialProviders` | String[] | 연결된 소셜 로그인 제공자 목록 |
| `socialAccounts` | Object[] | 연결된 소셜 로그인 제공자와 제공자 이메일 목록. 조회 전용 |

#### POST /api/v1/users/profile/image

프로필 이미지 파일을 업로드하고 프로필 저장에 사용할 이미지 URL을 반환합니다.
반환된 `imageUrl`은 `PATCH /api/v1/users/profile` 요청의 `profileImageUrl`에 전달해 저장합니다.

요청:

```text
Content-Type: multipart/form-data
field: file
```

응답:

```json
{
  "success": true,
  "data": {
    "imageUrl": "http://localhost:8080/api/v1/images/profile/1/sample.jpg"
  },
  "message": null
}
```

#### POST /api/v1/users/onboarding 요청 필드

온보딩 마지막 단계에서 한 번 호출합니다. 프로필, 선호 스타일, 마케팅 정보 수신 동의 여부는 같은 트랜잭션에서 함께 저장되며, 일부 정보만 저장된 상태를 남기지 않습니다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `nickname` | Y | 닉네임. 영문 소문자, 숫자, 마침표(`.`), 밑줄(`_`)만 3~30자 |
| `birthDate` | Y | 생년월일 (yyyy-MM-dd) |
| `gender` | Y | 사용자 성별: `MALE` / `FEMALE` |
| `regionName` | Y | 지역명 |
| `regionCode` | Y | 지역 코드 |
| `styleCodes` | Y | 선호 스타일 code 배열 (2~10개). 배열 순서 기준 첫 번째는 대표 스타일(+7), 나머지는 보조 스타일(+3), 선택하지 않은 스타일은 0점으로 반영합니다. |
| `marketingAgreed` | Y | 마케팅 정보 수신 동의 여부. 선택 동의이므로 `false` 저장 가능 |

#### POST /api/v1/users/onboarding 응답 필드

`GET /api/v1/users/profile` 응답 필드와 동일한 본인 프로필 정보를 반환합니다.

#### POST /api/v1/users/styles 요청 필드

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `styleCodes` | Y | 스타일 코드 배열 (2~10개, 예: `["CASUAL", "MINIMAL"]`). 저장 시 사용자별 전체 스타일 row를 보장하고, 배열 순서 기준 첫 번째는 대표 스타일(+7), 나머지는 보조 스타일(+3), 선택하지 않은 스타일은 0점으로 반영합니다. |

허용 스타일 코드: `CASUAL`, `STREET`, `MINIMAL`, `SPORTY`, `CLASSIC`, `CHIC`, `WORKWEAR`, `CITYBOY`, `GORPCORE`, `RETRO`

### 마케팅 동의

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/{userId}/marketing-consent` | 마케팅 정보 수신 동의 상태 조회 |
| PATCH | `/api/v1/users/{userId}/marketing-consent` | 마케팅 정보 수신 동의 변경 |

#### 마케팅 정보 수신 동의 변경

필수 약관인 이용약관과 개인정보 처리방침은 회원가입 시 자동 동의 기준으로 처리하며, 사용자별 약관 버전/동의 시각은 별도로 저장하지 않습니다. 선택 동의인 마케팅 정보 수신 동의는 `USERS.marketing_agreed`, `USERS.marketing_agreed_at` 기준으로 관리합니다. `marketing_agreed_at`은 동의 상태일 때 실제 동의 시각을 저장하고, 미동의 또는 철회 상태에서는 `NULL`로 관리합니다.

**PATCH** `/api/v1/users/{userId}/marketing-consent`

요청:

```json
{
  "marketingAgreed": true
}
```

응답:

```json
{
  "success": true,
  "data": {
    "marketingAgreed": true
  },
  "message": null
}
```

### 약관

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/legal/terms` | 서비스 이용약관 markdown 원문 조회 |
| GET | `/api/v1/legal/privacy-policy` | 개인정보 처리방침 markdown 원문 조회 |
| GET | `/api/v1/legal/marketing-consent` | 마케팅 정보 수신 동의 markdown 원문 조회 |

약관 원본은 BE `docs/legal/`에 두고, 실제 적용된 버전은 `docs/legal/versions/`에 보관합니다. API는 최신본의 frontmatter를 metadata로 분리하고 markdown 본문을 `content`로 반환합니다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "policyType": "terms",
    "version": "2026.06.15",
    "effectiveDate": "2026-06-15",
    "lastUpdated": "2026-06-15",
    "content": "# 서비스 이용약관\n\n..."
  },
  "message": null
}
```

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
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 옷장 기반 어울리는 옷 추천 조회 (`limitPerCategory` query, 아래 [옷장 기반 어울리는 옷 추천](#옷장-기반-어울리는-옷-추천-get-recommendations) 참고) |

#### 옷 등록/저장 공통 분류 필드

아래 필드는 보유 옷 등록(`POST /api/v1/users/{userId}/clothes`), 사진 저장, 구매내역 저장, 미보유 저장 요청에 공통으로 포함됩니다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `category` | Y | 대분류 code (`TOP`, `BOTTOM`, `OUTER`, `SHOES`) |
| `itemType` | Y | 소분류 code. 선택한 `category` 하위 값 |
| `season` | N | 옷 자체의 대상 계절 code (`SPRING`, `SUMMER`, `FALL`, `WINTER`, `ALL_SEASON`). 생략 시 `ALL_SEASON`으로 저장하며, 최종 저장 후 변경하지 않음 |
| `gender` | Y | 옷 대상 성별 code (`MALE`, `FEMALE`, `UNISEX`). 사용자 화면 표시 대상 아님 |
| `primaryColor` | Y | 대표 색상 code |
| `secondaryColors` | N | 보조 색상 code 배열 |
| `styles` | Y | 스타일 code 배열 (최소 1개) |

허용 code 목록은 [카탈로그 사용 가이드](../domain/catalog.md)를 따릅니다. 저장 요청 시 validation이 적용됩니다. `gender`는 사용자에게 노출하지 않고 옷 분류/추천과 저장 요청에 사용하는 내부 code입니다.

#### 옷 수정 기준

옷 수정(`PATCH /api/v1/clothes/{clothesId}`)은 생성된 공통 옷의 계절을 변경하는 용도로 사용하지 않습니다. 생성 후 `season`을 변경해야 하는 상황은 공식 기준과 충돌하므로 담당자 확인 후 별도 기준 변경으로 처리합니다.

#### 옷 조회 응답 (`ClothesResponse`)

옷 목록/상세/저장 성공 응답에는 분류 필드와 함께 `season`, `gender`가 포함됩니다. `season`은 `SPRING`, `SUMMER`, `FALL`, `WINTER`, `ALL_SEASON` code이고, `gender`는 `MALE`, `FEMALE`, `UNISEX` enum code입니다. FE는 `gender`를 사용자 화면에 표시하지 않고 내부 분류/추천 처리 기준으로만 사용합니다.

```json
{
  "success": true,
  "data": {
    "clothesId": 1,
    "wardrobeClothesId": 10,
    "name": "화이트 반팔 티셔츠",
    "category": "TOP",
    "itemType": "SHORT_SLEEVE",
    "season": "SUMMER",
    "gender": "UNISEX",
    "primaryColor": "WHITE",
    "secondaryColors": [],
    "styles": [
      { "styleCode": "CASUAL", "styleRole": "PRIMARY" }
    ],
    "ownershipStatus": "OWNED",
    "clothesInfoSource": "PHOTO"
  },
  "message": null
}
```

> **Note**: 응답 예시는 주요 필드만 발췌했습니다. 실제 응답에는 옷장/외부 연동 필드가 추가로 포함됩니다.

### 미보유 옷

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 목록 조회 |
| GET | `/api/users/{userId}/wishlist-clothes/favorites` | 즐겨찾기 미보유 옷 조회 |
| POST | `/api/users/{userId}/wishlist-clothes` | 미보유 옷 저장 (신규 CLOTHES 생성) |
| POST | `/api/users/{userId}/wishlist-clothes/{clothesId}` | 기존 `EXTERNAL_SHOPPING` CLOTHES를 위시리스트에 연결 (추천 상품 저장) |
| PATCH | `/api/v1/clothes/{clothesId}/convert-to-owned` | 미보유 옷을 보유 옷으로 전환 (**공식 경로**) |
| PATCH | `/api/clothes/{clothesId}/convert-to-owned` | 위와 동일 (legacy 호환. 신규 FE는 `/api/v1` 사용) |

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
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/save` | 구매내역 기반 옷 저장 (`itemIndex` 선택, 생략 시 0) |
| POST | `/api/v1/users/{userId}/clothes/purchase-captures/{captureId}/items/{itemIndex}/skip` | 구매내역 캡처 상품 건너뛰기 |

단일 상품 draft/analyze 응답은 `name`, `category`, `itemType`, `season`, `gender` 등 flat 필드와 `items[0]` 모두에 분류 값을 포함합니다. 복수 상품 시 flat 분류 필드는 `null`이며 `items[]`(`itemIndex`, `season`, `gender`, `imageUrl`, `status`), `pendingItemCount`, `captureCompleted`를 사용합니다. `season`은 옷 등록 시 확정되는 공통 옷 정보이고, `gender`는 사용자에게 노출하지 않는 내부 code입니다.

#### 구매내역 저장 요청 (`PurchaseCaptureSaveRequest`)

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `name`, `brandName`, `productCode`, `category`, `itemType`, `gender`, `primaryColor`, `styles`, `externalSource`, `size`, `favorite`, `isVerified` | Y | 옷 공통·옷장 정보 |
| `secondaryColors`, `season` | N | 보조 색상, 옷 계절 code. `season` 생략 시 `ALL_SEASON` |
| `itemIndex` | N | 생략 시 0. 복수 상품일 때 저장 대상 인덱스 |
| `imageUrl` | N | 상품별 이미지 URL. 생략 시 draft `items[].imageUrl` 또는 캡처 `previewUrl`로 fallback |

#### 구매내역 상품 상태 (`PurchaseCaptureItemStatus`)

`PENDING`, `SAVED`, `SKIPPED`

### 외부 상품

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/naver/search` | 네이버쇼핑 상품 검색 |
| POST | `/api/v1/external/clothes/naver` | 네이버쇼핑 상품을 공통 옷 정보로 저장 |

### 추천

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/similar-products` | 유사 상품 추천 조회 |
| GET | `/api/v1/users/{userId}/clothes/{clothesId}/recommendations` | 옷장 기반 어울리는 옷 추천 조회 (`limitPerCategory` query, 아래 [옷장 기반 어울리는 옷 추천](#옷장-기반-어울리는-옷-추천-get-recommendations) 참고) |
| POST | `/api/v1/users/{userId}/recommendations/feedback` | 추천 상품 피드백 제출 (저장/싫어요/추천 제외) |
| GET | `/api/v1/recommendations/{wardrobeId}?currentTemp={temp}` | 취향 기반 상품 추천 |
| GET | `/api/v1/ootd/{wardrobeId}?currentTemp={temp}` | 내 옷장 기반 OOTD 추천 |
| GET | `/api/v1/users/{userId}/recommendations/ai-md/personas` | 사용자 성별에 맞는 AI MD 목록 조회 |
| GET | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/products` | 선택한 AI MD 기준 외부 상품 추천 |
| POST | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits` | 선택한 AI MD 기준 코디 후보 추천 |
| POST | `/api/v1/users/{userId}/recommendations/ai-md/{mdId}/outfits/save` | 선택한 AI MD 코디 후보 저장 |

#### 유사 상품 추천 (`GET .../similar-products`)

JWT 사용자와 path의 `userId`가 일치해야 합니다. `clothesId`는 해당 사용자의 활성 옷장 항목이어야 하며, `WARDROBE_CLOTHES.ownership_status`가 `OWNED` 또는 `WISHLIST`인 옷을 모두 기준 옷으로 사용할 수 있습니다. 응답의 `products`는 네이버쇼핑 후보와 `EXTERNAL_SHOPPING` 공용 `CLOTHES` 후보를 함께 정리한 유사상품 목록이며 최대 50개입니다. 내부 공용 후보는 기준 사용자의 성별과 `UNISEX` 상품만 포함하되, 사용자 성별이 `OTHER`이거나 없으면 내부 후보 성별을 제한하지 않습니다. 이 단계에서는 추천 상품을 저장하지 않습니다.

`products[]`는 기존 `NaverShoppingProduct` 형태를 유지하지만, 내부 DB 후보를 구분하기 위해 `clothesId`와 `candidateSource`를 함께 반환합니다. `candidateSource=NAVER`인 후보는 `clothesId=null`일 수 있고 DB 태그가 없어 `primaryColor`/`primaryStyle`은 `null`입니다. `candidateSource=INTERNAL`인 후보는 피드백·위시리스트 연결에 사용할 수 있는 `clothesId`를 포함하며, DB 태그가 있으면 `primaryColor`/`primaryStyle`에 대표 색상·대표 스타일 코드가 내려옵니다. 내부 후보는 가격 정보가 없어 `lowestPrice`/`highestPrice`가 `null`일 수 있고, 구매 링크가 없는 데이터는 `link=""`로 내려올 수 있으므로 FE는 가격·구매 버튼을 nullable 기준으로 렌더링해야 합니다.

#### 옷장 기반 어울리는 옷 추천 (`GET .../recommendations`)

옷장에 등록한 보유 옷 1벌을 기준으로 **같은 카테고리를 제외한 `EXTERNAL_SHOPPING` 공용 DB 후보**를 점수화해 카테고리별로 반환합니다. `PHOTO`·`PURCHASE_HISTORY` 등 다른 사용자 개인 등록 마스터는 후보에 포함하지 않습니다. 옷장에 이미 등록된 `clothesId`(본인 보유)는 후보에서 제외됩니다.

| Query | 필수 | 설명 |
| --- | --- | --- |
| `limitPerCategory` | N | 카테고리당 최대 추천 수. 기본 `5`, 허용 범위 `1`~`50` |

JWT 사용자와 path의 `userId`가 일치해야 합니다. `clothesId`는 해당 사용자의 보유 옷(`OWNED`)이어야 합니다.

**응답 (`ClothesRecommendationResponse`)**

| 필드 | 설명 |
| --- | --- |
| `anchor` | 기준 옷 요약 (`clothesId`, `name`, `imageUrl`, `userImageUrl`, `category`, `itemType`, `primaryColor`, `primaryColorDisplay`) |
| `recommendations` | 카테고리 code → 추천 목록. key 예: `TOP`, `BOTTOM`, `OUTER`, `SHOES` (기준 옷과 **동일 카테고리 key는 포함되지 않음**) |

**추천 항목 (`RecommendedItem`)**

| 필드 | 설명 |
| --- | --- |
| `clothesId` | 외부 `CLOTHES` ID |
| `wardrobeClothesId` | 옷장 연결 ID. 외부 후보는 보통 `null` |
| `name`, `imageUrl`, `userImageUrl` | 상품명·이미지 |
| `brandName` | 브랜드명 (`UNKNOWN` 가능) |
| `category`, `itemType` | 대·소분류 code |
| `primaryColor`, `primaryColorDisplay`, `secondaryColors` | 색상 |
| `styleCodes` | 스타일 code 배열 |
| `season` | `CLOTHES.season` code. 옷 등록 시 확정하며 `WARDROBE_CLOTHES`에는 저장하지 않음 |
| `gender` | 옷 대상 성별 code (`MALE`, `FEMALE`, `UNISEX`). 사용자 화면 표시 대상 아님 |
| `compatibilityScore` | 어울림 점수 (0~100, 내림차순 정렬) |

```json
{
  "success": true,
  "data": {
    "anchor": {
      "clothesId": 101,
      "name": "화이트 반팔 티셔츠",
      "imageUrl": "https://...",
      "userImageUrl": "https://...",
      "category": "TOP",
      "itemType": "SHORT_SLEEVE",
      "primaryColor": "WHITE",
      "primaryColorDisplay": {
        "code": "WHITE",
        "label": "화이트",
        "hex": "#FFFFFF"
      }
    },
    "recommendations": {
      "BOTTOM": [
        {
          "clothesId": 502,
          "wardrobeClothesId": null,
          "name": "와이드 슬랙스",
          "imageUrl": "https://...",
          "userImageUrl": null,
          "category": "BOTTOM",
          "itemType": "SLACKS",
          "primaryColor": "BLACK",
          "primaryColorDisplay": {
            "code": "BLACK",
            "label": "블랙",
            "hex": "#000000"
          },
          "secondaryColors": [],
          "styleCodes": ["MINIMAL"],
          "season": "ALL_SEASON",
          "compatibilityScore": 87,
          "gender": "UNISEX"
        }
      ],
      "OUTER": [],
      "SHOES": []
    }
  },
  "message": null
}
```

> **Note**: 점수는 색상(35%)·스타일(30%)·itemType(20%)·시즌(15%) 가중 합산입니다. 동점(`compatibilityScore` 동일) 후보는 `brandName`이 `UNKNOWN`이 아닌 상품을 먼저 노출합니다. FE는 사용자 프로필 성별에 맞지 않는 `gender` 후보를 내부적으로 제외할 수 있지만, 해당 값을 사용자 화면에 표시하지 않습니다.

#### 취향 기반 상품 추천 응답 (RecommendResponse)

```json
{
  "success": true,
  "data": [
    {
      "title": "상품명",
      "link": "https://...",
      "imageUrl": "https://...",
      "price": "0",
      "score": "0.95",
      "reason": "Style Match: 0.9, Weather Match: 1.0",
      "brandName": "브랜드명",
      "category": "카테고리",
      "primaryColor": "GRAY",
      "primaryColorDisplay": {
        "code": "GRAY",
        "name": "그레이",
        "hex": "#9E9E9E"
      },
      "primaryStyle": "스타일",
      "clothesId": 123
    }
  ],
  "message": null
}
```

> **Note**: 현재 `price`는 placeholder("0")이며, `score`는 0.0~1.0 사이의 문자열, `reason`은 기술적 매칭 결과입니다. 상세 내용은 [implementation-gaps.md](../backend/implementation-gaps.md)를 참고하세요.

#### OOTD 추천 응답 (OotdResponse)

```json
{
  "success": true,
  "data": {
    "combinations": [
      {
        "top": {
          "clothesId": 101,
          "wardrobeClothesId": 1,
          "name": "상의 이름",
          "brand": "브랜드",
          "color": "BLACK",
          "imageUrl": "https://...",
          "externalProductUrl": "https://...",
          "category": "TOP",
          "itemType": "SHORT_SLEEVE",
          "favorite": true
        },
        "bottom": {
          "clothesId": 102,
          "wardrobeClothesId": 2,
          "name": "하의 이름",
          "brand": "브랜드",
          "color": "BLUE",
          "imageUrl": "https://...",
          "externalProductUrl": "https://...",
          "category": "BOTTOM",
          "itemType": "DENIM",
          "favorite": false
        },
        "outer": null,
        "shoes": {
          "clothesId": 103,
          "wardrobeClothesId": 3,
          "name": "신발 이름",
          "brand": "브랜드",
          "color": "WHITE",
          "imageUrl": "https://...",
          "externalProductUrl": "https://...",
          "category": "SHOES",
          "itemType": "SNEAKERS",
          "favorite": false
        },
        "totalScore": 2.5
      }
    ],
    "weatherLabel": "오늘 20°C — 얇은 셔츠·면바지 추천",
    "currentTemp": 20.0
  },
  "message": null
}
```

> **Note**: `outer` 필드는 기온에 따라 외투가 필요 없는 경우(HOT, WARM) `null`로 반환됩니다. `shoes` 필드는 외부 쇼핑몰 후보를 포함해 신발 후보가 전혀 없는 경우 `null`로 반환될 수 있으므로, FE는 `outer` 및 `shoes` 필드 모두 `null` 가능성을 고려해 렌더링을 분기해야 합니다. `wardrobeClothesId`는 보유 옷이 부족해 외부 쇼핑몰 상품으로 보충된 경우 `null`로 반환될 수 있습니다.

#### 추천 피드백 제출

**POST** `/api/v1/users/{userId}/recommendations/feedback`

- **요청 Body**
```json
{
  "clothesId": 123,
  "feedbackType": "SAVED"
}
```

- **피드백 타입 (`feedbackType`)**
    - `SAVED`: 저장하기 (긍정 - 가중치 미반영)
    - `DISLIKE`: 싫어요 (부정 - 가중치 마이너스 반영)
    - `EXCLUDE`: 추천 제외 (부정 + 후보 제외 - 가중치 마이너스 반영)

- **응답 (성공)**
```json
{
  "success": true,
  "data": null,
  "message": null
}
```
#### AI MD 목록 응답 (AiMdPersonaResponse)

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
    }
  ],
  "message": null
}
```

> **Note**: AI MD 목록은 JWT 사용자와 path의 `userId`가 일치해야 조회할 수 있으며, 사용자 성별에 맞는 MD만 반환합니다. 남성 사용자는 `taesik`, `junsik`, 여성 사용자는 `sesoon`, `gahyun`, `seongmi`를 선택할 수 있습니다.

#### AI MD 상품 추천 응답 (AiMdProductRecommendationResponse)

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
    "query": "남성 블랙 스트릿 티셔츠 | 남성 미니멀 팬츠 | 남성 캐주얼 스니커즈",
    "products": [
      {
        "product": {
          "title": "상품명",
          "link": "https://...",
          "image": "https://...",
          "lowestPrice": 59000,
          "highestPrice": null,
          "mallName": "쇼핑몰명",
          "productId": "123",
          "productType": "1",
          "brand": "브랜드",
          "maker": "제조사",
          "category1": "패션의류",
          "category2": "남성의류",
          "category3": "티셔츠",
          "category4": "",
          "clothesId": null,
          "candidateSource": "NAVER"
        },
        "reason": "MD 말투가 반영된 추천 이유"
      }
    ]
  },
  "message": null
}
```

> **Note**: 상품 추천은 `USER_STYLES.combined_weight`가 높은 스타일을 더 자주, 낮은 양수 스타일을 더 낮은 빈도로 반영합니다. 스타일·카테고리·색상·검색 페이지를 달리한 네이버쇼핑 검색과 내부 `EXTERNAL_SHOPPING` 공용 후보를 함께 사용하고, 내부 후보는 선택한 MD 성별과 `UNISEX` 상품만 포함합니다. 내부 후보는 DB 태그가 있으면 `product.primaryColor`/`product.primaryStyle`에 대표 색상·대표 스타일 코드를 포함하고, 네이버 후보는 해당 값이 `null`입니다. 동일 상품을 제거한 후보 중 Gemini가 브랜드와 카테고리가 한쪽에 치우치지 않도록 최대 40개 상품과 추천 이유를 선별합니다. 서버의 1차 선별에서도 같은 브랜드는 최대 2개, 같은 카테고리는 최대 4개로 제한합니다. 검색 후보가 치우쳐 40개를 채울 수 없을 때만 중복 상품 제외 조건을 유지한 채 이 제한을 완화합니다. 재추천 시 검색 조합과 후보 순서는 달라질 수 있습니다. `query`는 실제로 사용한 여러 검색어를 ` | `로 연결한 디버깅 값입니다. 이 단계에서는 저장하지 않습니다. 상품 카드 액션은 `candidateSource` 기준으로 분기합니다. `candidateSource=INTERNAL`이고 `clothesId`가 있으면 `POST /api/users/{userId}/wishlist-clothes/{clothesId}`로 기존 공용 옷을 위시리스트에 연결하고, 같은 `clothesId`로 `POST /api/v1/users/{userId}/recommendations/feedback`에 저장/싫어요/추천 제외 피드백을 제출할 수 있습니다. `candidateSource=NAVER`이고 `clothesId=null`인 후보만 `POST /api/users/{userId}/wishlist-clothes` 신규 생성 플로우를 사용합니다. `link=""`이면 구매 버튼을 숨기거나 비활성화합니다. 유사 상품 추천 결과도 동일한 `candidateSource` 분기 기준을 사용합니다. 보유 옷이 없으면 `409` 응답과 함께 등록 안내 메시지를 반환합니다.

#### AI MD 코디 추천 응답 (AiMdOutfitRecommendationResponse)

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
        "title": "코디 제목",
        "description": "코디 설명",
        "situation": "DAILY",
        "season": "ALL_SEASON",
        "reason": "MD 말투가 반영된 코디 추천 이유",
        "stylingTip": "스타일링 팁",
        "ownedItems": [
          {
            "wardrobeClothesId": 1,
            "name": "보유 상의",
            "category": "TOP"
          },
          {
            "wardrobeClothesId": 2,
            "name": "보유 하의",
            "category": "BOTTOM"
          },
          {
            "wardrobeClothesId": 3,
            "name": "보유 신발",
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

> **Note**: 코디 추천은 Gemini가 4개 코디 후보를 구성하지만 이 단계에서는 `OUTFITS`, `OUTFIT_ITEMS`, 외부 `Clothes`를 저장하지 않습니다. 각 후보는 사용자 옷장 등록 옷을 최소 1개 포함해야 하며, `OWNED`와 `WISHLIST` 옷장 항목을 모두 코디 구성에 사용할 수 있습니다. 옷장 등록 옷과 외부 상품을 합친 전체 구성에 `TOP`, `BOTTOM`, `SHOES`가 각각 최소 1개 있어야 합니다. `OUTER`는 선택 사항입니다. 외부 상품은 필수가 아니므로 옷장 등록 옷만으로 필수 세 카테고리가 완성된 후보도 유효합니다. 프론트는 사용자가 선택한 후보만 저장 API로 전달합니다.

#### AI MD 추천 코디 저장 요청/응답

```json
{
  "title": "코디 제목",
  "description": "코디 설명",
  "situation": "DAILY",
  "season": "ALL_SEASON",
  "reason": "MD 말투가 반영된 코디 추천 이유",
  "stylingTip": "스타일링 팁",
  "wardrobeClothesIds": [1, 2, 3],
  "externalProducts": []
}
```

위 예시의 `wardrobeClothesIds`는 각각 `TOP`, `BOTTOM`, `SHOES`인 사용자 옷장 등록 옷을 의미합니다. 저장 요청도 추천 후보와 동일하게 사용자 옷장 등록 옷을 최소 1개 포함하고, `OWNED`와 `WISHLIST` 옷장 항목을 모두 사용할 수 있습니다. `wardrobeClothesIds`와 `externalProducts`를 합쳐 `TOP`, `BOTTOM`, `SHOES`가 모두 구성되어야 합니다. 외부 상품 없이 옷장 등록 옷만으로 완성할 수 있으며, 필수 카테고리가 누락되면 `400 Bad Request`를 반환합니다.

저장 성공 시에는 선택된 코디 1개가 `OUTFITS`, `OUTFIT_ITEMS`에 저장되고, 응답은 저장된 `outfit`과 구성 옷 목록을 포함합니다. 저장된 구성 옷은 코디북 조회 응답의 `outfits[].items`에서도 다시 조회할 수 있습니다.

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
| GET | `/api/v1/outfit-books/{bookId}/outfits/{outfitId}` | 코디 상세 조회 |
| PATCH | `/api/v1/outfit-books/{bookId}/outfits/{outfitId}` | 코디 수정 |
| DELETE | `/api/v1/outfit-books/{bookId}/outfits/{outfitId}` | 코디 삭제 |

#### 코디 저장/수정 요청 (`OutfitCreateRequest`, `OutfitUpdateRequest`)

```json
{
  "title": "코디 제목",
  "description": "코디 설명",
  "thumbnailUrl": "https://...",
  "situation": "DAILY",
  "season": "ALL_SEASON",
  "favorite": false,
  "items": [
    {
      "clothesId": 1,
      "itemRole": "TOP",
      "layerOrder": 0
    }
  ]
}
```

> **Note**: 수정(`PATCH`) 요청에서 `items`를 생략하면 기존 구성 아이템이 유지됩니다.
> - `items` 생략(null): 기존 구성 아이템 유지, 메타데이터만 수정
> - `items: []` (빈 배열): 기존 구성 아이템 전체 삭제
> - `items: [...]` (목록): 기존 구성 전체 교체
#### GET /api/v1/outfit-books/{bookId}/outfits/{outfitId} — 코디 조회

```json
{
  "success": true,
  "data": {
    "outfitId": 1,
    "outfitBookId": 1,
    "title": "봄 데일리 코디",
    "description": "가볍게 입기 좋은 봄 코디",
    "thumbnailUrl": "",
    "situation": "일상",
    "season": "SPRING",
    "favorite": false,
    "items": [
      {
        "outfitItemId": 1,
        "itemRole": "TOP",
        "layerOrder": 1,
        "clothes": { ... }
      }
    ],
    "createdAt": "2026-06-11T11:00:00",
    "updatedAt": "2026-06-11T11:00:00"
  }
}
```
#### 코디북 조회 응답 (OutfitBookResponse)

```json
{
  "success": true,
  "data": {
    "outfitBookId": 1,
    "userId": 1,
    "outfitCount": 1,
    "outfits": [
      {
        "outfitId": 1,
        "outfitBookId": 1,
        "title": "코디 제목",
        "description": "코디 설명",
        "thumbnailUrl": "https://...",
        "situation": "DAILY",
        "season": "ALL_SEASON",
        "favorite": false,
        "items": [
          {
            "outfitItemId": 1,
            "itemRole": "TOP",
            "layerOrder": 0,
            "clothes": {
              "clothesId": 1,
              "wardrobeClothesId": 1,
              "name": "보유 옷 또는 외부 상품명",
              "brandName": "브랜드명",
              "clothesInfoSource": "PHOTO"
            }
          }
        ],
        "createdAt": "2026-06-08T12:00:00",
        "updatedAt": "2026-06-08T12:00:00"
      }
    ],
    "createdAt": null,
    "updatedAt": null
  },
  "message": null
}
```

> **Note**: `items[].clothes`가 사용자 옷장에 연결된 보유 옷이면 `wardrobeClothesId`, `wardrobeId`, `userId`, `size` 등 사용자 옷장 연결 정보가 함께 채워집니다. `season`은 `CLOTHES.season` 기준의 공통 옷 정보입니다. AI MD가 섞은 외부 상품처럼 옷장 연결이 없는 옷은 옷장 연결 필드가 `null`입니다.

### 이미지

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/v1/images/clothes/{userId}/{filename}` | 옷 이미지 조회 |
| GET | `/api/v1/images/purchase-captures/{userId}/{filename}` | 구매내역 캡처 이미지 조회 |
| GET | `/api/v1/images/feed/{userId}/{filename}` | 피드 이미지 조회 |
| GET | `/api/v1/images/profile/{userId}/{filename}` | 프로필 이미지 조회 |

### 룩피드 (FEED-001~008)

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/feed/posts` | FEED-001 피드 업로드 |
| GET | `/api/v1/feed/posts` | FEED-002 공개 피드 목록 |
| GET | `/api/v1/feed/posts/{postId}` | FEED-003 피드 상세 |
| PUT | `/api/v1/feed/posts/{postId}` | 피드 수정 |
| DELETE | `/api/v1/feed/posts/{postId}` | 피드 삭제 |
| GET | `/api/v1/feed/users/{userId}/posts` | 사용자 공유 피드 목록 |
| POST | `/api/v1/feed/images` | 피드 이미지 업로드 (`multipart/form-data`, field: `file`) |
| POST | `/api/v1/feed/posts/{postId}/likes` | FEED-004 좋아요 토글 |
| POST | `/api/v1/feed/posts/{postId}/saves` | FEED-005 저장 토글 |
| GET | `/api/v1/feed/posts/{postId}/comments` | FEED-006/007 댓글·대댓글 목록 |
| POST | `/api/v1/feed/posts/{postId}/comments` | FEED-006/007 댓글·대댓글 작성 |
| PUT | `/api/v1/feed/posts/{postId}/comments/{commentId}` | 댓글 수정 (작성자 본인만) |
| DELETE | `/api/v1/feed/posts/{postId}/comments/{commentId}` | 댓글 삭제 |
| POST | `/api/v1/feed/users/{followeeId}/follows` | FEED-008 팔로우 토글 |

#### POST /api/v1/feed/posts — 피드 업로드

```json
{
  "outfitId": 1,
  "caption": "오늘의 데일리룩",
  "imageUrls": [
    "http://localhost:8080/api/v1/images/feed/1/sample.jpg"
  ]
}
```

- `outfitId`는 선택. 본인 코디북의 활성 코디만 연결 가능
- `imageUrls`는 최소 1장, 최대 10장

#### GET /api/v1/feed/posts — 피드 목록

Query: `page`(default 0), `size`(default 20, max 50)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "feedPostId": 1,
        "author": {
          "userId": 1,
          "nickname": "closet",
          "profileImageUrl": "https://..."
        },
        "outfit": { "...": "OutfitResponse 또는 null" },
        "caption": "오늘의 데일리룩",
        "images": [
          {
            "feedPostImageId": 1,
            "imageUrl": "http://localhost:8080/api/v1/images/feed/1/sample.jpg",
            "sortOrder": 0
          }
        ],
        "likeCount": 3,
        "commentCount": 1,
        "likedByMe": false,
        "savedByMe": false,
        "hidden": false,
        "mine": false,
        "createdAt": "2026-06-09T12:00:00",
        "updatedAt": "2026-06-09T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

> **Note**: FEED-009 빈 상태는 BE가 빈 `content` 배열을 반환하면 FE에서 안내 UI를 표시합니다.

#### PUT /api/v1/feed/posts/{postId}/comments/{commentId} — 댓글 수정

작성자 본인만 수정 가능. `parentCommentId`는 수정 시 무시됩니다.

요청:

```json
{
  "content": "수정된 댓글 내용 (최대 1000자)"
}
```

응답:

```json
{
  "success": true,
  "data": {
    "feedCommentId": 1,
    "feedPostId": 10,
    "author": {
      "userId": 1,
      "nickname": "closet",
      "profileImageUrl": "https://..."
    },
    "parentCommentId": null,
    "content": "수정된 댓글 내용",
    "replies": [],
    "createdAt": "2026-06-09T12:00:00",
    "isOwner": true
  }
}
```

- 본인이 아니면 403 반환
- 존재하지 않는 댓글이면 404 반환

## API 변경 규칙

- API 경로, 요청 필드, 응답 필드가 바뀌면 이 문서를 같은 PR에서 수정합니다.
- FE가 사용하는 API 변경은 FE 담당자와 동기화 이슈 또는 PR에서 확인합니다.
- 공통 응답 형식에 필드가 추가되면 성공/실패 응답 예시를 함께 수정합니다.
