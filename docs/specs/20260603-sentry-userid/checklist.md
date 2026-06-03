# feat/253 Sentry userId 추적 보강 작업 체크리스트

- [x] feat/253 브랜치 생성.
- [x] 변경 전 `./gradlew test` 기준 통과 확인.
- [x] Sentry MDC tag 목록에 포털 job 추적 키 추가.
- [x] userId 기반 Sentry/MDC scope 헬퍼 추가.
- [x] 전역 예외 처리의 Sentry 보고 제외 정책 세분화.
- [x] callback 후처리 구간에 userId/jobId context 적용.
- [x] outbox dispatch 실패 구간에 userId/jobId context 적용.
- [x] stale reconcile 로그에 userId/jobId context 적용.
- [x] 포털 내부 예상 실패 로그 레벨 정리.
- [x] 대상 테스트 통과 확인.
- [x] 전체 `./gradlew test` 통과 확인.
- [x] 커밋 생성.
