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

배포 산출물은 `admin-web/dist`다. 빌드에는 Kakao key나 API origin을 주입하지 않는다.

## 환경별 관리자 로그인

프론트는 `GET /api/admin/auth/challenge`가 반환한 `javascriptAppKey`와 `redirectUri`로 Kakao JavaScript SDK를 초기화한다. 따라서 동일한 `dist`를 dev와 prod에서 사용할 수 있고, 환경별 Kakao 설정은 백엔드 실행 환경에서 관리한다.

백엔드에 필요한 값은 다음과 같다.

```text
ADMIN_KAKAO_JAVASCRIPT_APP_KEY
ADMIN_KAKAO_REST_API_KEY
ADMIN_KAKAO_CLIENT_SECRET
ADMIN_KAKAO_REDIRECT_URI
ADMIN_AUTH_COOKIE_SECURE=true
```

- 실제 값과 Client Secret은 Git, Vite 환경변수, 브라우저 저장소에 두지 않는다.
- `ADMIN_KAKAO_REDIRECT_URI`는 해당 환경의 관리자 웹 origin과 `/login/callback`을 조합한 전체 URI다.
- Kakao Developers의 JavaScript SDK 도메인과 JavaScript/REST API key의 redirect URI도 같은 환경 주소로 등록한다.
- dev 검증 전 ACTIVE `admin_accounts`에 테스트 담당자의 Kakao app-scoped social ID를 등록한다.
- 로그인 nonce, authorization code, ID token과 관리자 cookie는 console 또는 오류 추적 tag에 기록하지 않는다.
