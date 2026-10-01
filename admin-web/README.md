# 척척학사 관리자 웹

`admin-web`은 백엔드 Gradle 빌드와 독립된 React/Vite SPA다. API는 같은 출처의 `/api/admin/*` 상대 경로만 사용한다.

## 로컬 실행

```bash
npm ci --no-audit --no-fund
npm run dev
```

`npm run dev`는 실제 API 모드이며 `/api/admin/*`를 기본 `http://localhost:8080`으로 proxy한다. 다른 로컬 백엔드를 사용할 때는 커밋하지 않는 `.env.development.local`에 다음 값을 둔다.

```text
ADMIN_API_PROXY_TARGET=http://localhost:8080
```

서버 없이 화면만 확인할 때는 `npm run dev:mock`을 사용한다.

## 검증과 빌드

```bash
npm run lint
npm run test
npm run typecheck
npm run build
```

배포 산출물은 `admin-web/dist`다. 빌드에는 관리자 자격 증명이나 API origin을 주입하지 않는다.

## 환경별 관리자 로그인

관리자 로그인은 개발진이 발급한 `loginId`와 비밀번호를 사용한다. 프론트는 환경별 인증 설정을 갖지 않으며 같은 `dist`를 dev와 prod에 배포한다.

- 로그인 화면은 `GET /api/admin/auth/csrf`로 `XSRF-TOKEN`을 준비한 뒤 `POST /api/admin/auth/signin`을 호출한다.
- 로그인 성공 뒤 HttpOnly `cchaksa_admin_session` 쿠키로 `/api/admin/*`를 호출한다.
- 비밀번호 변경은 `POST /api/admin/auth/password`에 기존·신규 비밀번호만 전송한다.
- 로그아웃은 `POST /api/admin/auth/signout`을 호출하며 재로그인할 때 CSRF를 다시 준비한다.
- loginId와 비밀번호는 입력 원문 그대로 전송하고 query cache, URL, 브라우저 저장소, console과 analytics에 기록하지 않는다.
- 관리자 계정 발급·활성화, 비밀번호 hash와 세션 저장은 백엔드가 관리한다.

dev 통합 검증 전 로컬 자격 증명이 설정된 ACTIVE `admin_accounts` 테스트 계정이 필요하다. 실제 loginId와 비밀번호는 Git, 문서, mock과 테스트 fixture에 기록하지 않는다.
