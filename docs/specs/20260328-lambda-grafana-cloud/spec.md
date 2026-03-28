# Lambda Grafana Cloud 연동 Spec

## 1. Feature Overview
- Purpose:
  - `develop-shadow` Lambda 백엔드가 Grafana Cloud OTLP gateway로 trace, metrics, logs를 전송할 수 있게 만든다.
- Scope
  - In:
    - Spring Boot 애플리케이션의 OTLP metrics registry 의존성 추가
    - OTLP tracing / metrics endpoint 환경변수 바인딩 추가
    - Lambda console log 수집을 전제로 한 profile 유지
    - shadow 검증용 스펙/작업 문서 작성
  - Out:
    - 도메인 비즈니스 로직 변경
    - 외부 API 계약 변경
    - 로그 브리지(logback appender to OTel) 2차 도입
- Expected Impact:
  - Lambda에서 Grafana Cloud extension이 수집할 trace/metrics 전송 경로가 활성화된다.
  - `develop-shadow` 프로필의 콘솔 로그가 Lambda Telemetry API를 통해 Grafana Cloud logs로 전달될 수 있다.
- Stakeholder Confirmation:
  - 2026-03-28 사용자 메시지 `PLEASE IMPLEMENT THIS PLAN`으로 구현 승인 확인.

## 2. Domain Rules
- Rule 1:
  - 관측성 추가는 설정/인프라 레이어 중심으로 처리하고 기존 비즈니스 로직은 변경하지 않는다.
- Rule 2:
  - metrics export는 `develop-shadow`에서만 명시적으로 켜고, 기본값은 비활성화한다.
- Rule 3:
  - logs는 우선 기존 `develop-shadow -> dev` profile의 `CONSOLE` appender를 유지해 Lambda extension 경로를 사용한다.
- Mutable Rules:
  - OTLP endpoint와 sampling 확률은 환경변수로 조정 가능하다.
- Immutable Rules:
  - 외부 API 응답과 도메인 동작은 변경하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name:
  - Trigger:
    - develop-shadow Lambda가 요청을 처리한다.
  - Actor:
    - API Gateway -> Lambda -> Grafana Cloud extension
  - Steps:
    - Spring Boot tracing이 `management.otlp.tracing.endpoint`로 span을 전송한다.
    - Micrometer OTLP registry가 `management.otlp.metrics.export.url`로 metrics를 전송한다.
    - 애플리케이션 콘솔 로그가 Lambda Telemetry API를 통해 extension에 전달된다.
    - extension이 세 신호를 Grafana Cloud OTLP gateway로 전달한다.
  - Expected Result:
    - Grafana Cloud에서 develop-shadow Lambda의 trace, metrics, logs를 조회할 수 있다.

### Exception / Boundary Flow
- Scenario Name:
  - Condition:
    - OTLP metrics registry 의존성이 빠졌거나 metrics export env가 꺼져 있다.
  - Expected Behavior:
    - trace/log는 남더라도 metrics는 Grafana Cloud에 유입되지 않는다.
- Scenario Name:
  - Condition:
    - Lambda console log가 Telemetry API에서 수집되지 않는다.
  - Expected Behavior:
    - logs는 누락될 수 있으며, 후속으로 logback OTel bridge 도입 여부를 검토한다.

## 4. Transaction / Consistency
- Transaction Start Point:
  - 애플리케이션 시작 시 observability auto-configuration 초기화
- Transaction End Point:
  - Lambda invocation 종료 전 telemetry export enqueue
- Atomicity Scope:
  - 비즈니스 트랜잭션과 관측성 export는 분리된다.
- Eventual Consistency Allowed:
  - 허용. Grafana Cloud 반영은 비동기 수집으로 간주한다.

## 5. API List (필요 시)
- 없음:
  - 외부 공개 API 추가/변경 없음

## 6. Exception Policy
- Error Code:
  - 별도 애플리케이션 에러코드 추가 없음
  - Condition:
    - OTLP endpoint 미설정, collector 부재, 인증 오류
  - Message Convention:
    - 기존 Spring/Micrometer/OpenTelemetry 로그 포맷 사용
  - Handling Layer:
    - 인프라/설정 레이어
  - User Exposure:
    - 사용자 응답에는 노출하지 않고 운영 로그에서만 확인

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- `docs/specs/20260328-lambda-grafana-cloud/spec.md`
  - 구현 범위와 승인 근거 정리
  - Layer: documentation
- `docs/specs/20260328-lambda-grafana-cloud/clarify.md`
  - 외부 스택/토큰/설정 결정사항 정리
  - Layer: documentation
- `docs/specs/20260328-lambda-grafana-cloud/plan.md`
  - 애플리케이션/인프라 반영 계획 정리
  - Layer: documentation
- `docs/specs/20260328-lambda-grafana-cloud/tasks.md`
  - 작업 체크리스트와 검증 로그 관리
  - Layer: documentation
- `build.gradle`
  - Micrometer OTLP registry 의존성 추가
  - Layer: application-config
- `src/main/resources/application.yml`
  - OTLP tracing / metrics export 환경변수 바인딩 추가
  - Layer: global-config
