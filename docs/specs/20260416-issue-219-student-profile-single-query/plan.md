# Issue 219 Plan

## Architecture / Layering
- Domain impact:
  - `StudentService`에 프로필 전용 단일 조회 메서드를 추가하고 기존 `getStudentProfile(UUID)`는 유지한다.
- Application orchestration:
  - 별도 application layer 추가 없음. 기존 controller -> domain service 경로 안에서 처리한다.
- Infrastructure touchpoints:
  - `UserRepository`에 프로필 전용 fetch query를 추가한다.
- Global/config changes:
  - 없음. 다만 Swagger 문서와 예시 응답은 실제 예외 의미에 맞게 조정한다.

## Data / Transactions
- Repositories touched:
  - `UserRepository`
  - 기존 `StudentRepository`는 다른 호출자를 위해 유지
- Transaction scope:
  - 읽기 전용 트랜잭션에서 `User` 루트 조회 후 DTO 조립까지 수행
- Consistency expectations:
  - 요청 시점마다 `userId` 기준 최신 `User-Student` 연결 상태를 읽는다.

## Testing Strategy
- Domain tests:
  - `StudentServiceUnitTests`에서 `getStudentProfileByUserId` 성공/예외/null-safe/secondaryMajor 매핑 검증
- Application tests:
  - 없음
- Integration/API tests:
  - `StudentControllerApiIntegrationTest`에서 새 서비스 메서드 호출과 에러 응답 검증
- Additional commands:
  - `./gradlew test --tests 'com.chukchuk.haksa.domain.student.controller.StudentControllerApiIntegrationTest' --tests 'com.chukchuk.haksa.domain.student.service.StudentServiceUnitTests'`
  - `./gradlew test`

## Rollout Considerations
- Backward compatibility:
  - 외부 API shape와 다른 학생 컨텍스트 API는 유지한다.
- Observability / metrics:
  - 기존 `student.profile.get.done` 로깅 유지
- Feature flags / toggles:
  - 없음
