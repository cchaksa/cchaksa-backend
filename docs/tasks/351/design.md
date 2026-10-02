# 관리자 웹 UI 설계

## 목표와 범위

관리자와 CS 담당자가 별도 관리자 인증을 거쳐 문의를 조회하고, 미답변 문의에 한 번만 답변하는 React SPA를 구성한다.

- 이 문서는 #351 프론트엔드 UI와 #352·#354 공개 API의 프론트 어댑터 계약을 다룬다.
- 서버의 `SuccessResponse<T>`, UUID 식별자와 `reports` 스키마를 정본으로 사용한다.
- #351에서는 백엔드 코드와 DB migration을 변경하지 않으며 관리자 웹 CI·배포 workflow만 함께 관리한다.

## 정보 구조

| 경로 | 화면 | 역할 |
| --- | --- | --- |
| `/login` | 관리자 로그인 | 발급된 `loginId`와 비밀번호를 사용한 관리자 로그인 |
| `/inquiries` | 문의 목록 | 상태 필터, 정확 일치 검색, 최신 문의 조회 |
| `/inquiries/:reportId` | 문의 상세 | 문의 확인, 미답변 문의 답변, 완료 답변 조회 |

로그인 뒤 기본 진입점은 `/inquiries`이며, 초기 사이드바에는 `문의`만 노출한다.

## 프론트엔드 구조

현재 화면 수에 맞춰 Feature-Sliced Design의 일부 계층만 사용한다.

```text
src/
  app/       # 전역 provider와 라우터
  pages/     # 라우트 단위 화면
  widgets/   # 관리자 셸
  features/  # 로그인, 문의 필터, 문의 답변
  entities/  # 관리자 세션 및 문의 wire 계약·query
  shared/    # HTTP 어댑터, 설정, 자산, 공통 스타일
```

- 서버 상태는 TanStack Query로 관리한다.
- 운영 API는 `/api/admin/...` 상대 경로와 `credentials: include`를 사용한다.
- `requestJson<T>`는 공통 `SuccessResponse<T>`의 `data`를 해제해 entity 어댑터에 반환한다.
- CloudFront는 `/api/admin/*`를 API 동작으로 분리하고 프론트 경로에만 SPA fallback을 적용해야 한다.

## 관리자 인증 계약

로그인과 세션 흐름은 다음 경계를 유지한다.

1. 로그인 화면 진입 시 `GET /api/admin/auth/csrf`를 `cache: no-store`, `credentials: include`로 호출한다.
2. 응답은 204이며 서버는 Path `/`의 읽기 가능한 `XSRF-TOKEN`을 발급하거나 기존 유효 값을 유지한다.
3. CSRF 준비가 끝나고 쿠키를 읽을 수 있을 때만 로그인 제출을 허용한다.
4. `POST /api/admin/auth/signin`에 `{ loginId, password }`만 입력 원문 그대로 전송한다.
5. 성공 응답의 `data`는 `adminAccountId`, `displayName`, `adminRole`을 포함하고, 서버는 HttpOnly `cchaksa_admin_session` 쿠키를 발급한다.

- `GET /api/admin/auth/me`는 쿠키 세션으로 동일한 관리자 data를 반환한다.
- `POST /api/admin/auth/signout`은 CSRF와 쿠키 세션을 사용하고 204를 반환한다. 성공 시 session과 CSRF 쿠키가 만료된다.
- `POST /api/admin/auth/password`는 `{ currentPassword, newPassword }`만 받고 204를 반환한다.
- 비밀번호 변경 성공 시 서버는 해당 관리자의 기존 세션을 모두 폐기하고 현재 브라우저의 session cookie만 회전한다. CSRF token은 유지한다.
- `adminRole`은 `ADMIN` 또는 `CS_AGENT`다.
- `cchaksa_admin_session`은 HttpOnly이므로 프론트 코드에서 직접 읽지 않는다.
- HTTP 어댑터는 모든 POST 직전에 `XSRF-TOKEN`을 다시 읽어 `X-XSRF-TOKEN`에 전달한다.
- CSRF token이 없으면 상태 변경 요청을 네트워크 전송 전에 차단한다.

