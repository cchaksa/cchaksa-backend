# Tasks Template

## Checklist
- [ ] Domain tests: job 상태 전이/멱등 로직 추가
- [ ] Application layer: 콜백 서비스 DTO/상태 저장/후처리 트리거 구현
- [ ] Infrastructure layer: S3 client + 어댑터 + 설정 추가
- [ ] Global/config: S3 자격증명, 로깅 key
- [ ] API/controller: `/internal/scrape-results` 요청/응답 갱신 및 검증
- [ ] Documentation: API 명세, 운영 가이드 업데이트

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | Success | 2026-04-10 |
| 2 | `./gradlew test` | Success | 2026-04-10 |
| 3 | `./gradlew test --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackServiceUnitTests` | Success | 2026-04-10 |
| 4 | `./gradlew test` | Success | 2026-04-10 |

## Notes
- Observation: Phase 1 완료, clarify 답변(동기 처리/상태 코드) 반영됨. Phase 2에서 S3 검증~DB 반영을 단일 요청 안에서 구현해야 함.
