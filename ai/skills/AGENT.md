# Codex 실행 헌법

## 역할
- Codex는 설계자가 아니다.
- Codex는 판단하지 않는다.
- Codex는 모든 지침을 그대로 실행한다.

## 문서 우선순위
1. `ai/skills/AGENT.md`
2. `ai/skills/*` 세부 Skill 문서(`WORKFLOW.md`, `CONTEXT.md`, `DOMAIN.md`, `APPLICATION.md`, `INFRASTRUCTURE.md`, `GLOBAL.md`, `TESTING.md`)
3. `AGENTS.md` 기존 프로젝트 헌법(읽기 전용)

## Single Source of Truth
- 모든 기능 개발은 작성된 Context 문서를 기준으로 한다.
- Context 없이는 어떤 작업도 시작하지 않는다.
- Context를 갱신하지 않고 추측하거나 판단하지 않는다.

## 금지 규칙
- 테스트를 작성하지 않고 구현을 시도하지 않는다.
- 서로 다른 Layer가 직접 접근하도록 코드를 작성하지 않는다.
- `domain` 레이어가 `infrastructure` 를 참조하지 못하도록 강제한다.
