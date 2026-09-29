# 관리자 웹 UI 설계

## 목표와 범위

관리자와 CS 담당자가 별도 관리자 인증을 거쳐 문의를 조회하고, 미답변 문의에 한 번만 답변하는 React SPA를 구성한다.

- 이 문서는 #351 프론트엔드 UI와 어댑터 계약을 다룬다.
- 관리자 인증·인가는 #352, 테스트 데이터 API 경계는 #353, 문의 API는 #354, 검색 쿼리·인덱스 전략은 #355에서 구현한다.
- 서버의 UUID 식별자와 `reports` 스키마를 정본으로 사용한다.
- #351에서는 백엔드 코드, DB migration과 배포 설정을 변경하지 않는다.

## 정보 구조

| 경로 | 화면 | 역할 |
| --- | --- | --- |
| `/login` | 관리자 로그인 | 관리자 POST 로그인 시작 |
| `/inquiries` | 문의 목록 | 상태 필터, 정확 일치 검색, 최신 문의 조회 |
| `/inquiries/:reportId` | 문의 상세 | 문의 확인, 미답변 문의 답변, 완료 답변 조회 |

로그인 뒤 기본 진입점은 `/inquiries`이며, 초기 사이드바에는 `문의`만 노출한다.

## 화면 흐름

1. 인증되지 않은 관리자는 `/login`에서 척척학사 로고와 카카오 로그인 버튼을 본다.
2. 로그인 버튼은 링크 이동이 아니라 관리자 로그인 mutation을 시작한다.
3. 별도 준비 단계에서 서버가 발급한 nonce와 CSRF 정보를 얻은 뒤 `POST /api/admin/auth/signin`을 호출한다.
4. 인증 성공 뒤 `/inquiries`에서 서버가 `createdAt DESC`로 반환한 문의를 본다.
5. 목록에서는 전체·답변 필요·답변 완료 상태 필터를 사용한다.
6. 검색은 `USER_ID`의 UUID 또는 `STUDENT_CODE`를 정확히 일치시키는 방식만 제공한다.
7. 미답변 문의 상세에서는 문의 본문 아래에 답변 작성 영역을 제공한다.
8. 답변 완료 문의 상세에서는 답변과 답변한 관리자의 UUID·표시 이름을 읽기 전용으로 표시한다.

## 화면 원칙

- 운영 도구답게 검색, 상태 구분, 표 가독성과 반복 작업 효율을 우선한다.
- 상태는 색상만으로 구분하지 않고 텍스트 라벨을 함께 제공한다.
- nullable 서버 스냅샷은 값이 없을 때 `-`로 표시한다.
- 문의 본문, 답변, 학번과 학적 스냅샷을 console 또는 오류 추적 tag에 기록하지 않는다.
- 답변 관리자 정보는 서버가 인증된 관리자 계정으로 기록해 반환한 값만 표시한다.

## 프론트엔드 구조

현재 화면 수에 맞춰 Feature-Sliced Design의 일부 계층만 사용한다.

```text
src/
  app/       # 전역 provider와 라우터
  pages/     # 라우트 단위 화면
  widgets/   # 관리자 셸
  features/  # 로그인, 문의 필터, 문의 답변
  entities/  # 관리자 세션 및 문의 계약·query
  shared/    # HTTP 어댑터, 설정, 자산, 공통 스타일
```

- 루트 전역 store는 두지 않고 서버 상태는 TanStack Query로 관리한다.
- feature/page는 `index.ts`를 공개 진입점으로 사용한다.
- Vite SPA와 S3 배포 형태를 유지하고 React Router 선언형 모드를 사용한다.
- CloudFront는 `/api/admin/*`를 API 동작으로 분리하고 프론트 경로에만 SPA fallback을 적용해야 한다.

## 서버 정본 계약

### 식별자와 공통 규칙

