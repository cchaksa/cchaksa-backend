# Plan Template

## Architecture / Layering
- Domain impact:
  - Job 엔티티에 `callbackAttempt`, `callbackReceivedAt`, `resultS3Key`를 보관하고 `job_id + attempt` 멱등 판단 메서드 추가
  - `FAILED_S3_READ`, `FAILED_RESULT_SCHEMA`, `FAILED_POST_PROCESSING` 상태 코드 기록 규칙 정비
- Application orchestration:
  - `/internal/scrape-results` 서비스에서 시그니처 검증 → 멱등 체크 → `result_s3_key` 검증 → S3 HEAD/GET → JSON/schema 검증 → PortalCallbackPostProcessor 호출까지 **동기 처리**
  - duplicate 요청은 `job_id + attempt` 기준으로 즉시 200/202 응답
- Infrastructure touchpoints:
  - AWS SDK S3 client에 HEAD/GET + retry 래퍼(`ScrapeResultResultStoreClient`) 구현, prefix allowlist 검증 포함
  - 비동기 executor/scheduler 제거 (thread/Async 금지)
- Global/config changes:
  - `scraping.result-store`로 키 경로를 평탄화하고 dev 기본값(`SCRAPING_RESULT_BUCKET`, `SCRAPING_RESULT_PREFIX`, `SCRAPING_RESULT_REGION`, `SCRAPING_RESULT_MAX_PAYLOAD_BYTES`, `SCRAPING_RESULT_API_CALL_TIMEOUT_SECONDS`, `SCRAPING_RESULT_API_CALL_ATTEMPT_TIMEOUT_SECONDS`)을 명시한다.
  - post-process executor 설정은 제거/비활성화하고 callback 요청 내에서만 처리한다.
  - 새로운 오류 코드/상태에 대한 로깅 패턴 점검

## Data / Transactions
- Repositories touched:
  - ScrapeJobRepository (JPA) → `resultS3Key`, 상태 업데이트 쿼리
  - 콜백 이벤트 히스토리 테이블 추가 여부 평가
- Transaction scope:
  - Controller → Application 서비스 → PortalCallbackPostProcessor 전체를 하나의 요청 흐름에서 실행
  - PortalCallbackPostProcessor는 REQUIRES_NEW 유지하지만 호출은 동기적이며 응답 전에 완료되어야 함
- Consistency expectations:
  - eventual consistency 없음, 콜백 응답 전에 결과 반영이 끝나야 함

## Testing Strategy
- Domain tests:
  - 상태 전이/멱등 로직에 대한 단위 테스트 (`job_id`, terminal 상태 보호)
- Application tests:
  - 콜백 서비스가 DTO 검증 실패 시 예외 반환, 정상 시 repository 호출 + 멱등 케이스
- Integration/API tests:
  - `/internal/scrape-results` 컨트롤러 단위/통합 테스트 (MockMvc)
  - 콜백 서비스 단위 테스트에서 S3 읽기/검증/에러 코드 시나리오 커버
- Additional commands:
  - `./gradlew test`
  - 필요 시 S3 어댑터용 `./gradlew :module:test` (실제 모듈명 확인 후 기록)

## Rollout Considerations
- Backward compatibility:
  - 구 방식 요청 지원 필요 시 기간 동안 request에 payload/s3 key 병행 허용 여부 확정
- Observability / metrics:
  - 후처리 실패/재시도 횟수, S3 read latency, duplicate callback 카운터를 메트릭으로 노출
- Feature flags / toggles:
  - 비동기 플래그 제거, synchronous 모드가 기본. 롤백은 워커/IaC 배포 롤백으로만 처리.
