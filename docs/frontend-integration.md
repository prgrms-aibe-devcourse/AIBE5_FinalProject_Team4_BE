# 프론트엔드 연동 가이드 (closetnangam-BE)

백엔드 API와 로컬 연동 시 참고 문서입니다.  
상세 스펙은 Swagger UI(`http://localhost:8080/swagger-ui.html`)와 `CategoryReadme.md`를 함께 보세요.

---

## 1. 로컬 환경

```bash
docker-compose up -d          # MySQL :3307, Redis :6379
cp .env.example .env          # JWT_SECRET 등 채우기
./gradlew bootRun             # http://localhost:8080
```

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| 카테고리 code 규칙 | `CategoryReadme.md` |

---

## 2. 인증

### 2.1 JWT (대부분의 `/api/**`)

```http
Authorization: Bearer {accessToken}
```

- 토큰의 `userId`와 path의 `{userId}`가 다르면 **403** + `ApiResponse.fail`
- 토큰 없음/만료 → **401** (바디가 없을 수 있음, `ApiResponse` 아님)

### 2.2 OAuth2 로그인

성공 시 프론트로 리다이렉트:

```
http://localhost:3000?token={jwt}
```

(`application.yml` → `app.oauth2.redirect-uri`)

### 2.3 개발용 mock 토큰 (`local` 프로필만)

```http
GET /api/v1/auth/mock-token?userId=1
```

응답 예:

```json
{
  "success": true,
  "message": "임시 토큰이 정상 발급되었습니다...",
  "data": {
    "userId": 1,
    "accessToken": "...",
    "headerValue": "Bearer ..."
  }
}
```

---

## 3. 공통 응답 형식

```json
{
  "success": true,
  "data": { },
  "message": null
}
```

| HTTP | 의미 | body |
|------|------|------|
| 200/201 | 성공 | `success: true`, `data` 있음 |
| 400 | 검증/잘못된 요청 | `success: false`, `message` |
| 403 | userId 불일치 등 | `success: false`, `message` |
| 404 | 리소스 없음 | `success: false`, `message` |
| 409 | 중복·상태 충돌 | `success: false`, `message` |
| 401 | 미인증 | 바디 없을 수 있음 |
| 204 | 삭제 성공 | **바디 없음** (`DELETE` 옷 삭제) |

---

## 4. URL prefix (중요)

| 영역 | Prefix | 인증 |
|------|--------|------|
| 보유 옷, 옷장, 사진/구매 등록, 코디북, 유사상품 | `/api/v1/...` | 필요 |
| **위시리스트** | `/api/users/...` (**v1 없음**) | 필요 |
| 카테고리 | `/api/v1/categories` 또는 `/api/categories` | 불필요 |
| 네이버 검색 | `/api/naver/**` | 불필요 |
| 날씨 | `/api/weather/**` | 불필요 |
| 등록 방식 목록 | `/api/v1/clothes/registration-methods` | 불필요 |

---

## 5. 옷장 (Wardrobe)

### 5.1 데이터 모델

```
User (1) ── (1) Wardrobe ── (N) WardrobeClothes ── (1) Clothes
```

- **Wardrobe**: 회원당 1개 (`wardrobeId`)
- **WardrobeClothes**: 옷장에 담긴 연결(사이즈, 시즌, 즐겨찾기, `ownershipStatus`)
- **Clothes**: 옷 마스터(이름, 브랜드, 카테고리, 색/스타일 태그)

옷 등록 API는 내부에서 `getOrCreateWardrobe`로 옷장을 **자동 생성**합니다.  
FE에서 반드시 `POST /wardrobes`를 먼저 호출할 필요는 없습니다.

### 5.2 옷장 API

