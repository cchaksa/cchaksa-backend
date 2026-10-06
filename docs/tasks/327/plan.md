# Google Java Style 및 Javadoc 품질 기준 도입 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 척척학사 전체 Java 소스에 Google Java Style과 계약 중심 Javadoc 기준을 적용하고 로컬·CI의 공통 품질 게이트로 만든다.

**Architecture:** Gradle에 Spotless와 Checkstyle을 추가해 포맷, 정적 스타일, 테스트를 독립적으로 실행하고 `check`에서 통합한다. 기존 소스의 기계적 포맷과 의미 있는 Javadoc·스타일 수정을 분리하며, 사람·에이전트·PR 문서를 같은 명령과 커밋 규칙으로 정렬한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Gradle, Spotless 8.8.0, google-java-format 1.28.0, Checkstyle 12.3.1, GitHub Actions.

## Global Constraints

- 기능, 공개 API 계약, DB 스키마, 도메인 동작을 변경하지 않는다.
- Java 17, Spring Boot 3.2.5, 기존 `com.chukchuk.haksa` 패키지 구조와 Lambda/SQS 구조를 유지한다.
- main/test Java 소스 전체를 검사하며 기존 위반용 프로젝트 전용 suppression 또는 baseline 파일을 만들지 않는다. 공식 Google Checks의 규칙별 내장 예외는 유지한다.
- public/protected 타입과 메서드에 호출자 관점의 Javadoc을 작성한다. `@Override`, `@Test` 메서드는 상위 계약이나 테스트 이름으로 의도가 드러나면 제외한다.
- 새 Java 파일의 첫 줄에는 파일 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 커밋은 `{이슈 번호} {type}: {한국어 메시지}` 형식으로 한 논리 변경씩 남긴다.
- 자동 포맷 커밋과 Javadoc·스타일 수정 커밋을 섞지 않는다.
- 기존 작업 디렉터리의 사용자 변경을 수정, 이동, stash 또는 삭제하지 않는다.

---

### Task 1: Gradle 품질 검사 도구 설정

**Files:**
- Modify: `build.gradle`
- Create: `config/checkstyle/google_checks.xml`

**Interfaces:**
- Consumes: Gradle Java source sets `main`, `test`와 기존 `check` lifecycle task.
- Produces: `spotlessApply`, `spotlessCheck`, `checkstyleMain`, `checkstyleTest` task와 `check` 통합 품질 게이트.

- [x] **Step 1: 도입 전 검사 task 부재를 확인한다.**

Run:

```bash
./gradlew tasks --all --no-daemon | rg 'spotless(Check|Apply)|checkstyle(Main|Test)'
```

Expected: 일치하는 task가 없어 `rg`가 exit 1을 반환한다.

- [x] **Step 2: Gradle 플러그인과 버전을 설정한다.**

`build.gradle`의 plugins 블록에 다음 항목을 추가한다.

```groovy
id 'checkstyle'
id 'com.diffplug.spotless' version '8.8.0'
```

기존 `tasks.named('test')` 위에 다음 설정을 추가한다.

```groovy
spotless {
    java {
        googleJavaFormat('1.28.0')
    }
}

checkstyle {
    toolVersion = '12.3.1'
    configFile = file('config/checkstyle/google_checks.xml')
    configProperties = ['org.checkstyle.google.severity': 'error']
    maxWarnings = 0
}

tasks.withType(Checkstyle).configureEach {
    reports {
        xml.required = true
        html.required = true
    }
}
```

- [x] **Step 3: Google Checks 설정 파일을 추가한다.**

`config/checkstyle/google_checks.xml`은 Java 17을 지원하는 Checkstyle 12.3.1의 공식 Google Checks를 기준으로 저장한다. 다음 로컬 정책만 반영한다.

```xml
<module name="MissingJavadocType">
  <property name="scope" value="protected"/>
</module>
<module name="MissingJavadocMethod">
  <property name="scope" value="protected"/>
  <property name="allowedAnnotations" value="Override, Test"/>
</module>
```

공식 Google Checks에 포함된 규칙별 내장 예외는 유지한다. 별도 suppression 파일, 기존 척척학사 코드 경로를 대상으로 한 프로젝트 전용 suppression 또는 위반 baseline은 추가하지 않는다. 공식 설정의 wildcard import 금지, 명명, 줄 길이, 한 파일의 최상위 타입 규칙은 유지한다.

