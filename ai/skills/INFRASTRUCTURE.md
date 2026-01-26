# INFRASTRUCTURE Skill

## 사용 시점
- Context Phase 4 Infrastructure 수행 시 참조한다.
- Portal, Cache, Client, Persistence 어댑터를 구현하거나 수정할 때 참조한다.

## 실행 규약
- Repository 인터페이스와 Domain 객체 간 변환을 책임진다.
- 외부 시스템과의 통신은 어댑터, 클라이언트, 매퍼로 격리한다.
- Cache, Portal, Database 연결 정보는 설정으로 주입하고 코드에 하드코딩하지 않는다.
- 예외는 Domain 및 Application 규약에 맞춰 매핑한다.
- 테스트는 Fake/Mock 외부 의존성을 사용하여 TDD로 작성한다.

## 금지 사항
- Domain 모델을 Infrastructure 모델과 병합하지 않는다.
- Application 또는 Domain 로직을 어댑터 내부에 추가하지 않는다.
- 외부 라이브러리를 직접 Domain에 노출하지 않는다.
