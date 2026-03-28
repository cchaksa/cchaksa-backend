# Lambda Grafana Cloud Tasks

## Checklist
- [ ] Domain tests written
- [x] Application layer updated
- [x] Infrastructure layer updated
- [x] Global/config reviewed
- [ ] API/controller updated
- [x] Documentation updated

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | Success | 2026-03-28 |
| 2 | `./gradlew lambdaZip` | Success | 2026-03-28 |
| 3 | `terraform fmt -recursive` | Success | 2026-03-28 |
| 4 | `terraform validate` | Success | 2026-03-28 |
| 5 | `terraform plan -var-file=tfvars/develop-shadow.tfvars` | Success | 2026-03-28 |
| 6 | `terraform apply -auto-approve -var-file=tfvars/develop-shadow.tfvars` | Success | 2026-03-28 |
| 7 | `aws lambda update-function-code --publish` + `update-alias live` | Success | 2026-03-28 |
| 8 | `curl https://12eoa1iy3h.execute-api.ap-northeast-2.amazonaws.com/health` | Success (200) | 2026-03-28 |

## Notes
- Observation:
  - logs는 1차 구현에서 console appender + Lambda Telemetry API 경로를 사용한다.
  - Terraform만으로는 Lambda 코드가 교체되지 않아, shadow 검증을 위해 build zip을 수동 publish하고 `live` alias를 새 버전 `39`로 전환했다.
  - custom domain `https://dev.api.cchaksa.com/health`는 code publish 직후 일시적으로 500이 있었으나 재시도 시 200으로 정상화됐다.
  - Grafana Cloud write path는 extension 기동 로그상 정상으로 보이지만, read-back 자동 검증은 traces/logs/metrics 모두 별도 UI 확인이 남아 있다.
