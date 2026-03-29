# Tasks Template

## Checklist
- [ ] Domain tests written (N/A지만 template 준수 위해 유지)
- [ ] Application layer updated (예외 전달 영향 최소 확인)
- [x] Infrastructure layer updated (`AppleOidcService` 허용 목록, 예외 처리)
- [x] Global/config reviewed (`application.yml`, env key 설명)
- [ ] API/controller updated (해당 없음)
- [x] Documentation updated (spec bundle, follow-up)

### Work Breakdown
1. `AppleOidcService`에 허용 목록 설정 주입 및 fallback 로직 추가
2. TokenException 래핑 제거 -> 원본 예외 전달
3. `AppleConfigurationProperties` (또는 동일 클래스) 업데이트 + `application.yml` 기본 값 추가
4. `AppleOidcServiceTests` 보강: 허용 목록 통과/실패, fallback 동작, 예외 전달 확인
5. Spec/tasks 문서 업데이트 및 Ops follow-up 기록
6. Gradle 테스트 2종 실행 후 결과 기입

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.infrastructure.oidc.AppleOidcServiceTests` | PASS | 2026-03-29 |
| 2 | `./gradlew test` | PASS | 2026-03-29 |

## Notes
- Observation: 운영 환경에 `APPLE_ALLOWED_CLIENT_IDS`를 설정하지 않으면 기존 `client-id` 값으로 자동 fallback된다. 별도 알림 필요.
