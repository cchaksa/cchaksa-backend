# 관리자 웹 UI 및 문의 관리 기능 구현 계획

## 작업 기준

- GitHub 이슈: #351.
- 기능 브랜치: `feat/351`.
- 관리자 프론트엔드는 저장소 루트의 `admin-web/package.json`을 기준으로 하는 독립 npm 프로젝트로 관리한다.
- 백엔드 Gradle 빌드와 npm 빌드는 결합하지 않는다.
- 관리자 화면의 상세 정보 구조와 서버 API/DB 변경은 후속 명세 확정 뒤 진행한다.

## 접근 방식

1. 루트 npm workspace로 통합한다.
   - 장점: 여러 프론트 패키지로 확장할 때 의존성 관리가 쉽다.
   - 단점: 현재 단일 관리자 SPA에는 불필요한 루트 설정이 생긴다.
2. Gradle Node 플러그인으로 npm 빌드를 포함한다.
   - 장점: 하나의 빌드 명령으로 전체 산출물을 만들 수 있다.
   - 단점: 독립 배포 요구와 어긋나고 백엔드 빌드에 Node 환경이 결합된다.
3. `admin-web`을 독립 npm 프로젝트로 둔다.
   - 장점: `npm ci`, `npm run build`와 `admin-web/dist` 배포 계약이 명확하다.
   - 단점: 백엔드와 프론트 검증 명령을 각각 실행해야 한다.

이슈에서 합의한 독립 빌드·배포 경계를 그대로 유지하기 위해 3번을 선택한다.

## 단계

### 1. 독립 패키지 초기화

- `admin-web/package.json`에 React, Vite, TypeScript 의존성과 개발·검사·빌드 스크립트를 정의한다.
- Vite 진입점과 TypeScript 설정을 추가한다.
- 상세 화면 명세를 선점하지 않는 최소 앱 진입 화면만 둔다.
- `node_modules`와 `dist`를 `admin-web/.gitignore`에서 제외한다.

검증:

- `cd admin-web && npm install`로 `package-lock.json`을 생성한다.
- 의존성을 지운 깨끗한 상태를 재현하는 대신 lockfile 기반 `npm ci`를 실행한다.
- `npm run typecheck`와 `npm run build`를 실행한다.

### 2. 관리자 UI 구현

- `design.md`의 정보 구조와 화면 흐름을 기준으로 단계별 구현한다.
- 1단계에서 관리자 로그인과 라우팅 기반을 구현한다.
- 2단계에서 관리자 셸과 문의 목록을 구현한다.
- 3단계에서 미답변 문의의 답변 작성 UI를 구현한다.
- 4단계에서 답변 완료 문의의 읽기 전용 답변과 CS 담당자 표시를 구현한다.
- 프론트 API는 `/api/admin/...` 상대 경로만 사용한다.

### 3. 서버 및 배포 연동

- UI 계약 확정 후 관리자 인증, `admin_accounts`, 문의 관리자 API와 감사 정보를 구현한다.
- CloudFront에서 SPA 동작과 `/api/admin/*` 동작을 분리한다.
- 공개 API, 인증, DB, 배포 변경 시 관련 Wiki를 확인하고 갱신한다.

## 최초 작업 범위

- 1단계만 수행한다.
- 서버 코드, DB migration, 배포 설정은 변경하지 않는다.
- 후속 명세가 필요한 관리자 기능 UI는 구현하지 않는다.

## UI 작업 범위

- 사용자 화면 명세는 `design.md`에 기록한다.
- 한 번에 한 단계만 구현하고 단계별로 검증·커밋한다.
- 로그인 화면과 앱 라우팅 기반을 1단계로 구현했다.
- 관리자 셸과 문의 목록을 구현했다.
- 미답변 문의 상세와 답변 작성 UI를 구현했다.
- 답변 완료 문의의 읽기 전용 답변 및 담당자 표시를 구현했다.
- 인증 및 문의 API 계약과 mock/HTTP 데이터 어댑터를 연결했다.

## 복구 방법

- 초기 패키지는 `admin-web` 디렉터리에 한정된다.
- 문제가 있으면 해당 디렉터리와 이 계획 문서 변경만 되돌리며 백엔드 파일에는 영향을 주지 않는다.

## 작업 기록

- `admin-web` 독립 React/Vite/TypeScript 패키지와 `package-lock.json`을 생성했다.
- 관리자 UI와 프론트엔드 API 어댑터를 구현했으며 서버 API/DB/배포 설정은 변경하지 않았다.
- Wiki 갱신: 이번 단계에는 공개 API, 인증, DB 스키마, 배포 변경이 없어 갱신하지 않았다.

