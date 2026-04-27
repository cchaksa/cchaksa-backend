# Spec Bundle Guide

이 디렉터리는 기능/버그/리팩토링 단위 스펙을 Spec Kit 방식으로 분리해 관리한다. 모든 작업은 여기의 산출물을 기준으로 진행한다.

## 1. 명명 규칙
- 폴더 이름: `YYYYMMDD-<slug>` (예: `20260327-auth-studentid-separation`).
- `<slug>`는 브랜치명과 동일하게 맞춘다. 예: `feat/20260327-auth-studentid-separation` → `docs/specs/20260327-auth-studentid-separation`.
- 하나의 폴더는 하나의 Git 브랜치/Context에만 대응한다.

## 2. 폴더 구조
- `spec.md`: 요구사항, 범위, 도메인 규칙, 예외 정책, API 리스트 등 **무엇**을 정의.
- `clarify.md`: 모호점, 의사결정, 보류 항목을 추적. 모든 질문/답변을 기록하고 해결 여부를 명시.
- `plan.md`: 레이어별 구현 전략, 트랜잭션/테스트 계획 등 **어떻게**를 정의.
- `tasks.md`: 작업 분해 체크리스트와 테스트/빌드 실행 기록.
- `spec-lite.md` (선택): 작업 규모가 하루 미만이고 외부 API 계약을 건드리지 않을 때만 사용. Lite를 사용하다가 범위를 초과하면 즉시 표준 폴더로 승격한다.

## 3. 표준 vs Lite 경로
- **Standard**: 기본 경로. 모든 파일(`spec/clarify/plan/tasks`)을 작성하고 Phases 1~1.5에서 잠금.
- **Lite**: Scope < 1 day & 외부 API 변경 없음 & 도메인 영향 최소일 때만 허용. `spec-lite.md` 하나로 시작하고, 구현 전에 범위를 재확인한다. Lite에서 Standard로 전환할 때는 기존 내용을 `spec.md`로 옮기고 나머지 파일을 생성한다.

## 4. 생성 방법
- 스크립트: `./scripts/new-spec.sh <YYYYMMDD-slug> [--lite]`
  - Standard: 디렉터리와 4개 파일을 생성.
  - Lite: `spec-lite.md`만 생성.
- 수동 생성 시에도 동일한 구조를 따라야 하며, 템플릿은 `codex/skills/templates/*.md`에 있다.

## 5. 기존 Context 마이그레이션
1. 기존 `docs/context/<file>.md`를 복사해 새 폴더 `docs/specs/<YYYYMMDD-slug>`를 만든다.
2. 본문은 `spec.md`에 붙여 넣고 템플릿 형식에 맞춰 정리한다.
3. 마이그레이션 사실을 `clarify.md` 상단에 `> Legacy context migrated on 2026-03-27` 형태로 기록한다.
4. `plan.md`와 `tasks.md`를 채워 현재 상태를 반영한다.
5. `docs/context/<file>.md`에는 다음 한 줄만 남겨 앞으로 참조가 새 폴더로 이동했음을 알린다.
   ```markdown
   > Moved to `docs/specs/<YYYYMMDD-slug>` on 2026-03-27.
   ```
6. 모든 소비자가 새 경로를 사용하기 시작하면 Context 파일을 삭제한다.

## 6. 진행 중 작업 전환
- 이미 Context 기반으로 시작한 작업은 구현 단계에 들어가기 전에 새 폴더를 생성해 내용을 이관한다.
- 전환 단계에서 생긴 모호점은 반드시 `clarify.md`에 질문/답변 형식으로 정리한다.
- `plan.md`와 `tasks.md`를 작성하기 전에는 Phase 2로 넘어가지 않는다.

## 7. 자동화/Spec Kit 연동 계획
- 현재 `.specify/`나 Spec Kit CLI는 설치되어 있지 않다.
- 폴더 구조와 파일 네이밍은 Spec Kit 권장 사항과 동일하므로, 추후 `specify init`을 실행해도 충돌 없이 합칠 수 있다.
- CLI 도입 시 이 디렉터리가 기본 루트가 되며, 추가 메타데이터 파일이 생기더라도 기존 산출물은 그대로 유지한다.

## 8. 검사 체크리스트
- `rg -n "docs/context"` 실행 시 호환성 스텁 외 참조가 없어야 한다.
- `spec.md`에는 Scope/Rule/API/Exception이 빠짐없이 작성돼야 한다.
- `tasks.md`에는 테스트 커맨드(`./gradlew test` 등)와 실행 결과가 기록돼야 한다.
