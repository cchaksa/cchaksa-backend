# Issue 230 Plan

## Architecture / Layering
- Domain impact: `StudentGraduationProgress`가 외국어 인증 생성/갱신 규칙을 가진다.
- Application orchestration: `PortalSyncService`가 포털 동기화 성공 후 외국어 인증 저장을 호출한다.
- Infrastructure touchpoints: raw portal DTO와 mapper가 `flangPassGb`를 내부 boolean으로 변환한다.
- Global/config changes: 없음.

## Data / Transactions
- Repositories touched: `StudentGraduationProgressRepository` 추가.
- Transaction scope: 기존 포털 동기화 트랜잭션에 참여한다.
- Consistency expectations: 저장 후 학생 학업 캐시를 삭제해 다음 조회가 최신 값을 읽는다.

## Testing Strategy
- Domain tests: 외국어 인증 row 생성/갱신.
- Application tests: 초기 연동/새로고침 저장과 캐시 무효화 호출.
- Integration/API tests: 졸업 요건 응답 필드 확인.
- Additional commands: `./gradlew test`

## Rollout Considerations
- Backward compatibility: 기존 사용자 null 상태를 새로고침 필요로 노출한다.
- Observability / metrics: 기존 포털 후처리 로그를 사용한다.
- Feature flags / toggles: 없음.