검증 결과:

- `npm ci --no-audit --no-fund`: 성공.
- `npm run typecheck`: 성공.
- `npm run build`: 성공.
- `curl -fsS http://127.0.0.1:5173/`: 개발 서버 HTML 응답 확인.
- `./gradlew check --stacktrace --no-daemon`: Java 25에서 Checkstyle task 생성 오류로 실패.
- Java 17 환경의 `./gradlew check --stacktrace --no-daemon`: 성공.
- `git diff --check`: 성공.

남은 위험:

- 관리자 인증, `admin_accounts`, 문의 API와 답변 감사 정보의 서버 구현이 필요하다.
- 확정한 프론트 계약은 서버 구현 과정에서 응답 DTO와 오류 상태를 맞춰 통합 검증해야 한다.
- CI와 배포 파이프라인에는 아직 `admin-web` npm 검증 및 S3 배포 단계가 연결되지 않았다.
- `/api/admin/auth/signin` 서버 진입점과 로그인 callback은 실제 카카오 계정으로 검증되지 않았다.
- CloudFront의 `/api/admin/*` 동작 분리와 SPA fallback은 아직 설정·검증되지 않았다.

UI 1단계 검증 결과:

- `npm run typecheck`: 성공.
- `npm run build`: 성공.
- 데스크톱 1440x900 로그인 화면 렌더링: 성공.
- 모바일 390x844 로그인 화면 렌더링: 성공.
- 브라우저 console warning/error: 없음.
- 카카오 로그인 링크: `/api/admin/auth/signin` 확인. 서버 API 미구현으로 실제 인증 이동은 실행하지 않았다.

UI 2단계 검증 결과:

- 답변 상태 필터를 URL 검색 매개변수와 동기화하고 `PENDING` 문의 3건 표시를 확인했다.
- 오류 코드 `PORTAL_ACCOUNT_LOCKED` 검색 결과가 1건으로 좁혀지는 것을 확인했다.
- 데스크톱 1440x900 문의 목록 화면 렌더링: 성공.
- 모바일 390x844 관리자 메뉴 및 문의 목록 렌더링: 성공.
- 현재 데이터는 후속 API 연동 전까지 프론트엔드 mock을 사용한다.

UI 3단계 검증 결과:

- 미답변 문의 상세에서 문의 본문, 사용자 및 학적 스냅샷, 오류 코드를 확인했다.
- 답변 입력의 공백 검증, 2,000자 제한과 글자 수 표시를 확인했다.
- 유효한 답변 입력 시 답변 등록 버튼이 활성화되는 것을 확인했다.
- 답변 전송은 후속 API 어댑터 단계에서 연결한다.

UI 4단계 검증 결과:

- 답변 완료 문의에서 등록된 답변, 처리 시각과 CS 담당자 이름을 확인했다.
- 답변 완료 문의에는 답변 입력란과 추가 답변 버튼이 렌더링되지 않음을 확인했다.
- 담당자 식별자는 화면 선택값이 아니라 API 응답의 감사 정보만 표시하는 계약으로 제한한다.

UI 5단계 검증 결과:

- TanStack Query 기반 관리자 세션, 문의 목록, 상세, 답변 mutation 상태를 연결했다.
- 개발용 mock과 운영용 `/api/admin/...` HTTP 어댑터가 같은 화면 계약을 사용한다.
- 목록 6건 조회 후 미답변 문의에 답변을 등록하고 읽기 전용 완료 상태로 전환되는 흐름을 확인했다.
- 답변 완료 후 입력란이 제거되고 API 응답의 CS 담당자 이름과 처리 시각이 표시되는 것을 확인했다.
- 운영 API와 DB는 이번 작업 범위에서 구현하지 않았다.

## 완료 점검

- 요청 재확인: 완료.
- 저장소 규칙 재확인: 완료.
- 변경 파일 검사: 완료.
- 무관한 변경: 없음.
- 문서와 구현 일치: 확인.
- 보안 민감정보 검사: 통과. 이슈 본문, 작업 문서, 소스와 명령 출력에 자격 증명이나 개인정보를 기록하지 않았다.
- 사전 PR 검증: 현재 작업 문맥에서 프론트 빌드와 저장소 전체 검사를 완료했다. 독립 리뷰는 PR 생성 전 별도로 수행할 수 있다.

## PR 준비 요약

