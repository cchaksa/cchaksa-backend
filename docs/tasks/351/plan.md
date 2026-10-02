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
- 관리자 UI와 프론트엔드 API 어댑터를 구현했으며 서버 API와 DB는 변경하지 않았다.
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

- 확정한 프론트 계약은 #352·#354가 포함된 실제 서버와 통합 검증해야 한다.
- ACTIVE `admin_accounts` 로컬 계정과 실제 관리자 세션으로 로그인·비밀번호 변경·로그아웃을 확인해야 한다.
- dev CloudFront의 `/api/admin/*` JSON 오류 분리는 검증됐으며 SPA object 배포 뒤 extensionless fallback을 재검증해야 한다.

## 관리자 로컬 인증 피벗

이 절은 앞선 Kakao challenge/callback 인증 계획을 대체한다. 일반 사용자 Kakao 인증은 변경하지 않는다.

### 계약

- `GET /api/admin/auth/csrf`: 비인증 허용, 204, `Cache-Control: no-store`, `XSRF-TOKEN` 준비.
- `POST /api/admin/auth/signin`: `{ loginId, password }`, 200 `SuccessResponse<AdminSession>`.
- `GET /api/admin/auth/me`: 200 `SuccessResponse<AdminSession>`.
- `POST /api/admin/auth/signout`: 204, session/CSRF cookie 만료.
- `POST /api/admin/auth/password`: `{ currentPassword, newPassword }`, 204, 현재 session cookie 회전.
- 오류 응답은 `success=false`, `error.code`, `error.message` 구조이며 FE는 검증된 status와 code만 오류 객체에 남긴다.
- loginId와 password는 case-sensitive 원문 그대로 전송한다. 공백-only를 포함한 빈 값만 차단하고 기술적 상한은 각각 255자와 256자다.

### 구현 단계

1. callback route/page, Kakao button/CSS, SDK loader, provider 초기화, challenge/callback 타입과 sessionStorage 사용을 제거한다.
2. HTTP 어댑터가 실패 응답의 `error.code`만 안전하게 파싱하고 A05 세션 만료 listener를 지원하게 한다.
3. CSRF bootstrap 완료 전 제출이 비활성화되는 loginId/password 폼을 구현한다. bootstrap은 진행 중인 호출만 module single-flight로 합치고 성공·실패 정착 뒤 promise를 해제해 다음 로그인 화면 진입이 새 `/csrf` 요청을 실행하게 한다. 204 뒤 읽기 가능한 쿠키가 없으면 POST를 허용하지 않는다.
4. 로그인과 비밀번호 변경은 TanStack mutation cache를 사용하지 않는 즉시 실행 command로 구현한다.
5. 프로필 버튼에 `aria-expanded`와 연결된 메뉴를 만들고 비밀번호 변경 dialog 진입점을 추가한다.
6. dialog는 기존·신규·재확인 비밀번호를 받고 빈 값·공백-only·불일치를 로컬 검증한다. 서버에는 기존·신규 값만 원문 그대로 보낸다.
7. 비밀번호 변경 시 신규 보호 요청을 차단하고 기존 보호 query를 취소한다. 전환 전에 시작한 요청의 A05는 억제하되 password POST 자체의 A05는 세션 만료로 처리한다. 204 뒤에만 세대를 전진시키고 `/me`를 재조회한 다음 차단을 해제한다. 실패 시 기존 세대를 유지한다.
8. 보호 API의 A05만 단일 세션 만료 흐름으로 합쳐 query/mutation cache를 비우고 `/login`으로 이동한다. C04는 권한/요청 보호 오류로 유지한다.
9. 정상 signout도 신규 보호 요청을 차단하고 진행 중인 요청을 취소한 뒤 session·문의 query와 mutation cache 및 CSRF 준비 상태를 모두 제거하고 로그인 화면으로 이동한다.
10. mock은 초기 signed-out, signin, session, password change와 signout 상태를 모델링한다.

### 접근성

- 로그인 입력에는 연결된 label과 `username`, `current-password` autocomplete를 사용한다.
- 프로필 메뉴는 키보드 열기·Escape·외부 클릭 닫기와 focus 복귀를 지원한다.
- 비밀번호 dialog는 이름·설명, visible close, 초기 focus, focus containment, Escape와 opener focus 복귀를 제공한다.
- loading, 오류, 성공과 세션 만료 상태는 `aria-live` 또는 alert로 전달한다.

