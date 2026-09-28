# 관리자 웹 UI 설계

## 목표

관리자와 CS 담당자가 카카오 계정으로 인증한 뒤 문의를 조회하고, 미답변 문의에 한 번만 답변할 수 있는 업무용 SPA를 구성한다.

## 정보 구조

| 경로 | 화면 | 역할 |
| --- | --- | --- |
| `/login` | 관리자 로그인 | 카카오 관리자 인증 시작 |
| `/inquiries` | 문의 목록 | 상태 필터, 검색, 최신 문의 조회 |
| `/inquiries/:reportId` | 문의 상세 | 문의 확인, 미답변 문의 답변, 완료 답변 조회 |

로그인 뒤 기본 진입점은 `/inquiries`로 한다. 관리자 앱의 사이드바에는 초기 범위에서 `문의`만 노출한다.

## 화면 흐름

1. 인증되지 않은 관리자는 `/login`에서 척척학사 로고와 카카오 로그인 버튼을 본다.
2. 카카오 로그인 버튼은 `/api/admin/auth/signin`으로 전체 페이지 이동한다.
3. 인증 성공 뒤 `/inquiries`에서 전체 문의를 서버가 반환한 최신순으로 본다.
4. 목록 상단에서 `전체`, `답변 필요`, `답변 완료` 상태를 선택하고 사용자 ID, 학번, 오류 코드 검색어를 입력한다.
5. 미답변 문의 상세에서는 문의 본문 아래에 답변 작성 영역을 제공한다.
6. 답변 완료 문의 상세에서는 답변과 답변한 CS 담당자 이름을 읽기 전용으로 표시한다.

관리자 인증은 카카오 OIDC만 사용하므로 별도 아이디·비밀번호 입력 필드는 두지 않는다.

## 화면 원칙

- 첨부 레퍼런스의 어두운 고정 사이드바, 얇은 상단 바, 넓은 목록 영역을 관리자 셸에 적용한다.
- 운영 도구답게 장식보다 검색, 상태 구분, 표 가독성과 반복 작업 효율을 우선한다.
- 상태는 색상만으로 구분하지 않고 텍스트 라벨을 함께 제공한다.
- 문의 본문, 답변, 학번과 학적 정보는 브라우저 로그나 오류 추적 태그에 기록하지 않는다.
- 답변자 이름은 서버가 인증된 `admin_account_id`로 결정한 결과만 표시한다.

## 프론트엔드 구조

현재 화면 수에 맞춰 Feature-Sliced Design의 일부 계층만 사용한다.

```text
src/
  app/       # 전역 라우터와 앱 초기화
  pages/     # 라우트 단위 화면
  features/  # 관리자 로그인, 문의 필터, 문의 답변 등 사용자 행동
  shared/    # 자산, 공통 설정, 범용 UI와 스타일
```

- `entities`는 문의 API DTO와 도메인 모델이 확정되는 목록 단계에서 추가한다.
- `widgets`는 사이드바·상단 바·목록 조합이 반복되는 관리자 셸 단계에서 추가한다.
- 현재 규모에서 루트 전역 store는 두지 않고 상태를 사용하는 가장 가까운 화면 또는 기능에 둔다.
- 각 feature/page는 `index.ts`를 공개 진입점으로 사용하고 외부에서 내부 파일을 깊게 import하지 않는다.

## 라우팅 선택

Vite SPA와 S3 배포 형태를 유지하고 React Router의 선언형 모드를 사용한다. 현재는 클라이언트 라우팅과 활성 메뉴 상태만 필요하므로 Framework Mode나 SSR은 도입하지 않는다.

CloudFront에서는 `/api/admin/*`를 API 동작으로 분리하고, 그 외 프론트 경로만 SPA fallback을 적용해야 한다.

## 단계별 구현

### 1단계: 로그인과 앱 기반

- 공식 척척학사 앱 로고 자산을 재사용한다.
- `/login` 화면과 카카오 로그인 진입점을 구현한다.
- React Router와 위 계층 구조를 적용한다.

### 2단계: 관리자 셸과 문의 목록

