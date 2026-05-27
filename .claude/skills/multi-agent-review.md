# Multi-Agent 코드 리뷰 패턴

Spring Boot / Java 21 프로젝트에서 security-reviewer, performance-reviewer, style-reviewer
세 에이전트를 병렬로 실행하여 PR 코드를 자동 리뷰하는 패턴입니다.

## 에이전트 파일 구조 (frontmatter)

```yaml
---
name: <에이전트 이름>
description: |
  이 에이전트가 언제 호출되어야 하는지 구체적으로 설명.
  오케스트레이터가 이 description을 보고 호출 여부를 결정합니다.
tools:
  - Read
  - Glob
  - Grep
  - Bash  # 필요한 경우에만
model: claude-sonnet-4-5  # 복잡한 분석: sonnet / 단순 패턴: haiku
isolation: worktree        # 항상 설정 — 병렬 실행 시 파일 충돌 방지
---
```

## 에이전트 파일 위치

```
.claude/agents/
├── security-reviewer.md    # 보안 취약점 분석 (model: sonnet)
├── performance-reviewer.md # N+1, @Transactional, 성능 분석 (model: sonnet)
└── style-reviewer.md       # 네이밍, 컨벤션, DTO 변환 검사 (model: haiku)
```

## 오케스트레이터 병렬 실행 프롬프트 패턴

```
다음 파일들을 security-reviewer, performance-reviewer, style-reviewer 세 에이전트로
병렬 분석해줘.

파일 목록: [파일 경로 목록]

세 에이전트의 결과를 취합하여 심각도 순(🔴→🟡→🟢)으로 정렬하고,
중복 이슈는 하나로 합쳐서 /tmp/review/final.md 에 저장해줘.
```

## isolation: worktree 설정 이유

- 세 에이전트가 동시에 실행될 때 임시 파일 충돌 방지
- 각 에이전트가 깨끗한 Git 상태에서 독립적으로 실행
- 한 에이전트 실패가 다른 에이전트에 영향 없음

## 모델 선택 기준 (비용 최적화)

| 에이전트 | 모델 | 이유 |
|---|---|---|
| security-reviewer | sonnet | 코드 흐름·데이터 추론 필요 |
| performance-reviewer | sonnet | JPA 패턴·쿼리 분석 필요 |
| style-reviewer | haiku | 패턴 매칭 위주, 단순 검사 |

## GitHub Actions headless 모드 연동

```yaml
- name: Install Claude Code
  run: npm install -g @anthropic-ai/claude-code

- name: Run Claude Multi-Agent Review
  env:
    ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
  run: |
    claude --headless \
      --allowedTools "Read,Glob,Grep,Bash,Write" \
      -p "변경 파일들을 세 에이전트로 병렬 분석 후 /tmp/review/final.md 저장"
```

## PR 크기 제한 (비용 관리)

변경 파일 10개 초과 시 리뷰 스킵 + 경고 코멘트 게시.
대형 PR은 도메인별로 분리하여 올리도록 팀에 안내.