### 보안

- loginId와 비밀번호를 URL, query key/data, mutation variables, local/session storage, console, analytics와 오류 객체에 저장하지 않는다.
- loginId, password와 confirmation은 trim·정규화하지 않는다. 공백 검사는 제출 차단에만 사용한다.
- 로그인 성공·실패, dialog 닫기·성공, unmount와 세션 만료 시 민감 입력 상태를 초기화한다.
- 이전 session 요청의 A05가 회전된 session을 무효화하지 않도록 요청 시작 세대를 비교한다.

### 검증

- API 테스트: CSRF 선행, 정확한 signin/password body, confirmation 미전송, 204, A13/A14/A15/C01/A05/C04.
- jsdom과 DOM testing 도구를 추가하고 `npm run test`에서 로그인 검증·중복 제출 방지, 메뉴/dialog focus·Escape·복귀, 비밀번호 일치, 세션 세대 race와 A05 단일 처리를 자동 검증한다.
- 브라우저 검증: mock 로그인·비밀번호 변경·로그아웃, 데스크톱·모바일 레이아웃과 console 오류.
- mock 데스크톱·모바일: 로그인, 문의 접근, 비밀번호 변경, 로그아웃.
- `npm ci --no-audit --no-fund`, `npm run lint`, `npm run test`, `npm run typecheck`, `npm run build`, `git diff --check`.
- `rg`로 관리자 Kakao/challenge/callback/브라우저 저장소 잔존과 민감 정보 로깅을 검사한다.
- Wiki 갱신은 #352가 관리자 인증 API·DB 정본을 반영하는 단계에서 수행하며, #351은 FE 동작 문서와 PR 위험을 갱신한다.

### 계획 효력

이 피벗 절이 이 문서의 관리자 인증 정본이다. 폐기된 관리자 Kakao 인증 구현은 Git 이력에서 확인하며 현재 파일의 계약으로 사용하지 않는다. 일반 사용자 Kakao 인증은 변경하지 않는다.

## Career Extraction

- evaluated: yes.
- career signal: weak.
- career note path: none.
- action: skipped.
- unsupported metrics/outcomes: 사용자 영향, 운영 효과, 배포 성과는 아직 측정하지 않았다.

## 후속 이슈

- 관리자 인증·인가 및 세션: #352.
- 테스트 데이터 API 경계 분리: #353.
- 관리자 문의 목록·상세·답변 API: #354.
- 관리자 문의 검색 쿼리·인덱스 전략: #355.

## 피벗 검증 결과

- `npm ci --no-audit --no-fund`: 성공.
- `npm run lint`: 성공. 56개 파일을 검사했다.
- `npm test -- --run`: 성공. 8개 파일의 26개 테스트가 통과했다.
- `npm run typecheck`: 성공.
- `npm run build`: 성공.
- `git diff --check`: 성공.
- mock 브라우저에서 로그인, 프로필 메뉴, 변경 비밀번호 불일치, 비밀번호 변경 성공과 포커스 복귀를 확인했다.
- 1280px 데스크톱과 390x844 모바일에서 화면 가로 넘침이 없고 모바일 dialog가 viewport 안에 표시됨을 확인했다.
- 브라우저 warning/error가 없고, 관리자 자격 증명을 URL·브라우저 저장소·console·query/mutation cache에 기록하는 코드가 없음을 확인했다.

## 남은 위험

- #352·#354가 포함된 실제 Spring 서버와 ACTIVE 관리자 계정으로 로그인, session cookie, CSRF, 비밀번호 변경 session 회전과 로그아웃을 통합 검증해야 한다.
- 실제 API Gateway·CloudFront 환경의 cookie 전달, `/api/admin/*` 분기와 SPA fallback은 운영 준비 단계에서 검증해야 한다.
- Wiki 갱신은 #352의 관리자 인증 API·DB 정본 반영 범위이며 #351에서는 FE 작업 문서만 갱신했다.

## 독립 사전 PR 검토

