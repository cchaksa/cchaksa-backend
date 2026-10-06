# 포털 연동 로그인 선검증 Clarify

## 결정 사항

- 클라이언트는 `POST /portal/login`으로 포털 ID/PW를 먼저 검증한다.
- 로그인 검증 실패 시 광고를 보여주지 않는다.
- 로그인 검증 성공 시 backend는 `portal_verification_token`을 반환한다.
- 클라이언트는 `POST /portal/link`를 호출해 job 생성을 요청하고, 202 응답을 받으면 바로 광고를 보여준다.
- 광고 시간과 스크래핑 job 실행 시간을 겹친다.
- `POST /portal/link`는 `portal_verification_token`을 검증한 뒤 job 생성과 outbox 발행을 진행한다.
- token 검증 실패 시 스크래핑 job을 만들지 않는다.
- `/portal/link` 호출 시 포털 ID/PW는 다시 전송하지만, 사용자가 다시 입력하지는 않는다.

## 남은 확인 사항

- 스크래퍼 쪽에 `POST /login` 경량 엔드포인트가 필요하다.
- 스크래퍼 `POST /login`은 포털 로그인과 `sso_security_check` 세션 핸드오프 성공을 검증 기준으로 삼는다.
- 스크래퍼 `POST /login`은 ID/PW 오류 401, 계정 잠금 423, 시스템 오류 5xx를 반환해야 한다.
