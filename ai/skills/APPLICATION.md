# APPLICATION Skill

## 사용 시점
- Context Phase 3 Application 실행 시 참조한다.
- Use-case orchestration 또는 트랜잭션 경계를 정의할 때 참조한다.

## 실행 규약
- Application 서비스는 domain 서비스와 Repository 인터페이스를 조합해 Use-case를 완성한다.
- 트랜잭션은 Application 서비스에서만 시작하거나 전파한다.
- 입력 DTO, 출력 DTO, Command/Query 객체를 명확히 구분한다.
- 외부 통신은 Infrastructure 인터페이스를 통해서만 요청한다.

## TDD 체크리스트
- [ ] Application 서비스 단위 테스트를 먼저 작성한다.
- [ ] Domain, Repository 인터페이스를 mock 하여 Use-case 흐름을 검증한다.
- [ ] 실패 케이스(예외, 검증 실패)를 우선 정의한다.
- [ ] `./gradlew test --tests "*Application*"` 와 전체 테스트를 실행한다.

## 금지 사항
- 구현체(Repository, Client)를 직접 생성하거나 의존하지 않는다.
- Controller나 Infrastructure 로직을 Application 레이어로 끌어오지 않는다.
- 트랜잭션을 Domain 혹은 Infrastructure에서 시작하도록 허용하지 않는다.
