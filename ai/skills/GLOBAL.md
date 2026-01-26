# GLOBAL Skill

## 사용 시점
- Context Phase 5 Global/Config 또는 공통 설정 변경 시 참조한다.
- 보안, 로깅, 예외 처리, 메시지, 설정을 추가하거나 수정할 때 참조한다.

## 실행 규약
- Security, Logging, Exception Handler, Config 클래스만 정의한다.
- Cross-cutting concern은 Filter, Interceptor, AOP 등을 통해 Global 레이어에 한정한다.
- 설정 값은 `application-*.yml` 에 정의하고 `@ConfigurationProperties` 로 주입한다.
- 공통 예외 응답, 로깅 포맷은 `global` 레이어에서만 수정한다.
- 변경 사항은 다른 레이어의 책임을 침범하지 않도록 Context와 대조한다.

## 금지 사항
- 도메인 규칙이나 Application 로직을 Global 설정에 포함하지 않는다.
- Infrastructure 의존성을 Global 레이어에 주입하지 않는다.
- 임의의 전역 트랜잭션 경계를 추가하지 않는다.
