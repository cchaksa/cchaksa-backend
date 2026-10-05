# 복수전공 졸업요건 및 탈퇴 계정 재가입 Hotfix 구현 계획

> 구현 시 `superpowers:test-driven-development`를 적용하고, 완료 주장 전 `superpowers:verification-before-completion`으로 실제 검증 결과를 확인한다.

**목표:** 복수전공 졸업요건 404와 레거시 탈퇴 계정 재가입 후 보호 API 401을 각각 회귀 테스트로 재현하고 최소 변경으로 해결한다.

**구조:** 졸업요건은 Resolver 후보 선택 조건을 보강한다. 사용자 영역은 탈퇴 계정을 병합 대상에서 제외하고 레거시 학생·인증 연결을 현재 정책대로 정리하며, 도메인 병합과 소셜 로그인 경로에도 방어를 추가한다.

**기술:** Java 17, Spring Boot 3.2.5, Gradle, JUnit 5, Mockito, AssertJ, PostgreSQL.

## 공통 제약

- `main`에서 생성한 `hotfix/333`에서 작업하고 `main`에 먼저 병합한다.
- 두 결함은 하나의 Hotfix에 포함하되 졸업요건과 사용자 생명주기 변경을 별도 논리 커밋으로 나눈다.
- 공개 API, 오류 코드, DTO, DB 스키마와 Flyway migration은 변경하지 않는다.
- 정상 사용자의 기존 계정 병합 동작은 유지한다.
- 운영 개인정보는 문서, 테스트 데이터, 커밋과 PR에 기록하지 않는다.
- 사용자 승인 전에는 Java 코드와 테스트를 변경하지 않는다.

---

### Task 1. 작업 문서 검토와 확정

**Files**

- Modify: `docs/tasks/333/design.md`
- Modify: `docs/tasks/333/plan.md`

- [ ] 사용자 검토 의견을 설계와 계획에 함께 반영한다.
- [ ] 미확정 표현과 문서 공백 오류를 검사한다.

  ```bash
  rg -n 'T[B]D|T[O]DO|implement[ ]later|위와[ ]유사' docs/tasks/333
  rg -n '[[:blank:]]+$' docs/tasks/333/design.md docs/tasks/333/plan.md
  ```

- [ ] 운영 식별자가 포함되지 않았는지 수동 검토한다.
- [ ] 승인 후 문서를 커밋한다.

  ```bash
  git add -- docs/tasks/333/design.md docs/tasks/333/plan.md
  git commit -m "333 docs: hotfix 구현 계획"
  ```

### Task 2. 복수전공 후보 선택 회귀 테스트와 수정

**Files**

- Modify: `src/test/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolverTest.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolver.java`

- [ ] 현재 주전공 후보에는 영역 요건이 없고 과거 동일 학과 후보에는 영역 요건이 있는 실패 테스트를 추가한다.
- [ ] 기존 구현이 현재 후보를 잘못 선택해 테스트가 실패하는지 확인한다.

  ```bash
  ./gradlew test --tests 'com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolverTest' --stacktrace --no-daemon
  ```

- [ ] `resolveDualMajor`의 바깥 반복문에서 `hasSingleMajorRequirement`가 false인 주전공 후보를 건너뛴다.
- [ ] 무효 주전공 후보에는 `getDualMajorRequirementsWithCache`가 호출되지 않았는지 Mockito `never()`로 검증한다.
- [ ] Resolver 집중 테스트를 다시 실행해 통과시킨다.
- [ ] 졸업요건 변경을 커밋한다.

  ```bash
  git add -- src/main/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolver.java src/test/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolverTest.java
  git commit -m "333 fix: 복수전공 주전공 후보 검증"
  ```

### Task 3. 레거시 탈퇴 계정 병합 차단

**Files**

- Modify: `src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceUnitTests.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/user/service/UserService.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/student/service/StudentDeletionService.java`

- [ ] `tryMergeWithExistingUser`가 동일 학번의 탈퇴 사용자를 찾은 상황을 단위 테스트로 구성한다.
- [ ] 기대 동작을 다음과 같이 고정하고 기존 구현에서 실패하는지 확인한다.

  - 현재 활성 User를 그대로 반환한다.
  - `absorbFrom`과 기존 User 삭제를 수행하지 않는다.
  - 기존 Student를 익명화한다.
  - 기존 SocialAccount, RefreshToken과 인증 캐시를 제거한다.

