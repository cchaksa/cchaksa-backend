# Context Notes

## 2026-05-25

- 로그상 스크래퍼 성공 콜백과 S3 payload 수신은 정상이며, 후처리에서 `operationType=LINK` 상태로 포털 초기화를 재시도하다가 `already_connected`로 실패했다.
- 사용자는 병합 후 이미 연동된 LINK를 REFRESH처럼 재동기화하는 정책을 명시했다.
- 최초 LINK 사용자는 `portalConnected=false` 상태를 유지하므로 기존 초기화 경로에 남아야 한다.
- 이번 범위는 백엔드 application 계층 분기와 회귀 테스트로 제한한다.
- 대상 테스트 RED는 신규 초기화 호출 assertion으로 실패했고, 수정 후 같은 테스트가 통과했다.
- Gemini 리뷰를 반영해 REFRESH helper에 `User`를 직접 전달하고, 테스트의 중복 `lenient().doAnswer` 스텁을 제거했다.
