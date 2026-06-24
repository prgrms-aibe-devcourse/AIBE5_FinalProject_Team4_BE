---
doc_type: be_sequence_diagrams
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-24
---

# 시퀀스 다이어그램

이 문서는 옷장난감의 주요 기능 흐름을 시퀀스 다이어그램으로 정리합니다. API 경로는 개발 중 변경될 수 있으므로, 이 문서에서는 경로보다 기능명과 행위명 중심으로 흐름을 설명합니다.

## 문서 기준

- 상세 API 경로와 요청/응답 필드는 [API 계약](../api/api-contract.md)을 따릅니다.
- 기능 요구와 세부기능 ID는 [요구사항 정의서](../requirements/requirements-definition.md)와 [기능 인덱스](../requirements/feature-index.md)를 따릅니다.
- 데이터 저장 책임은 [ERD](../database/erd.md)와 [데이터 생명주기](../database/data-lifecycle.md)를 따릅니다.
- 시퀀스 다이어그램은 구현 코드를 그대로 나열하는 문서가 아니라, 기능이 어떤 순서로 동작해야 하는지 확인하기 위한 문서입니다.

## 1. 로그인 및 온보딩

```mermaid
sequenceDiagram
  title 로그인 및 온보딩
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant OAuth as OAuth Provider
  participant Token as Token Store
  participant DB as Database

  User->>FE: 소셜 로그인 선택
  FE->>BE: OAuth 로그인 시작 요청
  BE->>OAuth: 사용자 인증 요청
  OAuth-->>BE: 인증 결과 전달
  BE->>DB: 사용자 및 소셜 계정 조회

  alt 신규 사용자
    BE->>DB: 사용자 기본 정보 생성
    BE->>Token: 인증 토큰 저장
    BE-->>FE: 로그인 결과와 온보딩 필요 상태 전달
    FE->>User: 온보딩 화면 표시
    User->>FE: 약관 동의, 프로필, 선호 스타일 입력
    FE->>BE: 온보딩 정보 저장 요청
    BE->>DB: 사용자 프로필과 초기 선호 스타일 저장
    BE-->>FE: 온보딩 완료 응답
    FE->>User: 메인 화면 표시
  else 기존 사용자
    BE->>Token: 인증 토큰 저장
    BE-->>FE: 로그인 결과 전달
    FE->>User: 메인 화면 표시
  else OAuth 실패
    BE-->>FE: FE redirect URI + error=oauth_failed
    FE->>User: 로그인 실패 안내 및 재시도 유도
  end
```

## 2. 옷 등록

```mermaid
sequenceDiagram
  title 옷 등록
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant Image as Image Storage
  participant AI as AI Service
  participant Shopping as Shopping API
  participant DB as Database

  User->>FE: 옷 등록 방식 선택

  alt 사진 기반 등록
    FE->>BE: 옷 사진 업로드
    BE->>Image: 이미지 저장
    BE->>DB: 사진 등록 세션 저장
    FE->>BE: 사진 분석 요청
    BE->>AI: 의류 분류 요청
    AI-->>BE: 의류 분석 결과 반환
  else 구매내역 기반 등록
    FE->>BE: 구매내역 캡처 업로드
    BE->>Image: 캡처 이미지 저장
    BE->>DB: 구매내역 등록 세션 저장
    FE->>BE: 구매내역 분석 요청
    BE->>AI: 구매 상품 추출 요청
    AI-->>BE: 구매 상품 후보 반환
  else 외부 상품 저장
    FE->>BE: 외부 상품 조회 요청
    BE->>Shopping: 상품 후보 검색
    Shopping-->>BE: 상품 후보 반환
    BE-->>FE: 외부 상품 목록 반환
    User->>FE: 저장할 상품 선택
    FE->>BE: 외부 상품 저장 요청
  end

  BE->>DB: 등록 초안 저장
  BE-->>FE: 등록 초안 반환
  User->>FE: 결과 확인 및 허용 필드 수정
  FE->>BE: 옷 등록 확정 요청
  BE->>DB: 공통 옷 정보와 사용자 옷장 연결 저장
  BE-->>FE: 옷 등록 완료

  Note over FE,BE: 계절은 옷 공통 정보이며 등록 시 1개 선택합니다. 생성된 옷의 계절은 수정 대상이 아닙니다.
```

## 3. 옷장 조회 및 관리