Base: `/api/v1/wardrobes`

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/users/{userId}` | 옷장 조회 → `wardrobeId`, `userId` |
| POST | `/users/{userId}` | 옷장 1회 생성 (중복 시 409) |
| GET | `/users/{userId}/statistics` | 보유 옷 통계 |

**WardrobeResponse**

```json
{
  "wardrobeId": 1,
  "userId": 1
}
```

**WardrobeStatisticsResponse** (보유 OWNED만, soft delete 제외)

```json
{
  "userId": 1,
  "wardrobeId": 1,
  "totalOwnedCount": 12,
  "itemTypes": [
    {
      "itemType": "SHORT_SLEEVE",
      "itemTypeLabel": "반팔",
      "category": "TOP",
      "count": 3
    }
  ],
  "userStylePayloads": [
    {
      "styleId": 1,
      "styleCode": "CASUAL",
      "styleName": "캐주얼",
      "weightedScore": 4.2,
      "wardrobeWeight": 42
    }
  ]
}
```

`userStylePayloads`는 UI/향후 DB용 계산값이며, 아직 서버에 저장하지 않습니다.

### 5.3 옷장 화면에서 쓰는 옷 목록 API

| 화면 | API |
|------|-----|
| 보유 옷 전체 | `GET /api/v1/users/{userId}/clothes` |
| 보유 즐겨찾기 | `GET /api/v1/users/{userId}/clothes/favorites` |
| 위시리스트 | `GET /api/users/{userId}/wishlist-clothes` |
| 상세 | `GET /api/v1/clothes/{clothesId}` (토큰 userId 사용) |
| 등록 | `POST /api/v1/users/{userId}/clothes` |
| 수정 | `PATCH /api/v1/clothes/{clothesId}` |
| 즐겨찾기 | `PATCH /api/v1/clothes/{clothesId}/favorite` |
| 삭제 | `DELETE /api/v1/clothes/{clothesId}` → **204** |
| 코디 추천 | `GET /api/v1/users/{userId}/clothes/{clothesId}/recommendations?limitPerCategory=5` |

### 5.4 FE 옷장 화면 흐름 예시

1. 로그인 → `userId` + JWT 저장  
2. (선택) `GET /api/v1/wardrobes/users/{userId}`  
3. 목록: `GET /api/v1/users/{userId}/clothes`  
4. 통계 탭: `GET /api/v1/wardrobes/users/{userId}/statistics`  

---

## 6. ClothesResponse 필드

최근 스키마 기준 (**`sourceType` → `ownershipStatus`**):

| 필드 | 설명 |
|------|------|
| `clothesId` | 옷 마스터 ID |
| `wardrobeClothesId` | 옷장 연결 ID |
| `wardrobeId`, `userId` | 옷장/회원 |
| `ownershipStatus` | `OWNED` \| `WISHLIST` |
| `infoSource` | `PHOTO`, `PURCHASE_HISTORY`, `EXTERNAL_SHOPPING` |
| `primaryColor` | code 문자열 |
| `primaryColorDisplay` | `{ code, name, hex }` |
| `secondaryColors[]` | `{ code, colorDisplay, sortOrder }` |
| `styles[]` | `{ styleId, code, name, styleRole, sortOrder }` |
| `size`, `season`, `userImageUrl`, `isFavorite` | 옷장 연결 정보 |

---

## 7. 옷 등록 요청 예시

`GET /api/v1/categories`에서 **code**만 사용해 등록합니다.

```json
{
  "name": "셔츠",
  "brandName": "브랜드",
  "productCode": "ABC123",
  "imageUrl": "http://localhost:8080/api/v1/images/clothes/1/xxx.jpg",
  "category": "TOP",
  "itemType": "SHORT_SLEEVE",
  "primaryColor": "WHITE",
  "secondaryColors": [],
  "styles": ["CASUAL"],
  "size": "M",
  "season": "SPRING",
  "isVerified": false
}
```

- `imageUrl`: `http`/`https` 허용 (로컬 업로드 URL OK)
- 위시리스트 `externalProductUrl`: **https만**, localhost 등 내부 IP 차단

---

## 8. 사진 / 구매 캡처 등록 (multipart)

| 플로우 | Base path |
|--------|-----------|
| 사진 등록 | `/api/v1/users/{userId}/clothes/photos` |
| 구매 캡처 | `/api/v1/users/{userId}/clothes/purchase-captures` |

공통: `POST` 업로드 → `POST /{id}/analyze` → `GET /{id}/draft` → `POST /{id}/save`

---

## 9. 이미지 URL

- 업로드 후 저장 URL 예: `http://localhost:8080/api/v1/images/clothes/{userId}/{filename}`
- 이미지 **조회**도 JWT 필요 (`<img src>`만으로는 401)
- 프록시 또는 blob + Authorization 헤더 처리 필요

---

## 10. 삭제 (soft delete)

- `DELETE /api/v1/clothes/{clothesId}` → **204**, 바디 없음
- 사용자 옷장 목록에서는 사라짐 (`wardrobe_clothes.deleted_at` 설정)
- `clothes` 마스터 행은 유지 (피드·타 사용자 `clothes_id` 참조용)

---

## 11. CORS

백엔드에 **CORS 설정이 없습니다**.  
브라우저에서 `localhost:3000` → `localhost:8080` 직접 호출 시 preflight 오류가 날 수 있습니다.

- Vite/Next **dev proxy** 사용, 또는
- BE에 CORS 설정 추가 협의

---

## 12. axios 예시

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 성공: response.data.success === true → response.data.data
// 실패: response.data.message
// 401: 로그인 / mock-token 재발급
```

---

## 13. 자주 나는 오류

| 증상 | 확인 |
|------|------|
| 403 | URL `userId` ≠ JWT `userId` |
| 401 | Bearer 헤더 누락 |
| CORS 에러 | proxy 또는 CORS 설정 |
| 위시리스트 404 | `/api/v1/...` 대신 `/api/users/...` 사용 |
| 삭제 후 목록에 안 보임 | 정상 (soft delete) |
| `sourceType` undefined | 필드명 `ownershipStatus`로 변경됨 |

---

## 14. 변경 이력 (FE Breaking)

| 항목 | 이전 | 현재 |
|------|------|------|
| 보유/위시 구분 필드 | `sourceType` | `ownershipStatus` |
| 옷 삭제 | hard delete | soft delete (204) |

---

*문서 생성: BE ↔ FE 연동 정리. 질문은 백엔드 팀 또는 Swagger 기준으로 맞춰 주세요.*
