# Spec Lite Template

> 2026-03-28: Standard Spec (`spec.md`, `clarify.md`, `plan.md`, `tasks.md`)로 승격 완료. Lite 내용은 초기 CI/CD 조사 기록 용도로만 보관한다.

## Summary
- Purpose: CI/CD 구성 요소(빌드, 테스트, 배포 파이프라인)를 조사해 현재 흐름을 파악한다.
- Scope (In/Out): In — `.github/workflows` 내 GitHub Actions 정의, 관련 `scripts/` 및 `docker/` 도움 스크립트, README/문서에서 CI 언급. Out — 파이프라인 수정, 신규 배포 구성 추가.
- Expected Impact: Issue #198 대응을 위해 현재 자동화 단계를 명확히 문서화해 이후 개선/상담의 근거를 마련한다.

## Key Rules
- Rule 1: 조사만 수행하고 코드/설정을 변경하지 않는다.
- Rule 2: 발견 내용은 최소 2개 이상의 근거 파일/섹션을 교차 확인(Double check)한다.

## Risks / Assumptions
- Risk: 일부 파이프라인 문서화가 최신이 아닐 수 있어 실제 GitHub Actions 설정과 어긋날 수 있다.
- Assumption: GitHub Actions가 단일 CI/CD 진입점이며, self-hosted runner나 외부 서비스는 `.github/workflows`에 명시돼 있다.

## Tasks
- [x] `.github/workflows/*.yml` 분석으로 CI 단계/조건 파악
- [x] 배포 관련 스크립트/도커 설정 교차 확인
- [x] 분석 결과 정리 및 spec-lite/답변 업데이트

## Findings (2026-03-28)
- `.github/workflows`에는 autofix, issue-linker, dev/prod EC2, dev/prod Lambda 6개 workflow만 존재하며 모두 GitHub Actions에서 관리한다.
- CI 측면에서는 `autofix.yml` 외 별도 테스트/빌드 workflow가 없어서 PR/PUSH 시 Gradle 테스트가 자동 실행되지 않는다.
- 배포는 `workflow_dispatch` 기반으로 Dev/Prod 각각 EC2와 Lambda가 분리돼 있고, EC2 job은 dev/main 브랜치를 체크아웃해 Jar를 빌드한 후 SCP/SSH로 롤백 가능한 방식으로 배포하며 Lambda job은 `lambdaZip` 산출물을 S3에 올려 alias를 전환한다.

> Lite 스펙은 Scope < 1 day & API 변경 없음일 때만 허용. 조건을 벗어나면 즉시 Standard 스펙으로 승격한다.