자격 증명은 컴포넌트 로컬 상태와 진행 중인 요청에만 존재한다. TanStack Query의 query key, query data, mutation variables, 브라우저 저장소, URL, console과 오류 추적 정보에는 넣지 않는다. 로그인·비밀번호 변경은 캐시에 자격 증명을 남기지 않는 즉시 실행 command로 처리한다.

입력과 오류 경계는 다음과 같다.

- loginId와 비밀번호는 trim·대소문자 변환·정규화를 하지 않는다. 빈 문자열과 공백-only 값은 제출하지 않는다.
- 기술적 요청·DB 상한만 loginId 255자, password 256자로 적용하고 문자 조합, 정규식, 최소 길이와 복잡도 정책은 두지 않는다.
- 변경 비밀번호와 재확인은 프론트에서 먼저 일치 여부를 검사하며 재확인 값은 서버에 보내지 않는다.
- `401 A13`: 알 수 없는 loginId, 비밀번호 불일치와 비활성 계정을 구분하지 않는 공통 로그인 실패.
- `400 A14`: 현재 비밀번호 불일치.
- `400 A15`: 현재 비밀번호와 새 비밀번호가 동일함.
- `400 C01`: body validation 실패.
- `401 A05`: 관리자 session 없음·만료·회수. 보호 API에서 발생하면 캐시를 비우고 로그인 화면에 세션 만료 상태를 표시한다.
- `403 C04`: CSRF 오류 또는 관리자 권한 없음. 세션 만료로 오인하지 않는다.
- 실패 응답에서는 검증된 `error.code`와 HTTP status만 보존하며 원문 body, `error.details`와 자격 증명을 오류 객체에 넣지 않는다.

비밀번호 변경을 시작하면 신규 보호 요청을 잠시 차단하고 진행 중인 보호 query를 취소한다. 회전 전에 시작한 요청의 A05는 전환 중 세션 만료로 전파하지 않는다. password POST의 204를 받은 뒤에만 세대를 올리고 `/api/admin/auth/me`로 회전된 현재 세션을 재확인한 다음 차단을 해제한다. password POST가 실패하면 세대는 유지하고 차단만 해제한다.

## 로컬 API 연동

- `npm run dev`는 실제 API 모드이며 Vite가 `/api/admin/*`를 기본 `https://dev.admin.cchaksa.com`으로 proxy한다.
- 로컬 Spring 서버를 사용할 때는 브라우저에 노출되지 않는 `ADMIN_API_PROXY_TARGET`을 `.env.development.local`에서 `http://localhost:8080`으로 변경한다.
- 최초 `/api/admin/auth/me`의 401 A05는 정상 비인증 상태로 취급해 만료 안내 없이 `/login`으로 이동한다. 로그인 이후 보호 API의 A05만 session expiry coordinator가 만료 안내를 표시한다.
- proxy 연결 실패와 5xx는 정상 비인증 응답과 구분해 서버 연결 오류로 표시한다.
- `npm run dev:mock`은 화면 개발이 필요한 경우에만 mock API를 사용한다.
- 실제 로그인 검증에는 백엔드 #352와 #354가 포함된 서버와 로컬 자격 증명이 활성화된 `admin_accounts` 레코드가 필요하다.

## 문의 API 계약

모든 JSON 성공 응답은 `SuccessResponse<T>`로 감싸진다.

### 목록

- `GET /api/admin/reports?page=0&size=20`을 사용한다.
- 선택 query parameter는 `status`, `searchType`, `query`다.
- `searchType`은 `USER_ID` 또는 `STUDENT_CODE`이며 `query`와 함께 전달한다.
- 정확 일치만 지원하고 통합·부분검색은 사용하지 않는다.
- 서버 정렬은 `createdAt DESC, id DESC`다.
- 목록 항목은 `reportId`, `status`, `title`, nullable `userId`, nullable `studentCode`, `createdAt`, nullable `answeredAt`이다.
- 페이지 data는 `items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`를 포함한다.

### 상세

