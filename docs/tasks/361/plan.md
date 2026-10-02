# #361 작업 계획

## Phase A

- [x] GitHub 이슈 #361을 생성하고 `origin/feat/355` 기반 격리 `feat/361` worktree를 준비한다.
- [x] 런타임 참조와 V16~V19 migration, version 99 호환 경계를 확인한다.
- [x] 세 가지 접근을 비교하고 확장-축소 방식과 recovery 조건을 설계한다.
- [x] `AdminAccount`의 관리자 Kakao 필드 매핑을 제거한다.
- [x] V20에서 `admin_login_challenges` 테이블을 제거한다.
- [x] H2 migration 및 실제 PostgreSQL V19→V20 schema validation 테스트를 갱신한다.
- [x] 관리자 로그인·세션 회귀와 `./gradlew check --stacktrace --no-daemon`을 실행한다.
- [ ] 커밋·push 후 `feat/355` 대상 Draft PR을 만들고 검증과 Wiki 후속을 기록한다.
- [ ] 오케스트레이터에 Phase A SHA를 보고하고 dev 배포를 기다린다.

## Phase B

- [ ] Phase A Lambda의 dev `Active/Successful`, `live` alias 전환과 smoke 완료를 확인한다.
- [ ] dev PostgreSQL에서 관리자 제약 이름과 컬럼을 읽기 전용으로 재확인한다.
- [ ] V21에서 provider/social 제약을 먼저 제거하고 두 컬럼을 삭제한다.
- [ ] 최종 PostgreSQL schema validation, migration, 로그인·세션 회귀와 전체 `check`를 실행한다.
- [ ] 같은 브랜치와 PR에 논리 커밋을 누적하고 새 target SHA를 보고한다.
- [ ] 별도 승인 전 배포하지 않는다.

## 계획 약점 검토

[PLAN-REVIEW]
verdict: pass
critical:
  - none
important:
  - Phase A 배포 완료 전 V21을 추가하면 migration 선실행 안전성이 사라지므로 Phase B 진입 조건을 강제한다.
  - `origin/dev`에 선행 관리자 스택이 없어 `origin/feat/355` 기반 stacked branch와 PR이 필요하다.
minor:
  - GitHub Actions의 Node.js 20 deprecation 경고는 이 이슈 범위 밖의 workflow 유지보수 항목이다.
questions:
  - none
recommended_changes:
  - PostgreSQL V19 fixture에서 V20 적용과 전체 Hibernate schema validation을 함께 실행한다.
  - Phase B 직전에 실제 dev constraint 이름을 read-back하고 drift를 숨기지 않는 DDL을 확정한다.
[END-PLAN-REVIEW]