- `reportId`, `submittedUserId`, `answeredBy.adminAccountId`는 UUID 문자열이다.
- 날짜·시각은 ISO 8601 문자열로 수신한다.
- 문의 상태는 `PENDING` 또는 `ANSWERED`다.
- `category`, `errorCode`, `universityName`, `grade`, `semester`는 `reports` 스키마에 없으므로 프론트 계약에서도 사용하지 않는다.

### 문의 목록

- `GET /api/admin/reports`를 사용한다.
- 기본 요청은 `page=0`, `size=20`이며 서버 정렬은 `createdAt DESC`다.
- 선택 query parameter는 `status`, `searchField`, `query`다.
- `searchField`는 `USER_ID` 또는 `STUDENT_CODE`만 허용한다.
- 검색값은 부분검색이나 통합검색 없이 정확히 일치시킨다.
- 목록 항목은 `reportId`, `status`, `title`, nullable `submittedUserId`, nullable `studentCode`, `createdAt`을 사용한다.
- 응답 페이지는 `items`, `page`, `size`, `totalElements`, `totalPages`를 포함한다.

### 문의 상세와 답변

- `GET /api/admin/reports/{reportId}`는 목록 필드에 문의 본문과 nullable 학적 스냅샷, nullable 답변을 더해 반환한다.
- 학적 스냅샷은 `department`, `primaryMajor`, `secondaryMajor`, `isTransferStudent`, `admissionYear`, `graduationRequirementStatus`를 사용하며 각 값은 nullable이다.
- `POST /api/admin/reports/{reportId}/answer`에 `{ "answer": "..." }`를 전송한다.
- 서버는 클라이언트에서 답변 관리자 값을 받지 않고 인증된 관리자 계정을 감사 정보로 기록한다.
- 완료 답변은 `content`, `answeredAt`, `answeredBy.adminAccountId`, `answeredBy.displayName`을 포함한다.
- 이미 답변된 문의의 중복 답변은 서버가 충돌 응답으로 거부하고, 성공 시 갱신된 문의 상세를 반환한다.

### 관리자 인증과 CSRF

- 로그인 제출은 `POST /api/admin/auth/signin`을 사용하며 GET 링크 방식은 사용하지 않는다.
- 로그인 준비 provider는 서버 발급 nonce와 CSRF header 이름·값을 로그인 mutation에 전달한다.
- nonce/challenge를 발급하는 endpoint, 응답 DTO, CSRF cookie 이름과 header 이름은 #352에서 확정한다. #351은 이 값을 임의로 고정하지 않는다.
- 준비 provider가 연결되지 않은 운영 환경에서는 로그인 요청을 전송하지 않고 일반 오류 상태를 표시한다.
- HTTP 어댑터는 `POST`, `PUT`, `PATCH`, `DELETE`마다 CSRF provider를 다시 호출한다. 따라서 로그인·로그아웃 뒤 쿠키가 회전되면 다음 요청에서 새 값을 읽을 수 있다.
- cookie 이름과 header 이름이 확정되면 `createCookieCsrfTokenProvider`와 `configureCsrfTokenProvider`를 앱 초기화 시 연결한다.
- CSRF 값이 없는 상태 변경 요청은 네트워크 전송 전에 차단한다.
- 관리자 세션 확인과 로그아웃 endpoint의 최종 계약도 #352와 통합할 때 확인한다.

## 개발 및 검증

- 개발 환경은 `.env.development`의 `VITE_USE_MOCK_API=true`로 같은 문의 계약의 메모리 어댑터를 사용한다.
- 운영 빌드는 `/api/admin/...` 상대 경로와 `credentials: include`를 사용한다.
- `npm run lint`, `npm run test`, `npm run build`를 #351의 필수 검증으로 실행한다.

## 참고 자료

- React, Thinking in React: https://react.dev/learn/thinking-in-react.
- React Router, Picking a Mode: https://reactrouter.com/start/modes.
- Feature-Sliced Design: https://feature-sliced.design/.
- TanStack Query, Queries: https://tanstack.com/query/latest/docs/framework/react/guides/queries.
- 공식 로고 원본: `cchaksa/cchaksa-app`의 `composeApp/src/androidMain/ic_logo-playstore.png`.
