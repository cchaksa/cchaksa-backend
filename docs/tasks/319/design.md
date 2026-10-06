# CodeRabbit PR 리뷰 전환 설계

## 목표

척척학사 백엔드의 PR 자동 리뷰를 CodeRabbit으로 통일하고, 현재 연결된 Gemini Code Assist를 제거한다. CodeRabbit은 한국어로 실제 결함, 보안, 계약 위반, 회귀 위험에 집중하며 저장소의 Spring 백엔드 위험 영역을 경로별로 검토한다.

## 현재 상태

- `origin/dev`에 기본 `.coderabbit.yaml`이 이미 존재한다.
- 현재 설정은 한국어, `chill`, 자동 리뷰, Draft 제외만 지정하며 경로별 검토 기준과 incremental review 설정은 없다.
- 저장소에는 Gemini 전용 workflow나 설정 파일이 없다.
- PR #316에는 Gemini Code Assist consumer version 종료 안내가 남아 있다. Gemini 제거는 코드 삭제가 아니라 GitHub App의 저장소 접근 권한을 해제하는 운영 작업이다.

## 접근 방식

전환은 다음 순서를 따른다.

1. 기존 `.coderabbit.yaml`을 백엔드 맞춤형 정책으로 보강한다.
2. 설정 파일 구조와 핵심 정책을 자동 테스트로 검증한다.
3. CodeRabbit GitHub App이 `cchaksa/cchaksa-backend`에 접근 가능한지 확인한다.
4. Ready PR에서 최초 리뷰와 후속 커밋의 incremental review를 확인한다.
5. CodeRabbit 동작 확인 후 Gemini Code Assist의 저장소 접근 권한을 제거한다.

이 순서는 자동 리뷰가 비는 구간을 피하고, 설정 파일 반영과 외부 App 권한 문제를 구분할 수 있게 한다.

## CodeRabbit 정책

### 공통 리뷰 정책

- 스키마는 CodeRabbit v2 공식 스키마를 사용한다.
- 언어는 `ko-KR`, 프로필은 `chill`을 유지한다.
- 실제 결함, 보안 문제, API·DB 계약 위반, 회귀 가능성만 지적한다.
- 취향 차이, CI가 검사하는 포맷, 요청 범위 밖 리팩터링은 제안하지 않는다.
- poem과 fortune은 비활성화한다.
- Draft PR은 리뷰하지 않고 Ready for review 이후 자동 리뷰한다.
- 후속 push는 incremental review를 활성화한다.
- 기본 브랜치 `dev`는 자동 리뷰하고, `base_branches`에는 추가 대상인 `main`, `release/*`만 지정한다.

### 경로별 검토 기준

- `global/security/**`, `domain/auth/**`는 JWT 검증, 인증 우회, 토큰·개인정보 노출, 세션 무효화를 검토한다.
- `domain/**`, `application/**`는 트랜잭션 경계, 동시성, 멱등성, 엔티티 생명주기와 API 계약 회귀를 검토한다.
- `application/portal/**`, `infrastructure/portal/**`는 콜백 검증, 재시도, 중복 처리, 외부 payload 신뢰 경계를 검토한다.
- `src/main/resources/db/migration/**`는 순방향 migration, 이전 운영 Lambda와의 호환성, 제약 조건과 데이터 보정을 검토한다.
- `global/logging/**`, `global/exception/**`는 Sentry·로그의 학번, 이메일, 토큰, 원문 payload 같은 민감정보 노출과 중복 이벤트를 검토한다.
- `src/test/**`는 변경된 실패 경계와 회귀 조건을 실제로 검증하는지 확인한다.

다른 저장소와의 연결 지식은 이번 범위에서 추가하지 않는다. 척척학사 백엔드는 현재 단일 저장소 계약만으로 리뷰할 수 있다.

## 검증

- JUnit에서 `.coderabbit.yaml`을 YAML 파서로 읽어 문법 오류를 검출한다.
- 언어, 프로필, Draft 제외, incremental review, 추가 base branch 정규식, poem·fortune 비활성화를 테스트한다.
- `./gradlew test --stacktrace --no-daemon`으로 전체 회귀 테스트를 실행한다.
- Ready PR에서 CodeRabbit 자동 리뷰가 생성되는지 확인한다.
- 같은 PR에 후속 커밋을 추가하거나 수동 명령을 사용해 incremental review를 확인한다.
- 전환 후 새 PR에서 Gemini Code Assist 댓글이 더 이상 생성되지 않는지 확인한다.

## 실패 대응

CodeRabbit 리뷰가 생성되지 않으면 다음 순서로 원인을 분리한다.

1. CodeRabbit App의 저장소 접근 권한을 확인한다.
2. `.coderabbit.yaml`이 PR base branch에 존재하는지 확인한다.
3. PR이 Draft가 아닌지와 base branch가 허용 정규식에 포함되는지 확인한다.
4. CodeRabbit 댓글의 `Configuration used`와 skip 사유를 확인한다.
5. 필요하면 `@coderabbitai review` 또는 `@coderabbitai full review`로 수동 호출한다.

CodeRabbit 확인 전에 Gemini App을 제거하지 않는다. 설정 보강으로 리뷰 품질이 나빠지면 기존 최소 설정으로 되돌릴 수 있으며, 외부 App 권한은 별도로 유지한다.

## 범위 제외

- 애플리케이션 코드, API, DB 스키마, 배포 workflow는 변경하지 않는다.
- CodeRabbit 유료 플랜이나 조직 전체 Global Overrides는 변경하지 않는다.
- 다른 척척학사 저장소와 `knowledge_base.linked_repositories`를 연결하지 않는다.
