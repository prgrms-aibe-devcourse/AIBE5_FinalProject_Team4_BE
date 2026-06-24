---
doc_type: be_system_architecture
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-23
---

# 시스템 아키텍처

이 문서는 옷장난감 서비스의 전체 목표 시스템 구성을 정리합니다. FE, BE, DB, 외부 API, 이미지 저장소, 인증, 배포 인프라가 어떤 책임으로 연결되는지 이해하기 위한 문서입니다.

## 문서 기준

- MVP와 운영 배포까지 고려한 프로젝트 목표 구성 요소를 포함합니다.
- 일부 구성 요소는 현재 로컬 개발 또는 구현 진행 상태와 다를 수 있습니다.
- 현재 BE 코드와 목표 구조가 다른 항목은 [BE 구현 정합성 현황](../backend/implementation-gaps.md)을 함께 확인합니다.
- 현재 프로젝트 목표에 없는 Kafka, WebSocket, GitHub Webhook 기반 서비스는 포함하지 않습니다.
- 세부 패키지 책임은 [패키지 구조](./package-structure.md)를 따릅니다.
- API 경로와 요청/응답 상세는 [API 계약](../api/api-contract.md)을 따릅니다.
- DB 테이블 책임과 관계는 [ERD](../database/erd.md)를 따릅니다.

## 구성 요소

| 영역 | 구성 요소 | 역할 |
| --- | --- | --- |
| Client | React, Vite, TypeScript | 사용자 화면, 라우팅, API 호출 |
| API Layer | Spring Boot REST API | 인증, API 요청 처리, 도메인 서비스 호출 |
| API Docs | SpringDoc, Swagger | API 명세 확인 |
| Auth | Spring Security, OAuth2, JWT | 소셜 로그인, 인증 토큰 발급/검증 |
| Token Store | Redis | Refresh Token 저장 |
| Domain | User, Wardrobe, Clothes, Catalog, Recommendation, Outfit, Feed | 핵심 비즈니스 기능 |
| Data | MySQL, RDS, JPA | 서비스 데이터 저장 |
| Image Storage | AWS S3 | 옷 이미지, 피드 이미지 저장 |
| External API | Gemini, Naver Shopping, Weather, OAuth Provider | AI 분석, 외부 상품 조회, 날씨 보조 정보, 소셜 인증 |
| Infra | EC2, Docker Compose, GitHub Actions | 실행 환경, 로컬 인프라, CI/CD |

## 다이어그램

```mermaid
flowchart TB
  subgraph Client["Client"]
    Browser["사용자 브라우저"]
    FE["React + Vite + TypeScript<br/>React Router · Axios"]
    Browser --> FE
  end

  subgraph ApiLayer["API Layer"]
    REST["Spring Boot REST API<br/>Spring Security · OAuth2 · JWT"]
    Swagger["SpringDoc / Swagger<br/>API 명세 확인"]
  end

  subgraph DomainLayer["Domain / Service Layer"]
    Auth["Auth · User<br/>소셜 로그인 · JWT · 프로필"]
    Catalog["Catalog<br/>카테고리 · 아이템 타입 · 색상 · 스타일 · 계절"]
    Wardrobe["Wardrobe · Clothes<br/>옷장 · 보유/미보유 · 옷 등록"]
    Purchase["Purchase Capture<br/>구매내역 캡처 분석 · draft"]
    Reco["Recommendation<br/>OOTD · 취향 · 유사 · 어울리는 옷 · AI MD"]
    Outfit["Outfit · OutfitBook<br/>코디 · 코디북"]
    Feed["Feed<br/>룩피드 · 좋아요 · 댓글 · 대댓글 · 팔로우"]
  end

  subgraph DataLayer["Data / Storage"]
    DB["MySQL / RDS<br/>JPA Entity"]
    Redis["Redis<br/>Refresh Token 저장"]
    S3["AWS S3<br/>이미지 저장"]
  end

  subgraph ExternalApis["External APIs"]
    OAuth["OAuth Provider<br/>Kakao · Google · Naver"]
    Gemini["Gemini API<br/>의류 분석 · 구매내역 분석 · 추천 설명"]
    Naver["Naver Shopping API<br/>외부 상품 조회"]
    Weather["Weather API<br/>추천 보조 날씨 정보"]
  end

  subgraph Infra["DevOps / Infra"]
    EC2["AWS EC2<br/>Backend 실행"]
    Compose["Docker Compose<br/>로컬/배포 실행 보조"]
    Actions["GitHub Actions<br/>CI/CD"]
  end

  FE --> REST
  REST -.-> Swagger

  REST --> Auth
  REST --> Catalog
  REST --> Wardrobe
  REST --> Purchase
  REST --> Reco
  REST --> Outfit
  REST --> Feed

  Auth --> OAuth
  Auth --> Redis
  Auth --> DB

  Catalog --> DB
  Wardrobe --> DB
  Wardrobe --> S3
  Purchase --> S3
  Purchase --> Gemini
  Purchase --> DB

  Reco --> DB
  Reco --> Naver
  Reco --> Gemini
  Reco --> Weather

  Outfit --> DB
  Feed --> DB

  EC2 --> REST
  Compose --> REST
  Actions --> EC2

  classDef client fill:#4338ca,stroke:#a5b4fc,color:#fff;
  classDef api fill:#047857,stroke:#6ee7b7,color:#fff;
  classDef domain fill:#065f46,stroke:#34d399,color:#fff;
  classDef data fill:#1d4ed8,stroke:#93c5fd,color:#fff;
  classDef external fill:#92400e,stroke:#fbbf24,color:#fff;
  classDef infra fill:#404040,stroke:#a3a3a3,color:#fff;

  class Browser,FE client;
  class REST,Swagger api;
  class Auth,Catalog,Wardrobe,Purchase,Reco,Outfit,Feed domain;
  class DB,Redis,S3 data;
  class OAuth,Gemini,Naver,Weather external;
  class EC2,Compose,Actions infra;
```

## 변경 기준

- 인증 방식, 토큰 저장소, 외부 API, 이미지 저장소, 배포 인프라가 바뀌면 이 문서를 함께 수정합니다.
- 도메인 패키지나 책임 경계가 바뀌면 [패키지 구조](./package-structure.md)도 함께 확인합니다.
- DB 책임이나 테이블 구조가 바뀌면 [ERD](../database/erd.md)와 [데이터 생명주기](../database/data-lifecycle.md)를 함께 확인합니다.