- [x] **Step 4: 새 task가 등록되고 기존 위반을 탐지하는지 확인한다.**

Run:

```bash
./gradlew tasks --all --no-daemon | rg 'spotless(Check|Apply)|checkstyle(Main|Test)'
./gradlew spotlessCheck checkstyleMain checkstyleTest --no-daemon
```

Expected: 네 task가 출력된다. 품질 검사는 기존 소스 위반으로 실패하며 `build/reports/checkstyle/main.html`, `build/reports/checkstyle/test.html` 또는 콘솔에 실제 파일과 규칙이 표시된다.

- [x] **Step 5: 설정 변경을 커밋한다.**

```bash
git add build.gradle config/checkstyle/google_checks.xml
git commit -m "327 chore: Google Java Style 검사 도구 설정"
```

---

### Task 2: 기존 Java 소스 전체 자동 포맷

**Files:**
- Modify: `src/main/java/**/*.java`
- Modify: `src/test/java/**/*.java`

**Interfaces:**
- Consumes: Task 1의 `spotlessApply`, `spotlessCheck` task.
- Produces: google-java-format 1.28.0과 일치하는 전체 Java 기준선.

- [x] **Step 1: 포맷 위반이 있는 현재 기준선을 확인한다.**

Run:

```bash
./gradlew spotlessCheck --no-daemon
```

Expected: 기존 Java 소스의 포맷 위반으로 실패하고 변경 대상 파일이 출력된다.

- [x] **Step 2: 전체 Java 소스에 자동 포맷을 적용한다.**

Run:

```bash
./gradlew spotlessApply --no-daemon
```

Expected: main/test Java 파일만 google-java-format 결과로 변경된다.

- [x] **Step 3: 포맷 diff가 기계적 변경에 한정되는지 확인한다.**

Run:

```bash
git status --short
git diff --stat
git diff --word-diff=porcelain -- 'src/main/java/**/*.java' 'src/test/java/**/*.java'
```

Expected: Java 소스의 공백, 줄바꿈, import 배치 변화만 존재하고 식별자, 문자열, 수식, annotation 값은 바뀌지 않는다.

- [x] **Step 4: 포맷과 회귀 테스트를 검증한다.**

Run:

```bash
./gradlew spotlessCheck --no-daemon
./gradlew test --stacktrace --no-daemon
```

Expected: 두 명령 모두 `BUILD SUCCESSFUL`이다.

- [x] **Step 5: 자동 포맷만 커밋한다.**

```bash
git add src/main/java src/test/java
git commit -m "327 refactor: 기존 Java 소스 자동 포맷 적용"
```

---

### Task 3: main 소스 Javadoc 및 Checkstyle 위반 정리

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/**/*.java`

**Interfaces:**
- Consumes: `build/reports/checkstyle/main.html`과 Task 2의 포맷 기준선.
- Produces: `checkstyleMain`을 통과하는 운영 소스와 실제 동작에 맞는 public/protected 계약 문서.

- [x] **Step 1: main 위반 보고서를 생성하고 규칙별 개수를 기록한다.**

Run:

```bash
./gradlew checkstyleMain --no-daemon
rg -o 'source="[^"]+"' build/reports/checkstyle/main.xml | sort | uniq -c | sort -nr
```

Expected: 첫 명령은 위반으로 실패한다. 두 번째 명령은 `MissingJavadocType`, `MissingJavadocMethod`, `Javadoc*`, `MethodName`, `LineLength`, `OneTopLevelClass` 등 실제 위반 규칙과 개수를 보여준다.

- [x] **Step 2: public/protected 타입 계약을 문서화한다.**

보고서의 `MissingJavadocType` 위치를 모두 수정한다. 클래스·interface·enum·record의 선언 위에 다음 형식으로 책임과 경계를 작성한다.

```java
/**
 * 학사 도메인의 외부 요청을 처리하고 애플리케이션 서비스에 위임한다.
 */
