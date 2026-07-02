# Context Notes

- `JAVA-SPRING-BOOT-4C`는 OTLP trace exporter가 `localhost:4318`로 연결을 시도하면서 발생한다.
- Spring Boot 3.2.5 설정 메타데이터에는 `management.tracing.export.otlp.enabled`가 없다.
- `OtlpHttpSpanExporter`는 `OtlpAutoConfiguration`에서 구성되므로 해당 auto-configuration만 제외한다.
- `management.tracing.enabled` 기본값은 `true`로 유지해 trace/span ID 생성은 보존한다.