- 별도 문맥에서 변경 파일과 인증 계약을 재검토했다.
- 초기 `/me` A05 안내, 세션 전환 중 일반 조회 차단, 프로필 disclosure semantics와 command 회귀 테스트 지적을 반영했다.
- 재검토 결과 `pass`이며 critical, important와 minor finding은 없다.

## Dev 배포 workflow 준비

- 운영 workflow와 분리된 수동 `deploy-dev-admin-web.yml`을 추가한다.
- dev 전용 변수 이름과 `dev.admin.cchaksa.com`을 검증해 repo-level prod bucket 변수의 fallback 사용을 차단한다.
- CloudFront distribution의 alias와 S3 origin을 dev 도메인·버킷과 대조한 뒤에만 업로드한다.
- 실행한 branch의 `github.sha`를 checkout하고 npm 전체 검증 뒤 `admin-web/dist`만 배포한다.
- 배포 순서는 immutable assets, 비버전 파일, no-cache `index.html`, CloudFront invalidation, SPA/API 검증으로 고정한다. dev 검증은 `/csrf` 204와 `no-store`를 확인해 새 인증 백엔드 readiness도 함께 판별한다.
- workflow YAML, shell mock, npm 전체 검증, 민감정보 검사와 독립 검토를 완료한 뒤 논리 커밋으로 PR #356을 갱신한다.
- 실제 workflow dispatch는 수행하지 않는다. `dev-admin-web-deploy` IAM 연결, API Gateway signin throttle, 백엔드 `feat/355` 배포·V18·alias 확인과 ACTIVE 관리자 계정 발급을 기다린다.

검증 결과:

- workflow YAML parse와 배포·검증 shell의 `bash -n`: 성공.
- shell mock: dev CloudFront alias·S3 origin 통과, prod alias 거부, 업로드·invalidation 순서 확인.
- dev routing mock: `/csrf` 204·`no-store`, SPA HTML fallback, API 비HTML 오류 통과. `/csrf` 401·`no-store` 누락과 prod URL 거부.
- `npm ci --no-audit --no-fund`, lint, 8개 파일의 26개 테스트, typecheck, build: 성공.
- 민감정보 정규식 검사와 `git diff --check`: 성공.
- 별도 문맥 재검토: critical, important와 minor finding 없이 `pass`.

남은 위험:

- workflow dispatch와 실제 AWS 업로드·권한 검증은 수행하지 않았다. dev IAM policy 연결과 백엔드 readiness 완료 뒤 최초 승인 dispatch에서 확인한다.
- 공개 dev 배포 전 API Gateway signin route throttle, 백엔드 `feat/355` 배포, V18·alias 검증과 ACTIVE 관리자 계정 준비가 필요하다.

## Localhost 인증 초기 진입 보정

- 원인: 개발 기본 proxy가 실행되지 않은 `http://localhost:8080`을 바라봐 `/me`와 `/csrf`가 502를 반환했다. 루트의 보호 경로 진입은 이 응답을 일반 세션 확인 오류로 표시했다.
- 개발 기본 proxy를 준비된 `https://dev.admin.cchaksa.com`으로 변경하고, 로컬 Spring 서버 사용 시 `.env.development.local`에서만 `http://localhost:8080`으로 덮어쓰도록 문서화했다.
- 최초 `/me`의 A05는 정상 비인증 상태이므로 만료 안내 없이 `/login`으로 이동한다. 로그인 이후 보호 API에서 발생한 A05만 기존 coordinator가 만료 안내를 표시한다.
- proxy 5xx와 네트워크 실패는 정상 401과 구분해 관리자 서버 연결 오류로 표시한다.
- 격리 Vite 서버에서 dev proxy의 `/csrf` 204·Secure XSRF cookie 전달과 `/me` 401 A05 JSON을 확인했다.
- localhost 브라우저에서 루트가 `/login`으로 이동하고 로그인 버튼이 활성화되며 alert가 없음을 확인했다. 390×844와 1280×800에서 가로 넘침 및 console warning/error가 없었다.
- public root index와 unknown route를 보호 route 바깥의 `/login` redirect로 이동해 최초 진입에서 `/me`를 호출하지 않는다. `/inquiries` 계열에서만 session query가 실행된다.