```mermaid
sequenceDiagram
  title 옷장 조회 및 관리
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant DB as Database
  participant Image as Image Storage

  User->>FE: 옷장 화면 진입
  FE->>BE: 사용자 옷장 목록 요청
  BE->>DB: 보유 옷과 미보유 옷 조회
  BE-->>FE: 옷장 목록과 요약 정보 반환
  FE->>User: 옷장 목록 표시

  User->>FE: 옷 상세 선택
  FE->>BE: 옷 상세 정보 요청
  BE->>DB: 공통 옷 정보와 사용자 옷장 연결 정보 조회
  BE-->>FE: 옷 상세 정보 반환
  FE->>User: 옷 상세 화면 표시

  alt 옷 정보 수정
    User->>FE: 수정 가능한 정보 변경
    FE->>BE: 옷 정보 수정 요청
    BE->>DB: 수정 가능한 옷 정보 갱신
    BE-->>FE: 수정 완료 응답
  else 즐겨찾기 변경
    User->>FE: 즐겨찾기 선택 또는 해제
    FE->>BE: 즐겨찾기 상태 변경 요청
    BE->>DB: 사용자 옷장 연결 정보 갱신
    BE-->>FE: 즐겨찾기 변경 완료
  else 옷장에서 삭제
    User->>FE: 옷장에서 삭제 선택
    FE->>BE: 사용자 옷장 연결 삭제 요청
    BE->>DB: 사용자 옷장 연결만 제거
    BE-->>FE: 삭제 완료 응답
  end

  Note over BE,DB: 옷장에서 삭제해도 공통 옷 정보와 이미지는 유지합니다.
```

## 4. 추천 및 피드백

```mermaid
sequenceDiagram
  title 추천 및 피드백
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant DB as Database
  participant Shopping as Shopping API
  participant Weather as Weather API
  participant AI as AI Service

  User->>FE: 추천 화면 진입
  FE->>BE: 추천 목록 요청
  BE->>DB: 사용자 취향, 옷장, 피드백 정보 조회

  opt 외부 상품 추천이 필요한 경우
    BE->>Shopping: 상품 후보 검색
    Shopping-->>BE: 상품 후보 반환
  end

  opt 날씨 보조 정보가 필요한 경우
    BE->>Weather: 날씨 정보 조회
    Weather-->>BE: 날씨 정보 반환
  end

  opt AI 설명 또는 코디 생성이 필요한 경우
    BE->>AI: 추천 설명 또는 코디 생성 요청
    AI-->>BE: AI 추천 결과 반환
  end

  BE->>BE: 추천 점수 계산 및 정렬
  BE-->>FE: 추천 결과 반환
  FE->>User: 추천 목록 표시

  User->>FE: 저장, 싫어요, 추천 제외 중 선택
  FE->>BE: 추천 피드백 저장 요청
  BE->>DB: 추천 피드백 저장
  BE->>DB: 사용자 선호 점수 반영 대상 기록
  BE-->>FE: 피드백 처리 완료
```

## 5. 미보유 옷 보유 전환

```mermaid
sequenceDiagram
  title 미보유 옷 보유 전환
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant DB as Database

  User->>FE: 미보유 옷에서 구매 완료 선택
  FE->>BE: 보유 옷 전환 요청
  BE->>DB: 사용자 옷장 연결 정보 조회

  alt 미보유 옷이 사용자 옷장에 존재함
    BE->>DB: 보유 상태로 변경
    BE-->>FE: 보유 전환 완료
    FE->>User: 보유 옷장 반영 결과 표시
  else 전환할 수 없는 상태
    BE-->>FE: 전환 실패 사유 반환
    FE->>User: 오류 또는 안내 메시지 표시
  end
```

## 6. 룩피드 상호작용

```mermaid
sequenceDiagram
  title 룩피드 상호작용
  autonumber

  actor User as 사용자
  participant FE as Frontend
  participant BE as Backend
  participant Image as Image Storage
  participant DB as Database

  User->>FE: 룩피드 화면 진입
  FE->>BE: 피드 목록 요청
  BE->>DB: 공개 피드 목록 조회
  BE-->>FE: 피드 목록 반환
  FE->>User: 피드 목록 표시

  alt 피드 작성
    User->>FE: 코디, 이미지, 게시글 내용 입력
    FE->>BE: 피드 작성 요청
    opt 피드 이미지가 있는 경우
      BE->>Image: 피드 이미지 저장
    end
    BE->>DB: 피드 게시글과 이미지 정보 저장
    BE-->>FE: 피드 작성 완료
    FE->>User: 작성된 피드 표시
  else 피드 상호작용
    User->>FE: 좋아요 또는 댓글 입력
    FE->>BE: 피드 상호작용 요청
    BE->>DB: 좋아요 또는 댓글 정보 반영
    BE-->>FE: 상호작용 처리 완료
    FE->>User: 변경된 피드 상태 표시
  else 피드 삭제 또는 숨김
    User->>FE: 삭제 또는 숨김 선택
    FE->>BE: 피드 상태 변경 요청
    BE->>DB: 피드 삭제 또는 숨김 상태 반영
    BE-->>FE: 상태 변경 완료
    FE->>User: 변경된 피드 목록 표시
  end
```

## 변경 기준

- 주요 기능 흐름, 사용자 액션, 외부 API 사용 여부가 바뀌면 이 문서를 수정합니다.
- API 경로만 바뀌고 기능 흐름이 같으면 이 문서보다 [API 계약](../api/api-contract.md)을 우선 수정합니다.
- 코드와 이 흐름이 다르게 동작한다면, 코드가 기준을 따를지 기준 문서를 수정할지 PR에서 명시합니다.
