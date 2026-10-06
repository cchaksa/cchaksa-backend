# 포털 로그인 검증 분리 Spec

## 배경

현재 포털 연동 시작 API는 `POST /portal/link` 하나로 포털 ID/PW 입력, 스크래핑 job 생성, outbox 발행까지 처리한다.
이 구조에서는 포털 ID/PW 검증 실패도 스크래핑 실행 경로에 종속된다.

ECS, EC2 기반 상시 실행 구조에서는 스크래핑 서버가 이미 떠 있어 실패 피드백이 비교적 빠르게 돌아왔다.
하지만 요청 기반 Lambda 구조에서는 cold start와 스크래핑 초기화 비용 때문에 포털 인증 실패를 확인하는 데 30~60초 가까이 걸릴 수 있다.
또한 포털 연동 과정에 광고가 추가되면, 로그인 실패 사용자에게도 광고가 먼저 노출될 수 있다.

## 문제

- 사용자가 포털 ID/PW를 잘못 입력해도 스크래핑 job 경로를 기다려야 한다.
- 인증 실패가 job 생성 이후 polling 결과로 드러나면 첫 연동 UX가 불필요하게 느려진다.
- 포털 로그인 실패 사용자에게 광고를 보여주면 “실패할 요청인데 광고부터 봤다”는 경험이 된다.
- 광고 시간과 스크래핑 시간을 겹치려면 로그인 검증과 job 생성을 분리하되, job 생성은 광고 시작 전에 끝나야 한다.

## 목표

- 클라이언트는 먼저 `POST /portal/login`으로 포털 ID/PW를 검증한다.
- 포털 ID/PW 검증 실패 시 광고를 보여주지 않고 즉시 실패 응답을 반환한다.
- 검증 성공 시 `portal_verification_token`을 반환한다.
- 클라이언트는 `POST /portal/link`를 호출해 스크래핑 job을 생성하고, 202 응답 직후 광고를 보여준다.
- `POST /portal/link`는 `portal_verification_token`을 검증한 경우에만 job 생성과 outbox 발행을 수행한다.

## 범위

### 포함

- `POST /portal/login` 추가.
- 로그인 검증 성공 시 짧은 TTL의 `portal_verification_token` 발급.
- `POST /portal/link` 요청에 `portal_verification_token` 필드 추가.
- token 검증 실패 시 `ScrapeJob`과 `ScrapeJobOutbox`가 생성되지 않도록 보장.
- 스크래퍼의 경량 로그인 검증 엔드포인트 호출 경계 추가.
- service, token, controller, 스크래퍼 client 테스트 추가.

### 제외

- Lambda cold start 자체를 제거하는 인프라 변경.
- 스크래퍼 워커의 내부 로그인 구현 변경.
- 기존 스크래핑 job callback 처리 변경.
- 기존 polling endpoint 변경.
- 광고 SDK 연동과 광고 완료 증빙 검증.

## API 계약

### `POST /portal/login`

인증된 사용자가 포털 ID/PW를 입력하면 backend가 스크래퍼의 경량 로그인 검증 경로를 호출한다.
검증에 성공하면 `portal_verification_token`을 반환한다.
이 API는 스크래핑 job을 생성하지 않는다.

```json
{
  "portal_type": "suwon",
  "username": "17019013",
  "password": "pw"
}
```

```json
{
  "success": true,
  "data": {
    "portal_verification_token": "token"
  },
  "message": "요청 성공"
}
```

### `POST /portal/link`

인증된 사용자가 포털 ID/PW와 `portal_verification_token`을 함께 보내면 backend가 token을 검증한다.
검증에 성공한 경우에만 스크래핑 job을 생성하고 기존과 동일하게 202 응답을 반환한다.
클라이언트는 202 응답을 받은 직후 광고를 보여주고, 광고가 끝나면 기존 polling 흐름으로 job 상태를 확인한다.

```json
{
  "portal_type": "suwon",
  "username": "17019013",
  "password": "pw",
  "portal_verification_token": "token"
}
```

실패 응답은 기존 에러 코드 정책을 따른다.

- 잘못된 입력은 `INVALID_ARGUMENT`.
- 지원하지 않는 포털 타입은 `UNSUPPORTED_PORTAL_TYPE`.
- 포털 ID/PW 불일치는 `POST /portal/login`에서 `PORTAL_LOGIN_FAILED`.
- 계정 잠금은 `POST /portal/login`에서 `PORTAL_ACCOUNT_LOCKED`.
- token 누락, 만료, 불일치는 `INVALID_ARGUMENT`.

## 구현 방향

현재 backend에는 포털 인증만 수행하는 외부 클라이언트가 없다.
따라서 `PortalLoginVerifier` 인터페이스를 application 계층에 두고, 실제 구현은 infrastructure 계층에서 스크래퍼의 경량 로그인 검증 엔드포인트에 연결한다.

이번 변경의 backend 책임은 다음 경계로 제한한다.

- `PortalLoginService`가 `PortalLoginVerifier`를 호출하고 `PortalLoginVerificationTokenService`로 token을 발급한다.
- token은 서버 저장 없이 `jjwt` 기반 HMAC 서명 JWT로 발급한다.
- token payload에는 사용자와 포털 타입, username hash, 만료 시각만 포함한다.
- 비밀번호는 token payload에 넣지 않고, token 서명 입력에만 사용해 `/portal/link`에서 다시 제출된 비밀번호가 로그인 검증 당시 값과 같은지 확인한다.
- `PortalLinkJobService`는 job 생성 전에 token을 검증한다.
- token 검증 실패 시 job을 생성하지 않는 흐름을 보장한다.
- 실제 포털 검증 구현체는 스크래퍼 검증 경로에 맞춰 연결할 수 있도록 인터페이스로 분리한다.

## 테스트 기준

- `POST /portal/login`은 포털 로그인 검증 후 `portal_verification_token`을 반환한다.
- 로그인 검증 실패 예외가 발생하면 token을 발급하지 않는다.
- 발급한 token은 같은 사용자와 같은 자격 증명에서만 검증된다.
- 만료되거나 다른 비밀번호로 제출된 token은 거부된다.
- `POST /portal/link`는 token 검증을 job 생성보다 먼저 실행한다.
- token 검증 실패 시 `ScrapeJob`이나 `ScrapeJobOutbox`를 생성하지 않는다.
- `PortalClient`는 스크래퍼의 `/login` 엔드포인트를 호출하고 401을 `PORTAL_LOGIN_FAILED`로 매핑한다.
