# Workflow

## Phase 1: Context 생성
- 목적: 기능 요구를 실행 가능한 Context로 고정한다.
- 입력: 사용자 요청, 기존 Context 기록, `AGENTS.md` 참조 정보.
- 출력: `docs/context/<feature>.md` 또는 동등한 Context 산출물, 업데이트된 체크리스트.
- 완료 조건:
  - [ ] Context 템플릿 전 항목 작성 완료
  - [ ] 이해관계자 확인 메모 기록
  - [ ] 변경 사항을 커밋할 준비 완료

## Phase 2: Domain
- 목적: 순수 domain 규칙을 구현하여 핵심 모델과 서비스 규약을 확정한다.
- 입력: 승인된 Context, Phase 1 산출물.
- 출력: `src/main/java/.../domain` 클래스, `src/test/java/.../domain` 테스트, domain 예외 정의.
- 완료 조건:
  - [ ] TDD로 domain 테스트 선 작성
  - [ ] `./gradlew test` 를 domain 범위에서 통과
  - [ ] 변경 파일 정리 및 커밋 준비

## Phase 3: Application
- 목적: use-case orchestration과 트랜잭션 경계를 구현한다.
- 입력: Context, Domain 산출물, 인터페이스 계약.
- 출력: `src/main/java/.../service` 서비스, `src/test/java/.../service` 테스트, DTO 계약.
- 완료 조건:
  - [ ] Repository 인터페이스로만 의존
  - [ ] Application 단위 테스트 통과 (`./gradlew test`)
  - [ ] 빌드 산출물 검토 후 커밋 준비

## Phase 4: Infrastructure
- 목적: portal, cache, client, persistence 구현과 Domain 객체 변환을 수행한다.
- 입력: Context, Domain 모델, Repository 계약.
- 출력: `src/main/java/.../infrastructure` 어댑터, 매퍼, 외부 클라이언트, 대응 테스트.
- 완료 조건:
  - [ ] 외부 의존성을 mock 또는 fake로 분리한 테스트 통과
  - [ ] `./gradlew test` 전체 실행 통과
  - [ ] 구현 상세를 Context에 반영하고 커밋 준비

## Phase 5: Global / Config
- 목적: 보안, 로깅, 예외 처리, 설정을 추가하거나 수정한다.
- 입력: Context, 기존 Global 구성, 관련 Phase 산출물.
- 출력: `src/main/java/.../global` 설정, `src/main/resources` 구성 파일, 관련 테스트.
- 완료 조건:
  - [ ] 새로운 설정이 다른 Layer 규칙을 침범하지 않음
  - [ ] 관련 테스트 및 `./gradlew build` 통과
  - [ ] 설정 변경 사항 커밋 준비

## Phase 6: API / Controller
- 목적: Global 규칙을 준수하며 Controller와 API 계약을 완성한다.
- 입력: Context, Application 서비스, Global 설정.
- 출력: `src/main/java/.../domain/controller` 하위 Controller, `src/main/resources/public/openapi.yaml` 업데이트, API 테스트.
- 완료 조건:
  - [ ] Controller 테스트 및 문서화 완료
  - [ ] `./gradlew test` 와 필요 시 `./gradlew build` 통과
  - [ ] 엔드포인트 및 문서를 반영한 커밋 준비