- [ ] 집중 테스트를 실행해 실패를 확인한다.

  ```bash
  ./gradlew test --tests 'com.chukchuk.haksa.domain.user.service.UserServiceUnitTests' --stacktrace --no-daemon
  ```

- [ ] `tryMergeWithExistingUser`의 자기 자신 확인 다음에 탈퇴 사용자 분기를 추가한다.
- [ ] private `cleanupLegacyWithdrawnUser(User withdrawnUser)`를 추가해 학생 익명화와 인증 연결 정리를 한곳에서 수행한다.
- [ ] `StudentDeletionService.anonymizeByStudent`가 `saveAndFlush`로 학번 UNIQUE 값을 즉시 해제하도록 변경한다.
- [ ] 소셜 계정과 토큰 삭제 후 persistence context를 flush한다.
- [ ] 활성 사용자 정상 병합 테스트와 신규 탈퇴 분기 테스트를 함께 통과시킨다.

### Task 4. 병합 도메인에서 탈퇴 상태 전파 방지

**Files**

- Modify: `src/main/java/com/chukchuk/haksa/domain/user/model/User.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/user/model/UserTests.java`

- [ ] 신규 테스트 파일 첫 줄에 역할을 설명하는 한국어 한 줄 주석을 추가한다.
- [ ] 활성 대상 User가 탈퇴 원본 User를 `absorbFrom`해도 `isDeleted=false`, `deletedAt=null`을 유지하는 실패 테스트를 작성한다.
- [ ] `User.absorbFrom`에서 `isDeleted`와 `deletedAt` 복사를 제거한다.
- [ ] 프로필, 학생 연결 등 정상 병합 속성은 기존대로 이관되는지 함께 확인한다.
- [ ] 도메인 집중 테스트를 통과시킨다.

  ```bash
  ./gradlew test --tests 'com.chukchuk.haksa.domain.user.model.UserTests' --stacktrace --no-daemon
  ```

### Task 5. 탈퇴 사용자에 연결된 소셜 로그인 방어

**Files**

- Modify: `src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceUnitTests.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/user/service/UserService.java`

- [ ] 기존 SocialAccount가 탈퇴 User를 가리키는 로그인 테스트를 추가한다.
- [ ] 로그인 결과가 기존 탈퇴 User가 아닌 신규 활성 User인지 확인한다.
- [ ] 기존 탈퇴 User의 Student 익명화와 인증 연결 삭제가 호출되는지 확인한다.
- [ ] 신규 SocialAccount가 신규 User를 가리키는지 `ArgumentCaptor` 또는 `argThat`으로 검증한다.
- [ ] `findOrCreateUser`를 명시적 분기로 바꾸고 신규 생성 코드를 `createUserWithSocialAccount`로 추출한다.
- [ ] 활성 User의 기존 SocialAccount 로그인 동작이 변하지 않았는지 기존 테스트로 확인한다.
- [ ] 사용자 생명주기 변경을 하나의 논리 단위로 커밋한다.

  ```bash
  git add -- src/main/java/com/chukchuk/haksa/domain/user/service/UserService.java src/main/java/com/chukchuk/haksa/domain/user/model/User.java src/main/java/com/chukchuk/haksa/domain/student/service/StudentDeletionService.java src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceUnitTests.java src/test/java/com/chukchuk/haksa/domain/user/model/UserTests.java
  git commit -m "333 fix: 레거시 탈퇴 계정 재가입 방어"
  ```

### Task 6. 동일 학번 재가입 통합 테스트

**Files**

- Modify: `src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceIntegrationTest.java`

- [ ] 원문 학번 Student를 가진 레거시 탈퇴 User와 별도의 신규 활성 User를 저장한다.
- [ ] 현재 탈퇴 서비스는 학번을 자동 익명화하므로, 테스트 준비 단계에서는 레거시 상태를 직접 구성한다.
- [ ] 신규 활성 User의 병합 시도를 실행한다.
- [ ] 정리 직후 신규 User에 같은 원문 학번의 Student를 저장한다.
- [ ] 다음 결과를 검증한다.

  - UNIQUE 제약 충돌이 발생하지 않는다.
  - 기존 Student 학번은 `deleted_` 접두어로 익명화된다.
  - 기존 User는 탈퇴 상태로 보존된다.
  - 신규 User는 활성 상태이며 원문 학번 Student를 소유한다.

