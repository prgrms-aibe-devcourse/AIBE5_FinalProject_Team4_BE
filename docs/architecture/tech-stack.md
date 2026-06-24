---
doc_type: be_tech_stack
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-14
---

# 기술 스택

이 문서는 옷장난감 프로젝트의 기술 스택과 사용 목적을 정리합니다. 루트 [README](../../README.md)는 GitHub 방문자를 위한 요약을 제공하고, [기획서](../planning/project-plan.md)는 프로젝트 전체 기술 스택과 선택 근거를 함께 제공합니다. 이 문서는 docs 안에서 기술 스택을 빠르게 확인하기 위한 기준 문서입니다.

## Frontend

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| React 18.3.1 | 컴포넌트 기반 화면 구성 |
| Vite 8.0.14 | FE 개발 서버와 빌드 |
| TypeScript 6.0.3 | 타입 기반 화면, API DTO, props 검증 |
| React Router 7.15.1 | 화면 이동과 라우팅 |
| Axios 1.16.1 | API 요청과 공통 오류 처리 |
| Tailwind CSS 4.3.0 | 모바일 웹 UI 스타일링 |
| Node.js 24+ | FE 개발 서버와 빌드 실행 |

## Backend

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| Java 21 | Spring Boot 기반 서버 개발 |
| Spring Boot 3.5.14 | REST API 서버 구성 |
| Gradle | 의존성 관리, 빌드, 테스트 실행 |
| Spring Web | HTTP API 구현 |
| Spring Validation | 요청 값 검증 |
| Spring Data JPA | 객체와 테이블 매핑, CRUD 구현 |
| Spring Security / OAuth2 / JWT | 소셜 로그인, 인증, 사용자별 접근 제어 |
| Spring Data Redis | Refresh Token 저장 |
| SpringDoc OpenAPI / Swagger | API 명세 확인 |
| Lombok | 반복 코드 감소 |

## Database / Storage

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| MySQL 8.4 | 관계형 서비스 데이터 저장 |
| Redis 7 | Refresh Token 저장 |
| AWS S3 | 옷 이미지, 피드 이미지 저장 |

## External API

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| Kakao / Google / Naver OAuth | 소셜 로그인 |
| Naver Shopping API | 외부 상품 조회 |
| Gemini API | 사진 기반 의류 분석, 구매내역 분석, AI 추천 설명 생성 |
| 기상청 API | 사용자 지역 기반 날씨 보조 정보 조회 |

## Infra / DevOps

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| AWS EC2 | BE 서버 실행 |
| AWS RDS | 운영 DB |
| Docker Compose | 로컬 MySQL/Redis 등 개발 인프라 실행 |
| GitHub Actions | CI |

## Test / QA

| 기술 / 도구 | 사용 목적 |
| --- | --- |
| JUnit Platform | 테스트 실행 |
| Spring Boot Starter Test | BE 단위/통합 테스트 |
| Spring Security Test | 인증/인가 테스트 |
| H2 | 테스트용 인메모리 DB |
| Swagger UI | API 요청/응답 확인과 FE/BE 연동 검증 |

## 변경 기준

- 기술 스택이 추가, 제거, 버전 변경되면 이 문서를 수정합니다.
- 루트 README의 기술 스택 요약과 [기획서](../planning/project-plan.md)의 기술 스택도 함께 확인합니다.
- 기술 선택이 API, DB, 외부 연동, 인프라 기준에 영향을 주면 관련 기준 문서를 같은 PR에서 함께 확인합니다.
