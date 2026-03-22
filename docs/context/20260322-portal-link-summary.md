# Context: 포털 연동 요약 API 추가

## 1. Feature Overview
- **Purpose**: 비동기 포털 연동 완료 직후 필요한 학생 요약 정보를 빠르게 조회할 수 있는 API를 제공한다.
- **Scope**:
  - **In**: `job_id` 기반 연동 상태 조회 시 학생 기본 정보 요약을 제공하는 전용 API 및 필요한 서비스/DTO 추가, 보안/권한 검토.
  - **Out**: 기존 연동/재연동 내부 로직 변경, 프론트엔드 구현, 성적/학업 상세 조회 API 개편.
- **Expected Impact**: 프론트가 비동기 연동 완료 직후 학생 기본 정보를 즉시 확보하여 UX 저하 없이 다음 화면을 구성할 수 있다.

## 2. Domain Rules
- **Rule 1**: 요약 API는 연동 job의 소유자(동일 사용자)만 조회할 수 있다.
- **Rule 2**: job 상태가 `SUCCEEDED`일 때만 학생 요약 데이터를 반환하며, 그 외 상태는 적절한 에러 또는 상태 설명만 제공한다.
- **Rule 3**: 응답에는 기존 동기 API가 내려주던 `studentInfo` 필드와 동일한 항목을 포함한다.
- **Mutable Rules**: job 상태/오류 코드 매핑 방식은 향후 확장 가능.
- **Immutable Rules**: 권한 검증 및 데이터 노출 범위는 현재 정의된 사용자 범위를 벗어나지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- **Scenario**: 사용자 A가 비동기 연동을 완료하고 요약 정보를 조회
  - Trigger: `POST /portal/link` 결과로 받은 `job_id`를 사용해 새 API 호출
  - Steps:
    1. 사용자 A가 `job_id`를 포함해 API 호출
    2. 서버는 job 및 사용자 소유권 검증
    3. job 상태가 `SUCCEEDED`인지 확인
    4. 연동 저장소에서 최신 학생 요약 정보를 가져와 응답
  - Expected Result: `studentInfo` 필드가 포함된 JSON을 반환

### Exception Flow
- **Scenario**: job 상태가 아직 `QUEUED/RUNNING`인 경우
  - Condition: job이 완료되지 않음
  - Expected: 409 또는 202 유사 상태 코드와 함께 대기 메시지 반환
- **Scenario**: job 소유자가 아닌 사용자가 조회
  - Expected: 404 또는 403
- **Scenario**: job이 `FAILED` 상태
  - Expected: 실패 상태와 오류 코드/메시지를 전달, `studentInfo`는 없음

## 4. Transaction and Consistency Policy
- **Transaction Start**: 요약 조회가 job 및 학생 데이터 저장소를 조회하는 시점
- **Transaction End**: 학생 요약 정보를 응답하기 전
- **Atomicity Scope**: 단일 조회 트랜잭션 (읽기 전용)
- **Eventual Consistency**: 허용 (job 완료와 데이터 반영 사이에 미세한 지연 가능)

## 5. API List
- **Endpoint**: `GET /portal/link/jobs/{job_id}/summary`
  - Request: Path `job_id`, 인증 토큰
  - Response:
    ```json
    {
      "job_id": "UUID",
      "status": "succeeded",
      "studentInfo": {
        "name": "...",
        "school": "...",
        "majorName": "...",
        "studentCode": "...",
        "gradeLevel": 0,
        "status": "...",
        "completedSemesterType": 0
      },
      "finished_at": "2026-03-22T09:00:00Z"
    }
    ```
  - Authorization: 로그인 사용자, job owner
  - Idempotency: 조회성 API

## 6. Exception Policy
- `PORTAL_JOB_NOT_FOUND`: job_id 없음 또는 권한 없음 → 404
- `PORTAL_JOB_NOT_COMPLETED`: job 미완료 → 409
- `PORTAL_JOB_FAILED`: job 실패 → 422 (실패 정보 포함)

## 7. Phase Checklist
- [x] Phase 1 Context
- [ ] Phase 2 Domain
- [ ] Phase 3 Application
- [ ] Phase 4 Infrastructure
- [ ] Phase 5 Global
- [ ] Phase 6 API/Controller

## 8. Generated File List
- `docs/context/20260322-portal-link-summary.md` (Context)
- `src/main/java/...` TBD