- 사이드바, 상단 바와 `/inquiries` 화면을 구현한다.
- 전체/답변 필요/답변 완료 필터와 검색 UI를 구현한다.
- API 계약 전에는 실제 데이터 호출을 연결하지 않는다.

### 3단계: 미답변 문의 상세

- 게시글 형태의 문의 내용과 댓글 형태의 답변 작성 UI를 구현한다.
- 답변 등록 중, 성공, 실패 상태와 중복 제출 방지를 정의한다.

### 4단계: 답변 완료 상세

- 완료 답변과 CS 담당자 이름을 읽기 전용으로 표시한다.
- 추가 답변 입력은 렌더링하지 않는다.

### 5단계: API와 인증 연동

- 확정된 `/api/admin/...` 계약에 맞춰 세션 확인, 목록, 상세, 답변 API를 연결한다.
- 인증 실패, 권한 없음, 세션 만료와 API 오류 상태를 구현한다.

## 프론트엔드 API 계약

이번 UI 구현에서 아래 계약을 기준으로 HTTP 어댑터를 구성했다. 서버 구현 단계에서 필드명이나 상태 코드가 달라지면 어댑터와 이 문서를 함께 갱신한다.

### 관리자 인증

- `GET /api/admin/auth/me`는 현재 세션의 `adminAccountId`, `displayName`, `role`을 반환한다.
- `role`은 `ADMIN` 또는 `CS_AGENT`다.
- 인증되지 않은 세션은 `401`, 관리자 허용 목록에 없거나 비활성인 계정은 `403`을 반환한다.
- `POST /api/admin/auth/signout`은 관리자 세션을 종료하고 본문 없는 `204`를 반환한다.
- 로그인 시작점은 `GET /api/admin/auth/signin`이며 성공 후 관리자 SPA의 `/inquiries`로 복귀한다.

### 문의 목록

- `GET /api/admin/reports`를 사용한다.
- query parameter는 `status`, `searchField`, `query`, `page`, `size`다.
- `status`는 `PENDING` 또는 `ANSWERED`, `searchField`는 `USER_ID`, `STUDENT_CODE`, `ERROR_CODE`다. 통합 검색은 `searchField`를 생략한다.
- `page`는 0부터 시작하며 서버가 `createdAt` 내림차순으로 정렬한다.
- 응답은 `items`, `page`, `size`, `totalElements`, `totalPages`를 포함한다.

### 문의 상세와 답변

- `GET /api/admin/reports/{reportId}`는 문의 요약 필드, 본문, 학적 스냅샷과 nullable `answer`를 반환한다.
- 완료 답변의 `answer`는 `content`, `answeredAt`, `answeredBy.adminAccountId`, `answeredBy.displayName`을 포함한다.
- `POST /api/admin/reports/{reportId}/answer`에 `{ "answer": "..." }`를 전송한다.
- 서버는 요청 본문의 담당자 정보를 받지 않고 인증된 `admin_account_id`를 답변 감사 정보로 기록한다.
- 이미 답변된 문의의 중복 답변은 `409 Conflict`로 거부하고, 성공 시 갱신된 문의 상세를 반환한다.
- 문의 본문, 답변, 학번과 학적 스냅샷은 클라이언트 console 및 오류 추적 tag에 기록하지 않는다.

개발 환경에서는 `.env.development`의 `VITE_USE_MOCK_API=true`로 같은 계약의 메모리 어댑터를 사용한다. 운영 빌드는 mock을 포함한 실행 분기를 선택하지 않고 `/api/admin/...` 상대 경로를 호출하며 `credentials: include`로 세션 쿠키를 전달한다.

## 참고 자료

- React, Thinking in React: https://react.dev/learn/thinking-in-react.
- React Router, Picking a Mode: https://reactrouter.com/start/modes.
- React Router, Declarative Installation: https://reactrouter.com/start/declarative/installation.
- Feature-Sliced Design: https://feature-sliced.design/.
- TanStack Query, Queries: https://tanstack.com/query/latest/docs/framework/react/guides/queries.
- 공식 로고 원본: `cchaksa/cchaksa-app`의 `composeApp/src/androidMain/ic_logo-playstore.png`.
