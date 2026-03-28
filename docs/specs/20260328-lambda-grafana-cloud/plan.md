# Lambda Grafana Cloud Plan

## Architecture / Layering
- Domain impact:
  - 없음. 도메인 규칙/서비스 로직은 건드리지 않는다.
- Application orchestration:
  - Spring Boot tracing exporter와 Micrometer OTLP metrics registry가 Lambda extension localhost endpoint로 전송한다.
- Infrastructure touchpoints:
  - `cchaksa-aws-iac`의 Lambda env, layer, IAM, Secrets Manager, Grafana Cloud stack/token
- Global/config changes:
  - `application.yml`에 OTLP tracing / metrics export 환경변수 바인딩 추가
  - `build.gradle`에 `micrometer-registry-otlp` 추가

## Data / Transactions
- Repositories touched:
  - 없음. 저장소/트랜잭션 코드는 수정하지 않는다.
- Transaction scope:
  - 요청 처리 후 telemetry export는 비동기 부가 동작으로 본다.
- Consistency expectations:
  - observability 신호는 eventual consistency 허용

## Testing Strategy
- Domain tests:
  - 추가 없음
- Application tests:
  - 기존 `./gradlew test` 전체 수행으로 회귀 확인
- Integration/API tests:
  - Terraform apply 후 shadow Lambda 실제 호출로 Grafana Cloud 유입 확인
- Additional commands:
  - `terraform fmt -recursive`
  - `terraform validate`
  - `terraform plan -var-file=tfvars/develop-shadow.tfvars`
  - `./gradlew test`
  - `./gradlew lambdaZip`

## Rollout Considerations
- Backward compatibility:
  - 기본 metrics OTLP export는 비활성화 상태이므로 기존 local/dev/prod 런타임 영향 최소
- Observability / metrics:
  - develop-shadow에서만 OTLP metrics export와 sampling 1.0 활성
- Feature flags / toggles:
  - `backend_serverless.grafana_cloud.enabled`
  - `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED`
