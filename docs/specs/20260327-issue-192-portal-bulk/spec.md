# 포털 연동 Bulk I/O 최적화 스펙 (Issue #192)

## 1. Feature Overview
- Purpose: 포털 연동 후 수행되는 학업 이력 동기화 과정의 bulk insert/delete 구간을 최적화하여 현재 16초까지 지연되는 내부 I/O를 5초 이하로 단축한다.
- Scope
  - In: `SyncAcademicRecordService` 내 수강 이력 업데이트/삭제 로직, `AcademicRecordRepository` 중 포털 연동 경로에서 호출되는 bulk 저장, Spring JPA/Hibernate 글로벌 설정.
  - Out: 포털 크롤러(API)·스케줄러·ScrapeJob 큐잉 로직, 사용자-facing API 스펙 변경, DB 스키마 변경.
- Expected Impact: JDBC batch와 in-place dirty checking을 활용해 대량 insert/update/delete의 round-trip 횟수를 최소화하고, 16초 → 5초 이하(목표)로 포털 연동 완료 시간을 단축한다.
- Stakeholder Confirmation: 사용자(PO) 구두 요청 – Issue #192 코멘트.

## 2. Domain Rules
- Rule 1: 포털 데이터는 여전히 소스 오브 트루스로 간주되어 DB 기록과 정확히 동기화되어야 한다.
- Rule 2: 삭제 정책(포털에 존재하지 않는 수강 기록 삭제)과 재수강/성적 로직은 변경되지 않는다.
- Rule 3: 동기화 트랜잭션은 학생 단위로 atomic 해야 한다.
- Mutable Rules: JDBC batch 크기, flush 주기.
- Immutable Rules: 학업 이력 합산 규칙, CourseOffering/Professor 생성 규칙.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 포털 최초 연동 (LINK)
  - Trigger: 학생이 포털 계정으로 최초 연동
  - Actor: PortalSyncService
  - Steps:
    1. Scrape 결과를 PortalSyncService가 전달받음
    2. InitializePortalConnectionService가 기본 정보 저장
    3. SyncAcademicRecordService가 신규/변경/삭제 diff 처리
  - Expected Result: sync 단계가 5초 이하로 완료되며 기존 데이터와 정확히 일치

- Scenario Name: 포털 재연동 (REFRESH)
  - Trigger: 이미 연동된 사용자가 재동기화 수행
  - Actor: PortalSyncService
  - Steps: LINK와 동일하나 RefreshPortalConnectionService 경로 사용
  - Expected Result: 변경 사항만 빠르게 반영, 캐시 무효화 유지

### Exception / Boundary Flow
- Scenario Name: Batch 설정 오류
  - Condition: Hibernate batch 설정이 미적용/오류
  - Expected Behavior: 안전하게 단건 모드로 동작하되 로그 경고로 추적

## 4. Transaction / Consistency
- Transaction Start Point: `SyncAcademicRecordService.executeWithPortalData/executeForRefreshPortalData`
- Transaction End Point: 서비스 메서드 종료 시점 (Spring @Transactional)
- Atomicity Scope: 학생 단위 (studentId별)
- Eventual Consistency Allowed: 불허 – 실패 시 전체 롤백

## 5. API List
- N/A (내부 배치/서비스 로직만 변경)

## 6. Exception Policy
- 기존 ErrorCode 유지. 작업 실패 시 `PortalScrapeException` 혹은 `CommonException` 경로 동일
- 추가: batch 설정 문제 감지 시 warn 로그 남기고 fallback

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete (영향 없음)
- [ ] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete (Repository batch 활용)
- [ ] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete (영향 없음)

## 8. Generated File List
- docs/specs/20260327-issue-192-portal-bulk/
  - Description: Issue #192 스펙 번들
  - Layer: Docs
