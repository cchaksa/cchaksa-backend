# 20260428 Issue 211 CI Reset Academic Data Fix Plan

## Architecture / Layering
- Domain impact: `UserPortalConnectionRepository`의 기존 학생 재사용 경로에서 삭제된 엔티티 메서드 호출 제거 및 `Student` 컬렉션 상태 초기화.
- Application orchestration: 변경 없음.
- Infrastructure touchpoints: 변경 없음.
- Global/config changes: 변경 없음.

## Data / Transactions
- Repositories touched: `StudentService.resetBy` 내부 기존 repository delete 쿼리만 사용.
- Transaction scope: `initializePortalConnection`의 기존 트랜잭션 안에서 reset과 정보 갱신을 함께 수행.
- Consistency expectations: 기존 학사 데이터 삭제 후 현재 `Student` 객체의 학사 연관관계 컬렉션도 비워 같은 트랜잭션에서 객체 그래프와 DB 상태를 맞춘다.

## Testing Strategy
- Domain tests: `UserPortalConnectionRepositoryTests`에서 `resetBy(studentId)`와 컬렉션 초기화 호출을 검증하고, `StudentTests`에서 컬렉션 초기화 동작을 검증한다.
- Application tests: 해당 없음.
- Integration/API tests: 해당 없음.
- Additional commands: `./gradlew check --stacktrace --no-daemon`.

## Rollout Considerations
- Backward compatibility: API/DB 스키마 변경 없음.
- Observability / metrics: 변경 없음.
- Feature flags / toggles: 해당 없음.
