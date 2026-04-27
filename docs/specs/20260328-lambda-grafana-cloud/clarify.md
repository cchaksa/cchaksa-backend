# Lambda Grafana Cloud Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Lambda console log가 Grafana Cloud logs까지 안정적으로 유입되는가 | 개발/운영 | Open |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | Grafana Cloud stack slug는 `developshadowcchaksa`를 사용한다 | Cloud API에서 하이픈 포함 slug를 거부해 실제 생성 가능한 slug로 확정 | 2026-03-28 |
| 2 | Lambda extension용 `GRAFANA_CLOUD_INSTANCE_ID`는 `1575247`를 사용한다 | OTLP gateway 인증 테스트에서 `1575247`만 `200 {"partialSuccess":{}}` 응답 확인 | 2026-03-28 |
| 3 | ingest token은 stack-scoped access policy(`metrics:write`, `logs:write`, `traces:write`)로 분리한다 | 삭제/관리 토큰과 수집 토큰의 권한을 분리해야 안전하다 | 2026-03-28 |
| 4 | tracing env는 `MANAGEMENT_OTLP_TRACING_ENDPOINT`를 사용한다 | Spring Boot 3.2.5 configuration metadata 기준 유효 키가 `management.otlp.tracing.endpoint`이기 때문 | 2026-03-28 |
| 5 | metrics export는 `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=true`일 때만 켠다 | 기본 환경에서 불필요한 OTLP 연결 시도를 막기 위해서다 | 2026-03-28 |

## Risks / Unknowns
- Item:
  - Lambda console log -> Telemetry API -> Grafana Cloud logs 경로가 shadow에서 일부 누락될 수 있다
  - Impact:
    - traces/metrics만 보이고 logs는 대시보드에서 누락될 수 있다
  - Mitigation:
    - shadow 검증 후 필요하면 logback OTel bridge를 후속 패치로 추가한다
- Item:
  - Lambda secret이 customer managed KMS key로 전환되면 현재 IAM 권한이 부족할 수 있다
  - Impact:
    - extension이 토큰을 읽지 못해 전송이 실패할 수 있다
  - Mitigation:
    - secret 암호화 키 변경 시 `kms:Decrypt` 권한을 함께 추가한다

## Follow-ups
- [ ] shadow 호출 후 Grafana Cloud traces / metrics / logs 유입 여부를 캡처한다 (개발/운영)
