# Context Template

Context is the basis for all decisions.  
Requirements not explicitly written in Context are treated as nonexistent.  
No work starts without Context.

---

## 1. Feature Overview (Required)

- Purpose: (One sentence describing the problem this feature solves)
- Scope:
  - In:
  - Out:
- Expected Impact: (Quantitative or qualitative)

If this section is empty, stop the work.

---

## 2. Domain Rules (Highest Priority, Required)

- Rule 1:
- Rule 2:
- Rule 3:

- Mutable Rules:
- Immutable Rules:

If at least one rule is not specified, stop the work.

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

## 4. Transaction and Consistency Policy (Required)

- Transaction Start Point:
- Transaction End Point:
- Atomicity Scope:
- Eventual Consistency Allowed:

---

## 5. API List (Optional / Required When Present)

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

- [ ] Phase 1 Context: requirements, domain rules, exception policy fixed
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

---

## 8. Generated File List (Required)

- Path:
- Description:
- Layer:

The generated file list must match the actual commit contents.
