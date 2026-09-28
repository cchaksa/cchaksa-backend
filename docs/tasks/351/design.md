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

## 미확정 계약

- 로그인 성공·실패 callback 경로와 세션 확인 API.
- 목록 pagination 방식과 검색 query parameter 이름.
- 문의 목록·상세·답변 DTO.
- 답변 가능한 상태 전이와 동시 답변 충돌 응답.

## 참고 자료

- React, Thinking in React: https://react.dev/learn/thinking-in-react.
- React Router, Picking a Mode: https://reactrouter.com/start/modes.
- React Router, Declarative Installation: https://reactrouter.com/start/declarative/installation.
- Feature-Sliced Design: https://feature-sliced.design/.
- 공식 로고 원본: `cchaksa/cchaksa-app`의 `composeApp/src/androidMain/ic_logo-playstore.png`.