- 독립 React/Vite 관리자 SPA에 카카오 로그인 진입점과 보호 라우팅을 구성했다.
- 문의 목록 필터·검색·페이지네이션, 문의 상세, 단일 답변 등록과 완료 답변 감사 정보 UI를 구현했다.
- TanStack Query와 mock/HTTP 어댑터를 분리하고 `/api/admin/...` 서버 계약을 `design.md`에 기록했다.
- Wiki 갱신: 서버 API, 인증, DB, 배포 구현이 없어 갱신하지 않았다. 후속 서버 단계에서 관련 Wiki 갱신이 필요하다.

```text
[COMPLETION-CHECK]
request_rechecked: yes
agents_rechecked: yes
changed_files_inspected: yes
unrelated_changes: none
documentation_consistent: yes
required_tests:
  - cd admin-web && npm ci --no-audit --no-fund: pass
  - cd admin-web && npm run build: pass
  - JAVA_HOME=<corretto-17> ./gradlew check --stacktrace --no-daemon: pass
  - git diff --check: pass
  - browser desktop/mobile and answer workflow: pass
pre_pr_verification: pass
pre_pr_verification_isolation: current-context
security_check: pass
task_note_updated: yes
career_evaluated: yes
unsupported_claims:
  - 사용자 영향, 운영 효과와 배포 성과는 측정하지 않았다.
remaining_risks:
  - 관리자 인증, API, DB, CloudFront와 실제 카카오 로그인 통합은 아직 구현·검증되지 않았다.
[END-COMPLETION-CHECK]
```

## Career Extraction

- evaluated: yes.
- career signal: weak.
- career note path: none.
- action: skipped.
- unsupported metrics/outcomes: 사용자 영향, 운영 효과, 배포 성과는 아직 측정하지 않았다.

## 서버 정본 계약 후속 정합화

- `reportId`, 사용자 식별자와 답변 관리자 식별자를 UUID 문자열로 변경한다.
- 서버에 없는 분류·오류 코드·학교·학년·학기 필드를 제거한다.
- 상세 화면을 nullable 학적 스냅샷 필드로 교체한다.
- 검색을 사용자 UUID·학번 정확 일치로 제한하고 `page=0`, `size=20`, `createdAt DESC` 계약을 기록한다.
- 로그인 GET 링크를 제거하고 nonce·CSRF 준비 provider를 통한 POST 제출 경계를 둔다.
- 미확정 challenge endpoint와 CSRF cookie/header 이름은 #352 연동 지점으로 남긴다.
- 상태 변경 요청에서 매번 CSRF provider를 읽고 토큰 누락 시 전송 전에 차단한다.
- 백엔드 코드는 변경하지 않는다.

후속 정합화 검증 결과:

- `cd admin-web && npm ci --no-audit --no-fund`: 통과.
- `cd admin-web && npm run lint`: 통과. Biome가 47개 파일을 검사했다.
- `cd admin-web && npm run test`: 통과. 3개 파일의 8개 테스트가 통과했다.
- `cd admin-web && npm run build`: 통과. TypeScript 검사와 Vite 운영 빌드가 완료됐다.
- `git diff --check`: 통과.
- 데스크톱과 모바일 브라우저에서 목록, 정확 일치 검색, 미답변 답변 입력, 완료 답변의 관리자 UUID·표시 이름, POST 로그인 버튼 경계를 확인했다.
- 브라우저 warning/error가 없으며 소스에 console, 오류 추적 tag, 브라우저 저장소 기록이 없음을 확인했다.
- 변경 범위는 `admin-web/`과 `docs/tasks/351/`에 한정되며 백엔드 코드는 변경하지 않았다.

남은 위험:

- nonce/challenge 응답 DTO와 endpoint, CSRF cookie/header 이름은 #352에서 확정한 뒤 준비 provider와 앱 초기화에 연결해야 한다.
- 실제 관리자 세션, 문의 API와 DB 통합 검증은 #352, #354에서 수행해야 한다.

후속 이슈:

- 관리자 인증·인가 및 세션: #352.
- 테스트 데이터 API 경계 분리: #353.
- 관리자 문의 목록·상세·답변 API: #354.
- 관리자 문의 검색 쿼리·인덱스 전략: #355.

## #352·#354 확정 계약 최종 연동

- 공통 `SuccessResponse<T>`에서 `data`를 해제하는 HTTP 경계를 적용한다.
- `GET /api/admin/auth/challenge`가 반환한 nonce를 카카오 ID token provider에 전달한다.
- `POST /api/admin/auth/signin`에 `{ challengeId, idToken }`을 제출하고 관리자 `adminRole`을 세션 query에 저장한다.
- `XSRF-TOKEN`을 매 상태 변경 요청마다 읽어 `X-XSRF-TOKEN` header에 전달한다.
- HttpOnly `cchaksa_admin_session`은 `credentials: include`로만 사용한다.
- 문의 검색 parameter를 `searchType`으로 맞추고 목록 `userId`, 상세 `submitter`, 답변 DTO 필드명을 #354 코드와 일치시킨다.
- 답변 POST의 별도 결과 DTO를 받은 뒤 목록과 상세를 서버에서 다시 조회한다.
- 백엔드 파일은 변경하지 않는다.

