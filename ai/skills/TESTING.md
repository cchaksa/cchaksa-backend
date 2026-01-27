# TESTING Skill

## 사용 시점
- 모든 Phase에서 테스트를 작성하거나 실행할 때 참조한다.
- 새 기능, 버그 수정, 리팩터링 전후 검증 시 참조한다.

## 실행 규약
- Domain 테스트: 순수 단위 테스트로 작성하고 외부 프레임워크 사용 금지.
- Application 테스트: Repository, Client를 mock 하여 Use-case 흐름을 검증한다.
- Infrastructure 테스트: 어댑터 단위 또는 슬라이스 테스트로 외부 의존성을 대체한다.
- Global 테스트: 보안/로깅/예외 설정을 Spring Test로 검증하되 API 호출을 최소화한다.
- 테스트 클래스 이름은 `*Tests` 로 끝나야 하며 Given-When-Then 패턴을 따른다.

## 테스트 격리 원칙
- 실제 포털, Redis, 외부 API에 직접 호출을 보내지 않는다.
- 여러 Layer를 한 번에 검증하는 E2E 테스트를 작성하지 않는다.
- 테스트에서 임의 Thread.sleep, 무한 대기, 비결정성 코드를 사용하지 않는다.

## 완료 체크리스트
- [ ] 신규 테스트 추가 및 실패 확인
- [ ] 구현 완료 후 모든 관련 테스트 통과
- [ ] `./gradlew test` 와 필요 시 `./gradlew build` 통과 로그 확인
- [ ] 테스트 결과를 Context와 PR 설명에 기록
