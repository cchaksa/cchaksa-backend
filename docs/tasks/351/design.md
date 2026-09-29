# 관리자 웹 UI 설계

## 목표와 범위

관리자와 CS 담당자가 별도 관리자 인증을 거쳐 문의를 조회하고, 미답변 문의에 한 번만 답변하는 React SPA를 구성한다.

- 이 문서는 #351 프론트엔드 UI와 #352·#354 공개 API의 프론트 어댑터 계약을 다룬다.
- 서버의 `SuccessResponse<T>`, UUID 식별자와 `reports` 스키마를 정본으로 사용한다.
- #351에서는 백엔드 코드, DB migration과 배포 설정을 변경하지 않는다.

## 정보 구조

| 경로 | 화면 | 역할 |
| --- | --- | --- |
| `/login` | 관리자 로그인 | challenge와 카카오 ID token을 사용한 관리자 로그인 |
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

로그인 흐름은 다음 세 경계를 유지한다.

1. 프론트 어댑터가 `GET /api/admin/auth/challenge`를 호출한다.
2. 서버가 `SuccessResponse<{ challengeId, nonce, state, javascriptAppKey, redirectUri }>`와 Path `/`의 읽기 가능한 `XSRF-TOKEN`, HttpOnly `cchaksa_admin_login` 쿠키를 발급한다.
3. 프론트는 `challengeId`와 `state`만 `sessionStorage`에 보관하고, 서버가 반환한 `javascriptAppKey`, `redirectUri`, `nonce`, `state`를 그대로 `Kakao.Auth.authorize`에 전달한다.
4. 카카오는 인가 결과를 프론트 라우트 `/login/callback`에 `code`와 `state`로 전달한다.
5. 프론트는 callback state를 보관값과 대조한 뒤 `POST /api/admin/auth/signin`에 `{ challengeId, authorizationCode, state }`를 보내고 현재 `XSRF-TOKEN` 값을 `X-XSRF-TOKEN` 헤더에 싣는다.
6. 서버는 고정 redirect URI와 REST API key, 선택적 client secret으로 authorization code를 교환하고 ID token의 서명·audience·nonce를 검증한다. 카카오 token과 client secret은 브라우저에 노출하지 않는다.
7. 성공 응답의 `data`는 `adminAccountId`, `displayName`, `adminRole`을 포함하고, 서버는 HttpOnly `cchaksa_admin_session` 쿠키를 발급한다.

카카오 JavaScript SDK는 공식 CDN의 2.8.3 파일과 고정 SRI hash를 사용한다. nonce, authorization code와 token은 브라우저 저장소, console, 오류 추적 tag에 남기지 않는다. callback의 challenge 정보는 한 번 읽으면 즉시 제거하며 state 불일치 요청은 서버로 전송하지 않는다.

- `GET /api/admin/auth/me`는 쿠키 세션으로 동일한 관리자 data를 반환한다.
- `POST /api/admin/auth/signout`은 `X-XSRF-TOKEN`과 쿠키 세션을 사용하고 204를 반환한다.
- `adminRole`은 `ADMIN` 또는 `CS_AGENT`다.
- `cchaksa_admin_session`은 HttpOnly이므로 프론트 코드에서 직접 읽지 않는다.
- `cchaksa_admin_login`도 HttpOnly이므로 프론트에서 읽거나 저장하지 않고 challenge와 signin 요청의 `credentials: include`로만 왕복한다.
- HTTP 어댑터는 모든 상태 변경 요청 직전에 `XSRF-TOKEN`을 다시 읽어 회전된 값을 사용한다.
- CSRF token이 없으면 상태 변경 요청을 네트워크 전송 전에 차단한다.

로그인 오류는 서버 코드별로 다음 경계를 유지하되 UI에는 자격 증명이나 계정 존재 여부를 드러내지 않는 공통 메시지를 표시한다.

- `400 C01`: callback 요청 형식 오류.
- `401 A10`: 만료·사용 challenge, state 또는 브라우저 결합 쿠키 불일치.
- `401 A12`: Kakao authorization code 교환 실패 또는 ID token 부재.
- `401 T01`~`T10`: ID token 서명, audience, nonce 또는 만료 검증 실패.
- `403 A09`: 미등록 또는 비활성 관리자.
- `403`: CSRF token 누락 또는 불일치.

## 로컬 API 연동

- `npm run dev`는 실제 API 모드이며 Vite가 `/api/admin/*`를 기본 `http://localhost:8080`으로 proxy한다.
- proxy 대상은 브라우저에 노출되지 않는 `ADMIN_API_PROXY_TARGET`으로 변경할 수 있다.
- `npm run dev:mock`은 화면 개발이 필요한 경우에만 mock API를 사용한다.
- 실제 로그인 검증에는 백엔드 #352와 #354가 포함된 서버, 관리자 전용 Kakao 설정, ACTIVE `admin_accounts` 레코드가 필요하다.

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

- 문의 본문, 답변, 학번, 학적 스냅샷, nonce, ID token, CSRF와 세션 값을 console 또는 오류 추적 tag에 기록하지 않는다.
- API 오류는 상태 코드와 일반 사용자 메시지만 유지하며 응답 본문을 로그로 출력하지 않는다.
- 답변 관리자 정보는 서버가 인증 principal로 기록해 반환한 값만 표시한다.

## 검증

- `npm ci --no-audit --no-fund`.
- `npm run lint`.
- `npm run test`.
- `npm run typecheck`.
- `npm run build`.
- 로그인 challenge→Kakao authorize→callback→signin 요청 순서, state 검증과 CSRF header 계약 테스트.
- 문의 목록 query parameter, 상세 DTO와 답변 POST 계약 테스트.
- mock 환경의 데스크톱·모바일 브라우저 화면과 민감 정보 로깅 여부 확인.
