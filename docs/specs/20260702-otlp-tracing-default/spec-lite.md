# OTLP trace exporter 자동 설정 제외

## 배경

Sentry `JAVA-SPRING-BOOT-4C`에서 OTLP exporter가 `localhost:4318`로 span export를 시도하다가 연결 실패가 반복된다.

## 목표

tracing 자체는 유지하되, OTLP collector가 없는 환경에서 trace export가 발생하지 않게 한다.

## 계획

1. Spring Boot 3.2.5에서 별도 export enabled 설정이 없으므로 OTLP tracing auto-configuration만 제외한다.
2. local override와 metrics export 기본 비활성 설정은 유지한다.
3. 설정 파일 검증과 테스트로 기본값이 안전한지 확인한다.

## 완료 기준

- `MANAGEMENT_TRACING_ENABLED`가 없으면 tracing은 활성화된다.
- OTLP trace exporter는 자동 구성되지 않는다.
- Sentry `JAVA-SPRING-BOOT-4C` 재발 여부를 배포 후 확인할 수 있다.
