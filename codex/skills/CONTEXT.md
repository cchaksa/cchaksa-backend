# Spec Bundle Template (Context)

Context는 `docs/specs/<date-slug>/` 아래의 스펙 묶음으로 구성된다.  
아래 항목은 `spec.md` 안에서 작성하며, 모호점은 `clarify.md`, 구현 전략은 `plan.md`, 작업 분해는 `tasks.md`로 분리한다.

---

## 1. Feature Overview (Required)

- Purpose:
- Scope:
  - In:
  - Out:
- Expected Impact:
- Stakeholder Confirmation:

---

## 2. Domain Rules (Required)

- Rule 1:
- Rule 2:
- Rule 3:

- Mutable Rules:
- Immutable Rules:

---

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name:
  - Trigger:
  - Actor:
  - Steps:
  - Expected Result:

### Exception / Boundary Flow
- Scenario Name:
  - Condition:
  - Expected Behavior:

---

## 4. Transaction / Consistency Policy (Required)

- Transaction Start Point:
- Transaction End Point:
- Atomicity Scope:
- Eventual Consistency Allowed:

---

## 5. API List (When Applicable)

- Endpoint:
  - Method:
  - Request DTO:
  - Response DTO:
  - Authorization:
  - Idempotency:

---

## 6. Exception Policy (Required)

- Error Code:
  - Condition:
  - Message Convention:
  - Handling Layer:
  - User Exposure:

---

## 7. Phase Checklist

- [ ] Phase 1 Spec fixed (spec.md + clarify.md + plan.md + tasks.md created)
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

---

## 8. Generated File List

- Path:
  - Description:
  - Layer:

> 참고: Lite 스펙은 `spec-lite.md` 하나로 시작하지만, 범위를 넘어서면 즉시 본 템플릿으로 승격한다.
