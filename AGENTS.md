# 저장소 가이드라인

## 프로젝트 구조 및 모듈 구성
- `src/main/java` 디렉터리는 Spring Boot 앱(`com.chukchuk.haksa`)을 담아야 하며 `domain`, `application`, `infrastructure`, `global` 레이어 구조를 반드시 유지해야 한다.
- `src/main/resources` 는 `application-*.yml` 구성 파일, `logback-spring.xml` 로깅 설정, `public/openapi.yaml` API 문서를 포함해야 한다.
- `src/test/java` 에 모든 JUnit 테스트(`*Tests`)를 두어야 하며, 로컬 관측 구성을 위한 자료는 `docker/` 에만 배치해야 한다.

## 아키텍처 개요
- `domain` 은 엔티티와 비즈니스 규칙을, `application` 은 유스케이스 오케스트레이션을, `infrastructure` 는 포털 클라이언트·캐시·매퍼를, `global` 은 보안·로깅·에러·공용 설정을 각각 담당해야 한다.
- 모든 의존성은 반드시 안쪽을 향해야 하며, `domain/portal/PortalCredentialStore` 와 같은 `domain` 인터페이스를 통해 바깥 레이어에 접근해야 한다.
- 포털 동기화 흐름은 `credentials → scrape → initialize/refresh → persistence` 순서를 항상 따라야 한다.

## 빌드·테스트·개발 명령
- 빌드와 테스트는 `./gradlew build`(컴파일+테스트), `./gradlew test`, `./gradlew bootRun`, `./gradlew clean` 명령으로만 수행해야 한다.

## 코딩 스타일 및 네이밍
- Java 17과 Spring Boot 3.2.x를 사용해야 하며, 들여쓰기는 4칸 공백과 표준 Java 포맷을 따라야 한다.
- 패키지는 `com.chukchuk.haksa.<layer>.<feature>` 형태를 지켜야 하며, 클래스는 UpperCamelCase, 필드는 lowerCamelCase 로 명명해야 한다.
- Lombok을 사용하되 필요한 어노테이션은 명시적으로 선언해야 한다.

## 테스트 지침
- 모든 테스트는 JUnit 5와 Spring Boot test starter를 사용해야 하며, 클래스 이름은 `*Tests` 형식을 지켜야 한다.
- 영향받은 service 또는 domain 패키지에 단위 테스트를 반드시 추가하고 `./gradlew test` 로 실행해야 한다.
- 변경 사항을 모두 적용한 뒤에는 전체 테스트를 `./gradlew test` 로 실행하여 PR 전 검증을 완료해야 한다.

## 커밋 및 PR 지침
- 커밋 타입은 `README.md` 에 정의된 `feat`, `fix`, `refactor`, `docs`, `test`, `chore` 등을 그대로 따라야 한다.
- PR에는 요약, 연관 이슈, 테스트 결과를 포함해야 하며 엔드포인트 변경 시 API 예제를 반드시 추가해야 한다.

## 구성·인증·캐시 규칙
- 환경별 오버라이드는 `application-local.yml` 에만 두어야 하며, 어떤 비밀 정보도 커밋해서는 안 된다.
- 캐시 전략은 `cache.type=local|redis`, `portal.credential.store=local|redis` 설정으로만 제어해야 한다.
- 인증 캐시는 `AuthTokenCache` 가 토큰과 `UserDetails` 를 매핑해 access-token TTL 동안 유지해야 하며, 사용자 삭제 시 해당 캐시를 반드시 제거해야 한다.

## 도메인 규칙 및 우선순위
- 졸업 요건 분석은 domain 규칙에 따라야 하며, 누락되거나 불일치하는 데이터는 기술적 오류가 아닌 domain 예외로 처리해야 한다.
- 포털 동기화가 성공해도 전입생·요건 누락 등으로 졸업 분석이 실패할 수 있음을 항상 전제로 해야 한다.
- domain 정확성이 API 편의성이나 클라이언트 기대보다 우선해야 한다.

## 트랜잭션 경계
- 트랜잭션은 application 서비스 레벨에서만 관리해야 하며, 컨트롤러나 domain 모델에서 트랜잭션을 열어서는 안 된다.

## 리팩터링 가드레일
- domain 모델과 infrastructure 모델을 절대 결합해서는 안 된다.
- 흐름 단순화를 이유로 application 서비스를 우회해서는 안 된다.
- 명확한 근거 없이 전역 트랜잭션 경계를 새로 도입해서는 안 된다.

## 에이전트 참고(type/*)
- `type/*` 브랜치에서는 커밋 메시지를 한국어로 작성하고 항상 `* ` 로 시작해야 한다.
- `type` 값은 `feat`, `refactor`, `fix` 를 사용해야 한다.
