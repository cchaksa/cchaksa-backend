# Context Notes

- 2026-06-02: 개발 서버 장애 원인은 scraper와 backend의 `flangPassGb` 값 계약 불일치다.
- 2026-06-02: 개발 scraper는 `통과`/`미통과`를 보낼 수 있고 실패 payload에는 `미통과`가 있었다.
- 2026-06-02: backend `PortalDataMapper`는 현재 `Y`/`N`/blank만 허용한다.
- 2026-06-02: 매퍼 예외는 `PortalCallbackPostProcessor`의 후처리 실패 처리 범위 밖에서 발생해 job이 `POST_PROCESSING`에 남을 수 있다.
- 2026-06-02: 이번 수정은 stale reconciler가 아니라 callback 후처리 경계에서 실패를 확정하는 방식으로 진행한다.
- 2026-06-02: RED 확인 결과 `통과`/`미통과`는 매퍼에서 `IllegalArgumentException`을 던졌고, 알 수 없는 값은 `SCRAPE_RESULT_SCHEMA_INVALID`로 변환되지 않았다.
- 2026-06-02: `PortalCallbackPostProcessor`는 매퍼의 `IllegalArgumentException`을 `SCRAPE_RESULT_SCHEMA_INVALID`로 변환하고, 외부 `ScrapeResultCallbackService`가 기존 `FAILED_RESULT_SCHEMA`로 job을 확정한다.
- 2026-06-02: 로컬 기본 JDK 24에서는 Gradle test task 생성이 `Type T not present`로 실패해, 프로젝트 Java 17 toolchain에 맞춰 `JAVA_HOME`을 Temurin 17로 지정해 검증했다.