확인된 서버 기준:

- #352 공개 계약 통일 커밋 `79c5ba00`에서 `cchaksa_admin_session`과 `adminRole`을 확인했다.
- #352 CSRF 경로 보완 커밋 `26a106c4`에서 `XSRF-TOKEN`의 Path `/`와 통합 테스트를 확인했다.
- 재배치된 `feat/354`에서 #352 인증 계약과 #354 문의 DTO·통합 테스트를 함께 확인했다.

남은 통합 검증:

- 카카오 JavaScript SDK 초기화와 ID token 획득 구현을 `configureKakaoIdTokenProvider`에 연결해야 한다.
- 실제 API Gateway·CloudFront 환경의 쿠키 전달과 CORS 동작은 배포 환경에서 검증해야 한다.

최종 검증 결과:

- `cd admin-web && npm ci --no-audit --no-fund`: 통과.
- `cd admin-web && npm run lint`: 통과. 47개 파일을 검사했다.
- `cd admin-web && npm run test`: 통과. 3개 파일의 12개 테스트가 통과했다.
- `cd admin-web && npm run typecheck`: 통과.
- `cd admin-web && npm run build`: 통과.
- `git diff --check`: 통과.
- 데스크톱과 390x844 모바일에서 목록·상세·답변 등록·완료 답변·로그인 버튼 흐름을 검증했다.
- 모바일 페이지 전체에는 가로 넘침이 없고 목록 표 컨테이너만 의도대로 가로 스크롤된다.
- 답변 입력의 5,000자 제한과 답변 후 읽기 전용 전환을 확인했다.
- 브라우저 warning/error는 없었다.
- 소스에 console, 오류 추적 tag, 브라우저 저장소 기록이 없음을 확인했다.

```text
[PRE-PR-VERIFY]
verdict: pass
isolation: current-context
scope_match: yes
agents_boundary: pass
files_reviewed:
  - admin-web의 변경된 인증, HTTP, 문의, UI, 테스트 파일 전체
  - docs/tasks/351/design.md
  - docs/tasks/351/plan.md
files_not_reviewed:
  - none
commands:
  - npm ci --no-audit --no-fund: pass
  - npm run lint: pass
  - npm run test: pass, 12 tests
  - npm run typecheck: pass
  - npm run build: pass
  - git diff --check: pass
  - browser desktop/mobile and answer/login workflow: pass
findings:
  critical:
    - none
  important:
    - none
  minor:
    - none
unsupported_claims:
  - 사용자 영향과 운영 효과는 측정하지 않았다.
remaining_risks:
  - 카카오 SDK adapter와 실제 배포 환경은 이 저장소에서 검증하지 못했다.
  - 검증은 구현과 같은 문맥에서 수행돼 독립성이 제한된다.
recommended_next_action: open-pr
[END-PRE-PR-VERIFY]
```

```text
[SECURITY-CHECK]
verdict: pass
checked_surfaces:
  - changed source, tests, task docs, browser logs, command output
findings:
  critical:
    - none
  important:
    - none
  minor:
    - none
redactions:
  - none
unsupported_security_claims:
  - 실제 배포 환경의 쿠키와 CORS 보안 동작은 검증하지 않았다.
required_follow_up:
  - none
[END-SECURITY-CHECK]
```

```text
[COMPLETION-CHECK]
request_rechecked: yes
agents_rechecked: yes
changed_files_inspected: yes
unrelated_changes: none
documentation_consistent: yes
required_tests:
  - npm ci --no-audit --no-fund: pass
  - npm run lint: pass
  - npm run test: pass, 12 tests
  - npm run typecheck: pass
  - npm run build: pass
  - git diff --check: pass
  - browser desktop/mobile and answer/login workflow: pass
pre_pr_verification: pass
pre_pr_verification_isolation: current-context
security_check: pass
task_note_updated: yes
career_evaluated: yes
unsupported_claims:
  - 사용자 영향과 운영 효과는 측정하지 않았다.
remaining_risks:
  - 카카오 SDK adapter와 실제 API Gateway·CloudFront 통합은 후속 검증이 필요하다.
[END-COMPLETION-CHECK]
```
