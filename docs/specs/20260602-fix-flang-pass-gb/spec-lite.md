# Spec Lite

## Summary
- Purpose: 개발 스크래퍼가 보내는 `flangPassGb`의 `통과`/`미통과` 값을 백엔드가 수용하고, 알 수 없는 값은 콜백 job을 실패로 확정하게 한다.
- Scope (In/Out):
  - In: 포털 raw 데이터 매핑, 성공 콜백 후처리 중 매퍼 검증 실패 처리, 관련 단위 테스트
  - Out: 공개 API 응답 스키마, scraper 변경, stale reconciler 정책 변경
- Expected Impact: 개발 서버 포털 연동에서 `flangPassGb: "미통과"` payload가 후처리 예외로 멈추지 않고 정상 동기화된다.

## Key Rules
- Rule 1: `통과`는 `true`, `미통과`는 `false`, `null`/blank는 기존처럼 `null`로 매핑한다.
- Rule 2: `Y`/`N`은 더 이상 유효 값으로 인정하지 않고 알 수 없는 값으로 처리한다.
- Rule 3: 알 수 없는 `flangPassGb` 값과 매핑 중 `RuntimeException`은 조용히 기본값으로 치환하지 않고 `SCRAPE_RESULT_SCHEMA_INVALID` 경로로 실패 확정한다.
- Rule 4: `POST_PROCESSING` job timeout 처리는 이번 변경 범위에 포함하지 않는다.

## Risks / Assumptions
- Risk: scraper가 향후 다른 한글 값을 보내면 schema invalid로 job이 FAILED 된다.
- Assumption: 현재 개발 스크래퍼 계약은 `통과`/`미통과`이며 운영 payload 미전송 케이스는 기존 `null` 처리로 유지된다.
- Approval: 2026-06-02 사용자 요청 "PLEASE IMPLEMENT THIS PLAN"으로 Phase 2 구현을 승인받았다.

## Tasks
- [x] Tests planned
- [x] RED verified
- [x] Implementation complete
- [x] Targeted tests passed
- [x] Full tests passed

> Lite 스펙은 Scope < 1 day & API 변경 없음일 때만 허용. 조건을 벗어나면 즉시 Standard 스펙으로 승격한다.
