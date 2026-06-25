---
doc_type: be_implementation_gaps
source_of_truth: AIBE5_FinalProject_Team4_BE
last_updated: 2026-06-24
---

# BE 구현 정합성 현황

이 문서는 현재 BE 코드와 `docs/` 공식 기준 사이의 차이를 정리합니다. 차이는 곧바로 오류라는 뜻이 아니라, 실제 구현 또는 문서 기준 확정 단계에서 맞춰야 할 기준을 명확히 하기 위한 기록입니다.

기능 요구사항과 세부기능 ID의 원본은 [requirements-definition.md](../requirements/requirements-definition.md)입니다. [feature-index.md](../requirements/feature-index.md)는 요구사항 정의서의 세부기능 ID를 API, 데이터, 화면과 연결하는 빠른 참조 문서입니다.

코드가 이 문서의 목표 기준과 다르게 변경되거나, 목표 기준 자체가 바뀌면 관련 기준 문서를 같은 PR에서 수정합니다.

## 기록 기준

이 문서는 아래 경우만 기록합니다.

- 기준 문서와 현재 BE 코드 구현이 서로 다르게 읽히는 경우
- 현재 BE API 경로, 응답 DTO, DB 조회 범위, 도메인 계산 기준이 기준 문서의 기능 설명보다 좁거나 다른 경우
- FE, BE 또는 코드리뷰 담당자가 문서를 보고 현재 BE 코드를 잘못 이해할 가능성이 있는 경우
- 기준 문서가 확정 기준인지, 현재 구현 상태인지 구분이 필요한 경우
- 코드가 변경되면서 기존 gap이 해소되거나 새 gap이 생긴 경우

아직 구현되지 않은 MVP 예정 기능은 이 문서에 gap으로 기록하지 않습니다. 개발 중인 기능은 기준 문서에 명확히 정의되어 있으면 됩니다.

단, 현재 코드가 공식 기준과 다른 API 응답 범위, DTO 필드 의미, DB 조회 조건, 정렬/계산 정책을 이미 사용자 기능처럼 제공한다면 이 문서에 기록합니다.

패키지나 파일이 존재하더라도 실제 구현이 비어 있거나 placeholder 수준이면 구현 완료로 보지 않습니다. 기능 완료 여부는 기준 문서, API 계약, 실제 컨트롤러/서비스 구현, 이슈/PR 상태를 함께 확인합니다.

해소된 항목은 이 문서에 `해소`, `완료`, `resolved` 상태로 남기지 않고 삭제합니다. 일부만 해소된 경우에는 아직 남은 차이만 좁혀서 다시 작성합니다.

## 현재 코드와 목표 기준 요약

현재 `develop` 기준으로 BE 공식 문서와 구현 사이에서 별도 추적해야 할 gap은 없습니다.

## 요구사항 ID 연결표

현재 별도 추적 대상 없음.

## BE 코드와 공식 기준 확인 필요

현재 별도 확인 필요 항목 없음.

## 우선 정리 대상

현재 별도 우선 정리 대상 없음.

## 문서 변경 기준

- 이 표에 적힌 현재 구현 차이가 실제 코드 수정으로 해소되면 이 문서도 함께 수정합니다.
- 코드 변경으로 기준 문서와 구현 차이가 새로 생기면 같은 PR에서 이 문서를 갱신합니다.
- 현재 코드를 우선 기준으로 확정하기로 결정한 경우, 코드만 유지하지 않고 관련 기준 문서도 같은 PR에서 함께 수정합니다.
- API 경로, 요청 필드, 응답 필드, 오류 처리 기준이 바뀌면 [api-contract.md](../api/api-contract.md)를 같은 PR에서 수정합니다.
- 기능 범위나 세부기능 ID 기준이 바뀌면 [requirements-definition.md](../requirements/requirements-definition.md)를 먼저 확인하고, [feature-index.md](../requirements/feature-index.md)를 같은 PR에서 수정합니다.
- DB 테이블, 컬럼 의미, 삭제/보존 정책이 바뀌면 [erd.md](../database/erd.md)와 [data-lifecycle.md](../database/data-lifecycle.md)를 함께 확인합니다.
- 도메인 규칙, enum, catalog code, 점수 정책이 바뀌면 [glossary.md](../domain/glossary.md), [catalog.md](../domain/catalog.md), [invariants.md](../domain/invariants.md), [recommendation-policy.md](../features/recommendation-policy.md)를 함께 확인합니다.
- 패키지 책임이나 주요 코드 위치 기준이 바뀌면 [package-structure.md](../architecture/package-structure.md)를 같은 PR에서 수정합니다.
- 시스템 구성, 기술 스택, 화면 구조, 주요 기능 흐름이 바뀌면 [system-architecture.md](../architecture/system-architecture.md), [information-architecture.md](../architecture/information-architecture.md), [sequence-diagrams.md](../architecture/sequence-diagrams.md), [tech-stack.md](../architecture/tech-stack.md)를 함께 확인합니다.
- 공통 문서가 변경되면 FE 레포의 동일 문서도 함께 확인합니다.
