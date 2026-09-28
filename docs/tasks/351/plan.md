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

## 초기 작업 범위

- 1단계만 수행한다.
- 서버 코드, DB migration, 배포 설정은 변경하지 않는다.
- 후속 명세가 필요한 관리자 기능 UI는 구현하지 않는다.

## UI 작업 범위

- 사용자 화면 명세는 `design.md`에 기록한다.
- 한 번에 한 단계만 구현하고 단계별로 검증·커밋한다.
- 로그인 화면과 앱 라우팅 기반을 1단계로 구현했다.
- 관리자 셸과 문의 목록을 구현했다.
- 미답변 문의 상세와 답변 작성 UI를 구현했다.
- 다음 단계는 답변 완료 문의의 읽기 전용 답변 및 담당자 표시다.

## 복구 방법

- 초기 패키지는 `admin-web` 디렉터리에 한정된다.
- 문제가 있으면 해당 디렉터리와 이 계획 문서 변경만 되돌리며 백엔드 파일에는 영향을 주지 않는다.

## 작업 기록

- `admin-web` 독립 React/Vite/TypeScript 패키지와 `package-lock.json`을 생성했다.
- 상세 관리자 화면과 서버 API/DB/배포 설정은 변경하지 않았다.
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

- 관리자 화면의 정보 구조와 API 계약은 후속 상세 명세가 필요하다.
- CI와 배포 파이프라인에는 아직 `admin-web` npm 검증 및 S3 배포 단계가 연결되지 않았다.
- `/api/admin/auth/signin` 서버 진입점과 로그인 callback 계약은 아직 구현·검증되지 않았다.

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

## 완료 점검

- 요청 재확인: 완료.
- 저장소 규칙 재확인: 완료.
- 변경 파일 검사: 완료.
- 무관한 변경: 없음.
- 문서와 구현 일치: 확인.
- 보안 민감정보 검사: 통과. 이슈 본문, 작업 문서, 소스와 명령 출력에 자격 증명이나 개인정보를 기록하지 않았다.
- 사전 PR 독립 검증: 아직 PR 생성 요청 전이므로 해당 없음.

## Career Extraction

- evaluated: yes.
- career signal: weak.
- career note path: none.
- action: skipped.
- unsupported metrics/outcomes: 사용자 영향, 운영 효과, 배포 성과는 아직 측정하지 않았다.