public class ExampleController {
```

record 구성요소가 있으면 선언부 Javadoc에 모든 구성요소를 설명한다.

```java
/**
 * 조회된 학과의 식별 정보다.
 *
 * @param id 학과 식별자
 * @param name 학과 이름
 */
public record DepartmentResponse(UUID id, String name) {}
```

실제 문장은 각 타입의 구현, 생성자 주입 의존성, 호출부를 읽고 책임을 반영한다. 위 예문의 도메인명은 그대로 복사하지 않는다.

- [x] **Step 3: public/protected 메서드 계약을 문서화한다.**

보고서의 `MissingJavadocMethod`와 `Javadoc*` 위치를 모두 수정한다. 매개변수, 반환값, 호출자가 처리해야 하는 예외를 빠짐없이 기록한다.

```java
/**
 * 사용자 식별자로 졸업 요건 조회 결과를 반환한다.
 *
 * @param userId 조회할 사용자 식별자
 * @return 계산된 졸업 요건 결과
 * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
 */
public GraduationResult getGraduationResult(UUID userId) {
```

Lombok이 생성하는 접근자, `@Override` 메서드, 프레임워크 callback은 중복 설명을 만들지 않는다. 명시적으로 선언된 public/protected 생성자는 생성 책임과 모든 인자를 문서화한다.

- [x] **Step 4: Javadoc 이외의 main 위반을 동작 보존 방식으로 수정한다.**

- wildcard import는 실제 사용 타입의 명시 import로 바꾼다.
- `LineLength`는 문자열 값이나 로직을 바꾸지 않고 줄바꿈한다.
- `OneTopLevelClass`는 package-private 최상위 타입을 같은 package의 독립 파일로 이동하며 가시성을 바꾸지 않는다.
- 지역변수와 private 심볼은 참조 전체를 함께 바꿀 수 있을 때만 명명 규칙에 맞춘다.
- public/protected 심볼의 이름 변경이 필요하면 중단하고 `design.md`의 API 비변경 원칙과 대조한다.

- [x] **Step 5: main 검사와 전체 테스트를 반복 실행한다.**

Run:

```bash
./gradlew spotlessApply checkstyleMain --no-daemon
./gradlew test --stacktrace --no-daemon
```

Expected: `checkstyleMain`과 전체 테스트가 `BUILD SUCCESSFUL`이다. `src/test/java` 이외 파일의 실패는 남아 있을 수 있지만 main 보고서에는 위반이 없다.

- [x] **Step 6: main 계약 문서와 스타일 수정을 커밋한다.**

```bash
git add src/main/java
git commit -m "327 refactor: 운영 코드 스타일 및 Javadoc 정리"
```

---

### Task 4: test 소스 Javadoc 및 Checkstyle 위반 정리

**Files:**
- Modify: `src/test/java/com/chukchuk/haksa/**/*.java`

**Interfaces:**
- Consumes: `build/reports/checkstyle/test.html`, JUnit 5의 `@Test`, Task 2의 포맷 기준선.
- Produces: `checkstyleTest`를 통과하면서 테스트 동작과 의미를 보존한 테스트 소스.

- [x] **Step 1: test 위반 보고서를 생성하고 규칙별 개수를 기록한다.**

Run:

```bash
./gradlew checkstyleTest --no-daemon
rg -o 'source="[^"]+"' build/reports/checkstyle/test.xml | sort | uniq -c | sort -nr
```

Expected: 첫 명령은 남은 위반으로 실패하고 두 번째 명령은 test 소스의 실제 규칙별 개수를 출력한다.

- [x] **Step 2: 테스트 타입과 helper 계약을 문서화한다.**

- public/protected 테스트 타입에는 검증 대상과 경계를 Javadoc으로 설명한다.
- `@Test`, `@Override` 메서드는 Javadoc을 중복 작성하지 않는다.
- public/protected fixture, extension, helper 메서드는 매개변수, 반환값, 실패 조건을 실제 동작에 맞게 문서화한다.

- [x] **Step 3: 테스트 명명과 구조 위반을 수정한다.**

- 한글, underscore 또는 공백 기반 메서드명이 `MethodName`에 걸리면 lowerCamelCase로 바꾸고 기존 의미를 `@DisplayName`에 보존한다.
- wildcard import는 명시 import로 바꾼다.
- 줄 길이는 assertion 값과 기대값을 바꾸지 않고 fluent call 또는 인자를 줄바꿈한다.
- package-private helper 타입 분리가 필요하면 같은 test package의 별도 파일로 이동한다.

예시:

```java
@Test
@DisplayName("존재하지 않는 사용자는 졸업 요건을 조회할 수 없다")
void rejectsGraduationQueryForMissingUser() {
```

- [x] **Step 4: test 검사와 전체 테스트를 통과시킨다.**

Run:

```bash
./gradlew spotlessApply checkstyleTest --no-daemon
./gradlew test --stacktrace --no-daemon
```

Expected: 두 명령 모두 `BUILD SUCCESSFUL`이고 테스트 수와 실패 수가 변경 전 기준선과 일치한다.

- [x] **Step 5: 테스트 문서와 스타일 수정을 커밋한다.**

```bash
git add src/test/java
git commit -m "327 refactor: 테스트 코드 스타일 규칙 적용"
```

---

### Task 5: 개발 규칙과 스타일 문서 정렬

**Files:**
- Create: `docs/development/java-style.md`
- Create: `docs/tasks/README.md`
- Modify: `AGENTS.md`
- Modify: `CONTRIBUTING.md`
- Modify: `README.md`
- Modify: `.github/PULL_REQUEST_TEMPLATE.md`

**Interfaces:**
- Consumes: 승인된 `docs/tasks/327/design.md`, 실제 Gradle task 이름, 기존 척척학사 브랜치·Wiki 규칙.
- Produces: 사람, 코딩 에이전트, PR 작성자가 같은 스타일·검증·커밋 기준을 찾을 수 있는 문서 진입점.

- [x] **Step 1: Java 스타일 가이드를 작성한다.**

`docs/development/java-style.md`에 다음 섹션을 실제 명령과 함께 작성한다.

```markdown
# Java 스타일 가이드

## 자동 포맷
## Checkstyle
## Javadoc
## 파일 역할 주석
## import와 명명
## 로컬 검증
```

`spotlessApply`, `spotlessCheck`, `checkstyleMain checkstyleTest`, `test`, `check`의 목적을 구분한다. public/protected Javadoc의 `@param`, `@return`, `@throws`, record `@param`, `@Override`/`@Test` 예외와 private Javadoc 기준을 예시로 설명한다.

- [x] **Step 2: 작업 문서 가이드를 작성한다.**

`docs/tasks/README.md`에는 다음 계약을 기록한다.

- 경로는 `docs/tasks/{GitHub 이슈 번호}/`다.
- 설계 판단은 `design.md`, 다단계 실행·인수인계는 `plan.md`에 둔다.
- 단순 작업은 문서를 만들지 않는다.
- 새 작업에 `spec-lite.md`, `spec.md`, `clarify.md`, `tasks.md`, `checklist.md`, `context-notes.md`를 만들지 않는다.
- 발견, 계획 변경, 검증 결과는 기존 `design.md` 또는 `plan.md`에 반영한다.

- [x] **Step 3: 공통 규칙을 AGENTS와 CONTRIBUTING에 반영한다.**

- 코드·설정 변경의 최종 검증을 `./gradlew check --stacktrace --no-daemon`으로 통일한다.
- 자동 수정은 `spotlessApply`, 개별 진단은 `spotlessCheck`, `checkstyleMain checkstyleTest`, `test`로 설명한다.
- Java 작성 규칙은 `docs/development/java-style.md`를 참조하게 한다.
- 커밋 type 목록과 한 논리 변경 원칙을 `CONTRIBUTING.md`에 기록한다.
- 기존 이슈 번호 형식, `feat/{이슈 번호}`와 `dev`, Wiki `master`, Flyway, Lambda 배포 규칙은 변경하지 않는다.

- [x] **Step 4: README와 PR 템플릿의 진입점을 갱신한다.**

README의 문서 목록에 `docs/development/java-style.md`, `docs/tasks/README.md` 링크를 추가한다. PR 템플릿은 다음 제목을 포함하게 정리한다.

```markdown
## 연결 이슈
## 변경 범위
## 검증 결과
## Wiki 갱신 여부
## 남은 위험
## 리뷰 요청 사항
```

스크린샷은 API 백엔드의 모든 PR에 필수로 요구하지 않는다.
이번 변경은 아키텍처, API, 인증, DB, 배포, 운영 절차를 바꾸지 않으므로 Wiki 갱신은 불필요하다고 기록한다.

- [x] **Step 5: 문서 계약과 링크를 검증한다.**

Run:

```bash
rg -n 'spotlessApply|spotlessCheck|checkstyleMain|checkstyleTest|./gradlew check' AGENTS.md CONTRIBUTING.md docs/development/java-style.md
rg -n 'design.md|plan.md|checklist.md|context-notes.md' CONTRIBUTING.md docs/tasks/README.md
rg -n '연결 이슈|변경 범위|검증 결과|Wiki 갱신 여부|남은 위험' .github/PULL_REQUEST_TEMPLATE.md
git diff --check
```

Expected: 각 명령이 요구한 계약을 모두 찾고 `git diff --check`가 출력 없이 성공한다.

- [x] **Step 6: 개발 문서를 커밋한다.**

```bash
git add AGENTS.md CONTRIBUTING.md README.md .github/PULL_REQUEST_TEMPLATE.md docs/development/java-style.md docs/tasks/README.md
git commit -m "327 docs: Java 개발 및 커밋 규칙 정립"
```

---

### Task 6: CI 단계 분리와 최종 품질 검증

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `docs/tasks/327/plan.md`

**Interfaces:**
- Consumes: Task 1의 Gradle task와 Task 2~5의 기준선.
- Produces: 실패 원인을 포맷, Checkstyle, 테스트로 구분하는 CI와 이슈 #327 완료 증거.

- [x] **Step 1: CI의 단일 검사 단계를 확인한다.**

Run:

```bash
rg -n -C 3 './gradlew check' .github/workflows/ci.yml
```

Expected: 현재 하나의 `check` 실행 단계가 표시된다.

- [x] **Step 2: CI 검사를 세 단계로 분리한다.**

기존 Gradle 검사 step을 다음 세 step으로 교체한다.

```yaml
- name: Check Java formatting
  run: ./gradlew spotlessCheck --no-daemon

- name: Run Checkstyle
  run: ./gradlew checkstyleMain checkstyleTest --no-daemon

- name: Run tests
  run: ./gradlew test --stacktrace --no-daemon
```

Java 17 설정, Gradle cache, 기존 결과 요약과 다른 workflow는 변경하지 않는다.

- [x] **Step 3: 개별 품질 게이트를 검증한다.**

Run:

```bash
./gradlew spotlessCheck --no-daemon
./gradlew checkstyleMain checkstyleTest --no-daemon
./gradlew test --stacktrace --no-daemon
```

Expected: 세 명령 모두 `BUILD SUCCESSFUL`이다.

- [x] **Step 4: 통합 품질 게이트를 재실행한다.**

Run:

```bash
./gradlew check --rerun-tasks --stacktrace --no-daemon
```

Expected: `spotlessCheck`, `checkstyleMain`, `checkstyleTest`, `test`가 실행되고 최종 결과가 `BUILD SUCCESSFUL`이다.

- [x] **Step 5: 최종 diff와 범위를 검토한다.**

Run:

```bash
git diff --check origin/dev...HEAD
git diff --stat origin/dev...HEAD
git diff --name-status origin/dev...HEAD
git log --oneline origin/dev..HEAD
git status --short
```

Expected: DB migration과 환경 설정 변경이 없고, 커밋이 설계·도구·포맷·Javadoc·문서·CI의 논리 단위로 나뉜다. 아직 커밋하지 않은 CI와 계획 문서만 status에 표시된다.

- [x] **Step 6: CI 변경을 커밋한다.**

```bash
git add .github/workflows/ci.yml docs/tasks/327/plan.md
git commit -m "327 chore: Java 품질 검사를 CI에 분리 적용"
git status --short
```

Expected: 커밋이 성공하고 worktree가 clean이다.

- [x] **Step 7: 독립 리뷰를 수행하고 지적 사항을 검증한다.**

독립 리뷰어에게 `origin/dev...HEAD` diff와 다음 증거를 제공한다.

- Spotless, Checkstyle main/test, 전체 테스트, 통합 `check` 결과.
- 공개 API, DB migration, 환경 설정 비변경 확인.
- Javadoc이 구현과 일치하는지 표본이 아니라 전체 public/protected 계약 기준으로 검토해 달라는 요청.

차단 지적이 있으면 수정하고 관련 개별 검사와 통합 `check`를 다시 실행한다. 남은 차단 지적이 없어야 완료한다.

## 완료 기록

- 2026-08-10 기준 최신 `origin/dev`로 rebase했으며 브랜치 분기는 `0 behind`다.
- Java 내부 record 구성요소와 accessor는 camelCase로 변경하고 `@JsonProperty`로 기존 snake_case JSON 계약을 유지했다.
- Controller 구현 메서드는 `@Override`를 사용하고 API Javadoc은 `*ControllerDocs` 계약에만 유지했다.
- google-java-format과 Checkstyle의 multiline record 닫는 괄호 들여쓰기 차이는 해당 AST 노드에만 한정한 `SuppressionXpathSingleFilter`로 조정했다.
- 독립 리뷰에서 확인한 이름 반복형 Javadoc을 구현·호출부 기준의 책임, 반환 의미와 실패 조건으로 다시 작성했다.
- 이름 반복, 포괄적 요청·응답 설명과 의미 없는 `@param`·`@return` 문구는 `RegexpSinglelineJava` 품질 규칙으로 재유입을 차단했다.
- 후속 리뷰에서 public/protected 메서드의 필수 `@param`·`@return` 검사를 활성화하고 누락된 51개 태그를 보완했다.
- 정상적인 `값` 설명은 통과하고 `@param value value 값`은 실패하는 임시 probe로 품질 정규식의 오탐·미탐을 검증했다.
- 재리뷰에서 한 줄 JavaDoc과 포괄적인 전달·처리 문구가 품질 규칙을 우회하는 문제를 확인해, 한 줄·여러 줄 형식 모두와 반복형 `@param`·`@return`을 검사하도록 보완했다.
- `JavadocMethod.validateThrows`를 활성화하고 직접 발생시키는 예외의 누락된 `@throws` 계약을 보완했다.
- 테스트 메서드용 `MethodName` suppression을 제거하고 밑줄이 남은 271개 테스트 메서드를 lowerCamelCase로 변경했다.
- camelCase로 바꾼 포털·OIDC DTO가 기존 snake_case JSON을 유지하는지 요청 역직렬화와 응답·워커 메시지 필드별 assertion으로 보강했다.
- 기계적 테스트명 변경에서 text block의 SQL 테이블명까지 바뀐 1건을 diff 감사로 발견해 원복했고, 해당 단일 테스트와 전체 테스트를 다시 통과시켰다.
- 2차 독립 리뷰의 Spotless 위반, OpenAPI 소요 시간 예시 불일치, 상투적 생성자 JavaDoc, 과도한 `@return` 정규식과 조사 오류를 모두 수정했다.
- 저장소 조회 조건, 응답 생성자와 DTO에 남은 자동 생성형 설명을 전수 감사하고 구체적인 조회·반환·고정 payload 계약으로 교체했다.
- 3차 독립 리뷰에서 발견한 상투적 저장·조건 판정 문구를 품질 게이트에 추가하고, 계산형 `is*` predicate의 반환 계약을 문서화했다.
- 위 수정 후 최신 working tree를 다시 독립 리뷰한 결과 actionable finding은 없었다.
- 메인 에이전트와 리뷰어의 Gradle 동시 실행 중 공유 `build/` 출력 충돌로 48개 테스트가 `ClassNotFoundException`을 낸 뒤, 리뷰어 실행을 중단하고 단독 전체 테스트와 통합 검사를 재실행해 통과했다.
- Wiki 대상인 공개 API, 인증, DB 스키마, 아키텍처, 배포·운영 절차는 변경하지 않아 Wiki 갱신은 불필요하다.
- `./gradlew spotlessApply checkstyleMain checkstyleTest --rerun-tasks --no-daemon`: 성공.
- `./gradlew check --rerun-tasks --stacktrace --no-daemon`: 성공.
- `./gradlew test --rerun-tasks --stacktrace --no-daemon`: 388개 중 실패 0개, 오류 0개, 1개 skipped로 성공.
- `git diff --check origin/dev`와 `git diff --check`: 성공.