- [ ] 통합 테스트와 사용자 영역 테스트를 실행한다.

  ```bash
  ./gradlew test --tests 'com.chukchuk.haksa.domain.user.service.UserServiceIntegrationTest' --stacktrace --no-daemon
  ./gradlew test --tests 'com.chukchuk.haksa.domain.user.*' --stacktrace --no-daemon
  ```

- [ ] 통합 회귀 테스트를 커밋한다.

  ```bash
  git add -- src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceIntegrationTest.java
  git commit -m "333 test: 레거시 탈퇴 계정 재가입 회귀 검증"
  ```

### Task 7. 포맷과 전체 회귀 검증

**Files**

- No file changes expected. 포맷에 따른 대상 파일 변경만 허용한다.

- [ ] Java 자동 포맷을 적용하고 대상 밖 파일 변경 여부를 확인한다.

  ```bash
  ./gradlew spotlessApply --no-daemon
  git status --short
  ```

- [ ] 정적 검사와 관련 집중 테스트를 실행한다.

  ```bash
  ./gradlew spotlessCheck checkstyleMain checkstyleTest --stacktrace --no-daemon
  ./gradlew test --tests 'com.chukchuk.haksa.domain.graduation.*' --stacktrace --no-daemon
  ./gradlew test --tests 'com.chukchuk.haksa.domain.user.*' --stacktrace --no-daemon
  ```

- [ ] 저장소 최종 검증을 실행한다.

  ```bash
  ./gradlew check --stacktrace --no-daemon
  git diff --check main...HEAD
  git diff --name-only main...HEAD
  ```

- [ ] 기존 미추적 `.DS_Store`와 사용자 변경이 커밋에 포함되지 않았는지 확인한다.

### Task 8. Wiki, PR과 운영 조치

**Files**

- Wiki repository의 관련 탈퇴·재가입 정책 문서.
- No application repository file changes expected.

- [ ] Wiki에서 회원 탈퇴, 재가입, 포털 연동과 계정 병합 정책을 확인한다.
- [ ] 레거시 탈퇴 계정은 신규 계정에 병합하지 않고 익명화한다는 정책을 반영한다.
- [ ] PR 본문에 Wiki 링크, Issue #333, 변경 범위, 검증 결과와 남은 위험을 기록한다.
- [ ] PR 작성자를 Assignee로 지정하고 BugFix 릴리즈 노트 라벨을 붙인다.
- [ ] 운영 배포 전 조회 전용 쿼리로 다음 두 유형을 분류한다.

  - 탈퇴 상태이며 Student 학번이 익명화되지 않았고 포털 미연동인 레거시 탈퇴 계정.
  - 탈퇴 상태이며 Student 학번이 익명화되지 않았고 포털 연동된 재가입 오병합 의심 계정.

- [ ] 포털 미연동 계정은 현재 탈퇴 정책에 맞게 Student와 인증 연결을 정리한다.
- [ ] 포털 연동 계정은 이력을 수동 확인하고 신규 계정임이 확인된 건만 활성 상태로 복구한다.
- [ ] 활성 복구 대상은 인증 캐시를 제거하고 사용자에게 재로그인을 안내한다.
- [ ] Hotfix 배포 후 장애 학생의 임시 주전공 값을 실제 학적 값으로 복원한다. UPDATE는 현재 임시값과 복수전공·입학년도 조건을 모두 확인하고 정확히 1행일 때만 commit한다.
- [ ] 실제 학적 상태에서 졸업요건 API 200과 보호 API 정상 응답을 확인한다.
- [ ] 운영 배포가 확인된 `main`을 `dev`와 진행 중인 `release/*` 브랜치에 역병합한다.

## 완료 기준

- Resolver가 영역 요건이 없는 주전공 후보를 선택하지 않는다.
- 탈퇴 레거시 사용자가 활성 신규 사용자에 병합되지 않는다.
- `User.absorbFrom`이 탈퇴 상태를 전파하지 않는다.
- 탈퇴 User에 연결된 소셜 계정 로그인은 신규 활성 User를 생성한다.
- 동일 원문 학번으로 신규 Student를 저장해도 UNIQUE 충돌이 없다.
- 관련 집중 테스트와 `./gradlew check --stacktrace --no-daemon`이 통과한다.
- Wiki와 PR에 탈퇴·재가입 정책, 검증 결과와 운영 보정 절차가 기록된다.
- 운영 데이터 복원 후 졸업요건 및 보호 API가 정상 응답한다.
