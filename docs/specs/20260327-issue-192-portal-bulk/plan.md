# Plan

## Architecture / Layering
- Domain impact: 없음. Domain 모델/규칙은 그대로 유지.
- Application orchestration: `SyncAcademicRecordService` 내부 diff 로직을 정리하여 dirty checking / batch delete 사용. 필요 시 보조 메서드 가시성 조정.
- Infrastructure touchpoints: `StudentCourseRepository` (batch delete 메서드 활용), Hibernate 설정.
- Global/config changes: `application.yml`에 JDBC batch, ordered insert/update 설정 추가.

## Data / Transactions
- Repositories touched: `StudentCourseRepository`, `StudentCourseRepositoryCustom`(필요시), `AcademicRecordRepository` (간접 영향 없음), Spring JPA EntityManager.
- Transaction scope: 기존과 동일하게 학생 단위 @Transactional 유지. Batch delete/flush도 동일 트랜잭션 내에서 수행.
- Consistency expectations: diff 계산 후 insert/update/delete 모두 동일 트랜잭션에서 커밋되어야 하며 실패 시 전체 롤백.

## Testing Strategy
- Domain tests: N/A
- Application tests: `SyncAcademicRecordService` 단위 테스트 추가 – 삭제 diff 시 `deleteAllByIdInBatch` 호출, dirty checking으로 업데이트 되는지 검증(mock 기반).
- Integration/API tests: 필요 시 `@DataJpaTest`로 batch 설정이 정상 기동되는지 검증(시간상 생략 가능, 수동 확인).
- Additional commands: `./gradlew test`

## Rollout Considerations
- Backward compatibility: 데이터 정합성 불변. batch 설정 미지원 DB에서 fallback하여 단건 처리되므로 안정성 확보.
- Observability / metrics: 성능 개선 후 scrape job 작업 시간 log/metrics를 모니터링.
- Feature flags / toggles: 필요 없음.
