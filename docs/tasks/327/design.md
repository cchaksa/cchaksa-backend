# Google Java Style 및 Javadoc 품질 기준 도입 설계

## 연결 이슈

- [#327 Google Java Style 및 Javadoc 품질 기준 도입](https://github.com/cchaksa/cchaksa-backend/issues/327)

## 목표

척척학사 백엔드의 전체 Java 소스에 Google Java Style 기반의 자동 검사와 일관된 Javadoc 기준을 도입한다. 로컬과 CI에서 같은 명령으로 서식, 정적 스타일, 테스트를 검증하며, 사람과 코딩 에이전트가 같은 개발·커밋·PR 규칙을 따르게 한다.

## 현재 상태

- `src/main/java`에 Java 소스 277개, `src/test/java`에 87개가 있으며 Javadoc 주석(`/**`)이 있는 파일은 전체 364개 중 20개다.
- Gradle에 Spotless와 Checkstyle이 설정되어 있지 않다.
- CI는 `./gradlew check --stacktrace --no-daemon`을 실행하지만 `AGENTS.md`와 `CONTRIBUTING.md`는 코드 변경 후 `test`만 요구한다.
- 커밋 형식과 브랜치 전략은 척척학사 규칙이 이미 있으므로 유지한다.

## 설계 원칙

1. 기능, 공개 API 계약, DB 스키마, 도메인 동작은 변경하지 않는다.
2. 신규 코드뿐 아니라 기존 main/test Java 소스 전체를 한 번에 기준선에 맞춘다.
3. 기존 위반을 숨기는 프로젝트 전용 suppression이나 baseline 파일은 만들지 않는다. 공식 Google Checks 자체의 규칙별 내장 예외는 유지하며, google-java-format과 Checkstyle이 다르게 들여쓰는 multiline record의 닫는 괄호만 해당 `RPAREN` AST 노드로 한정해 예외 처리한다.
4. 자동 포맷 변경과 의미가 있는 Javadoc·스타일 수정을 논리적으로 분리한다.
5. Java 17, Spring Boot 3.2.5, Gradle, 기존 패키지 구조와 Lambda/SQS 운영 구조를 유지한다.
6. Landit의 검증 방식과 문서 구조를 참고하되 척척학사의 이슈 번호, 브랜치, 커밋 규칙을 우선한다.

## 검사 도구

### Spotless

- Gradle 플러그인 `com.diffplug.spotless` 8.8.0을 사용한다.
- Java 17에서 실행 가능한 `google-java-format` 1.28.0을 main/test Java 소스에 적용한다.
- `spotlessApply`는 서식을 고치는 명령, `spotlessCheck`는 변경 없이 위반을 검사하는 명령으로 사용한다.
- 전체 소스의 최초 포맷 결과는 별도 커밋으로 분리해 이후 의미 변경을 검토하기 쉽게 한다.

### Checkstyle

- Gradle Checkstyle 플러그인과 Java 17을 지원하는 Checkstyle 12.3.1을 사용한다.
- 공식 Google Checks를 저장소의 `config/checkstyle/google_checks.xml`로 관리한다.
- main/test 소스를 모두 검사하고 경고가 아닌 오류로 처리한다.
- 기존 위반도 프로젝트 전용 예외로 숨기지 않고 실제 코드를 수정한다.
- Google Checks를 척척학사 규칙과 충돌 없이 적용하기 위한 최소 설정만 문서화해 관리한다.

## Javadoc 기준

- public/protected 타입과 메서드는 호출자가 알아야 할 계약을 Javadoc으로 설명한다.
- 모든 매개변수에는 `@param`, 반환값이 있으면 `@return`, 호출자가 대응해야 하는 예외에는 `@throws`를 작성한다.
- record 구성요소는 record 선언의 `@param`으로 설명한다.
- `@Override`, `@Test` 메서드는 상위 계약이나 테스트 이름으로 의도가 충분히 드러나는 경우 메서드 Javadoc 검사 대상에서 제외한다.
- private 요소는 복잡한 비즈니스 의도, 제약, 부작용이 코드만으로 명확하지 않을 때만 Javadoc을 작성한다.
- 클래스명이나 메서드명을 그대로 반복하는 문장이 아니라 책임, 전제조건, 부작용, 실패 조건을 설명한다.
- 형식 충족 여부는 Checkstyle로 검사하고, 설명의 의미와 정확성은 코드 리뷰에서 확인한다.

## 척척학사 로컬 스타일

- 새 Java 소스 파일은 package 선언보다 위의 첫 줄에 파일 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 생성 파일, 설정 파일, lockfile에는 파일 역할 주석 규칙을 적용하지 않는다.
- wildcard import를 사용하지 않는다.
- 테스트 메서드명도 Checkstyle의 메서드 명명 규칙을 따른다.
- 파일 역할 주석의 내용 품질은 Checkstyle로 자동 판정하지 않고 리뷰에서 확인한다.

## 기존 코드 적용 방식

1. `spotlessApply`로 main/test Java 소스 전체를 기계적으로 포맷한다.
2. 포맷 결과만 별도 커밋으로 남긴다.
3. `checkstyleMain`, `checkstyleTest`가 보고하는 위반을 모두 수정한다.
4. public/protected 계약에 Javadoc을 추가하고 파라미터, 반환값, 예외 설명을 실제 동작과 대조한다.
5. 명명 규칙, 한 파일의 최상위 타입 수, 줄 길이 등 Google Checks 위반은 동작을 보존하는 범위에서 수정한다.
6. 이름 변경이 필요한 테스트 메서드는 테스트 의미만 유지한 채 변경한다. 운영 코드의 공개 심볼 변경이 필요해 보이면 자동으로 진행하지 않고 설계를 다시 검토한다.
7. 각 단계에서 관련 테스트와 전체 `check`를 실행해 기능 회귀가 없는지 확인한다.

## 커밋 규칙

- 척척학사의 `{이슈 번호} {type}: {한국어 메시지}` 형식을 유지한다.
- type은 `feat`, `fix`, `refactor`, `docs`, `comment`, `chore`, `deploy`, `test`, `rename`, `remove`를 사용한다.
- 메시지는 변경한 내용과 이유가 드러나게 작성한다.
- 한 커밋에는 한 문장으로 설명할 수 있는 논리적 변경만 담는다.
- 커밋 본문은 필요한 경우에만 사용하며 한 줄은 약 30자를 권장 기준으로 삼는다.

## 문서 변경

- `docs/development/java-style.md`에 자동 포맷, Checkstyle, Javadoc, 파일 역할 주석 기준과 실행 명령을 기록한다.
- `docs/tasks/README.md`에 `design.md`와 `plan.md`의 역할, 단일 작업 문서 원칙을 기록한다.
- `AGENTS.md`와 `CONTRIBUTING.md`의 검증·커밋·문서 규칙을 같은 기준으로 정렬한다.
- `README.md`에서 개발 규칙과 Java 스타일 문서로 진입할 수 있게 링크를 추가한다.
- `.github/PULL_REQUEST_TEMPLATE.md`에 연결 이슈, 변경 범위, 검증 결과, Wiki 갱신 여부, 남은 위험을 명시하는 항목을 반영한다.
- 아키텍처와 운영 상세 문서는 기존 Wiki를 source of truth로 유지한다. Landit의 아키텍처 문서를 로컬에 복제하지 않는다.

## CI 설계

CI에서 실패 원인을 바로 구분할 수 있도록 다음 검사를 별도 단계로 실행한다.

1. `./gradlew spotlessCheck --no-daemon`
2. `./gradlew checkstyleMain checkstyleTest --no-daemon`
3. `./gradlew test --stacktrace --no-daemon`

로컬의 최종 통합 검증 명령은 `./gradlew check --stacktrace --no-daemon`으로 통일한다. Gradle의 `check`가 Spotless, Checkstyle, 테스트를 모두 포함하는지 설정과 실행 결과로 확인한다.

## 커밋 분리 계획

1. `327 docs`: 설계와 구현 계획을 확정한다.
2. `327 chore`: Spotless와 Checkstyle 설정을 추가한다.
3. `327 refactor`: 기존 Java 소스 전체에 자동 포맷을 적용한다.
4. `327 docs`: 기존 공개 계약에 Javadoc을 추가한다.
5. `327 refactor`: 남은 Checkstyle 위반을 수정한다.
6. `327 chore`: CI와 개발 문서, PR 템플릿을 새 검증 기준에 맞춘다.

실제 위반 유형과 변경 결합도에 따라 같은 목적의 커밋은 더 작게 나눌 수 있지만 서로 다른 목적의 변경을 합치지는 않는다.

## 검증

- 포맷 적용 단계에서 `./gradlew spotlessApply --no-daemon`을 실행한다.
- `./gradlew spotlessCheck --no-daemon`이 통과해야 한다.
- `./gradlew checkstyleMain checkstyleTest --no-daemon`이 통과해야 한다.
- `./gradlew test --stacktrace --no-daemon`이 통과해야 한다.
- `./gradlew check --stacktrace --no-daemon`이 통과해야 한다.
- `git diff --check origin/dev...HEAD`가 통과해야 한다.
- diff에서 기능, API 계약, DB migration, 환경별 설정의 의도치 않은 변경이 없는지 확인한다.

## 제외 범위

- 이슈 #205, #206의 기능 또는 버그 수정.
- 패키지 구조와 아키텍처 재설계.
- Java 또는 Spring Boot 버전 업그레이드.
- DB migration, 공개 API 동작, Lambda/SQS 동작 변경.
- SpotBugs, PMD 등 추가 분석 도구 도입.
- 기존 위반을 허용하는 프로젝트 전용 suppression 또는 baseline 도입.

## 완료 조건

- main/test Java 소스 전체가 Spotless와 Checkstyle 검사를 통과한다.
- public/protected 계약의 Javadoc이 형식뿐 아니라 실제 동작과 일치한다.
- 로컬 `check`와 CI의 개별 검사 단계가 모두 통과한다.
- 개발 규칙, Java 스타일 문서, 작업 문서, PR 템플릿이 서로 모순되지 않는다.
- 기능, 공개 API, DB 스키마와 운영 동작이 변경되지 않는다.