- `GET /api/admin/reports/{reportId}`를 사용한다.
- 상세 data는 `reportId`, `status`, `title`, `content`, nullable `userId`, `createdAt`, `updatedAt`, `submitter`, nullable `answer`를 포함한다.
- `submitter`는 nullable `submittedUserId`, `departmentId`, `departmentName`, `studentCode`, `primaryMajorId`, `primaryMajorName`, `secondaryMajorId`, `secondaryMajorName`, `transferStudent`, `admissionYear`, `graduationRequirementStatus`를 포함한다.
- UI는 사용자 UUID, 학번, 학과, 주전공, 복수전공, 편입 여부, 입학 연도와 졸업요건 상태를 표시하고 nullable 값은 `-`로 표시한다.
- `answer`는 `answer`, `answeredAt`, `adminAccountId`, `adminDisplayName`을 포함한다.

### 답변

- `POST /api/admin/reports/{reportId}/answer`에 `{ "answer": "..." }`를 전송한다.
- 요청에는 현재 `XSRF-TOKEN` 값을 `X-XSRF-TOKEN` 헤더로 전달한다.
- 성공 data는 `reportId`, `status`, `answeredAt`, `adminAccountId`, `adminDisplayName`, `adminRole`을 포함한다.
- 답변 성공 뒤 상세와 목록 query를 무효화해 서버 정본을 다시 조회한다.
- 이미 답변된 문의의 중복 답변은 409로 처리하며 답변 수정·삭제 UI는 제공하지 않는다.

## 개인정보와 오류 처리

- 문의 본문, 답변, 학번, 학적 스냅샷, loginId, 비밀번호, CSRF와 세션 값을 console 또는 오류 추적 tag에 기록하지 않는다.
- API 오류는 상태 코드와 일반 사용자 메시지만 유지하며 응답 본문을 로그로 출력하지 않는다.
- 답변 관리자 정보는 서버가 인증 principal로 기록해 반환한 값만 표시한다.

## 검증

- `npm ci --no-audit --no-fund`.
- `npm run lint`.
- `npm run test`.
- `npm run typecheck`.
- `npm run build`.
- CSRF bootstrap→자격 증명 signin 순서, 정확한 요청 body, 오류 코드와 세션 만료 계약 테스트.
- 비밀번호 일치 검증, 정확한 password body, 204 세션 회전과 이전 요청 race 테스트.
- 프로필 메뉴와 비밀번호 변경 dialog의 키보드 탐색, Escape, focus 복귀와 상태 안내 확인.
- 문의 목록 query parameter, 상세 DTO와 답변 POST 계약 테스트.
- mock 환경의 데스크톱·모바일 브라우저 화면과 민감 정보 로깅 여부 확인.

## Dev 배포 계약

- `.github/workflows/deploy-dev-admin-web.yml`은 수동 dispatch만 허용하고 GitHub Environment `dev`를 사용한다.
- dispatch에서 선택한 ref의 정확한 `github.sha`를 checkout하며 prod workflow나 repo-level `ADMIN_WEB_S3_BUCKET`을 재사용하지 않는다.
- dev 전용 변수는 `DEV_ADMIN_WEB_S3_BUCKET`, `DEV_ADMIN_WEB_CLOUDFRONT_DISTRIBUTION_ID`, `DEV_ADMIN_WEB_BASE_URL`과 기존 `DEV_AWS_REGION`이다.
- AWS 자격 증명 이름은 기존 dev Lambda workflow와 같은 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 사용하며 값은 코드·문서·로그에 기록하지 않는다.
- 해시된 `assets/`를 immutable cache로 먼저 보존 업로드하고, 비버전 파일을 `assets/*`와 `index.html` 제외 후 `--delete`로 동기화한다.
- `index.html`은 `no-cache,no-store,must-revalidate`로 마지막에 교체하고 `/*` invalidation 완료를 기다린 뒤 dev SPA/API 분리를 검증한다.
- AWS 업로드 전에 CloudFront alias가 `dev.admin.cchaksa.com`이고 origin이 지정된 dev S3 버킷인지 확인해 prod distribution 오입력을 차단한다.
- dev 검증 wrapper는 `https://dev.admin.cchaksa.com` 외 URL을 거부한다. `/api/admin/auth/csrf`의 204·비HTML·`Cache-Control: no-store`, extensionless SPA route의 HTML 200과 `/api/admin/auth/me`의 비HTML 401 또는 403을 확인한다.
- 실제 dispatch와 S3 업로드는 dev IAM 연결, 백엔드 signin throttle, `feat/355` 배포, V18과 alias 검증 및 ACTIVE 관리자 계정 준비 뒤 오케스트레이터 승인으로 수행한다.
