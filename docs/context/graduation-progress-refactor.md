# Refactoring Context Template
- Context는 모든 판단의 기준이다.
- Context 없이는 작업을 진행하지 않는다.

## 리팩토링 목적
- 대상: `/api/graduation/progress` 흐름의 `GraduationService.resolvePrimaryMajorId` 및 졸업 요건 조회 경로
- 리팩토링 이유: 학과 개편으로 동일 `establishedDepartmentName` 아래 여러 학과 ID가 존재하며, `student.getMajor()`가 있든 없든 단일 ID만으로 졸업 요건을 찾으면 사용자가 실제로 속한 학과의 요구 사항을 놓치는 문제가 발생한다. Major가 null일 때 Department를 쓰는 기존 분기는 단지 Major가 존재하지 않는 학과가 있기 때문이며, 근본 원인은 단일 학과 ID 조회 전략이다.
- 기대 효과 (성능 / 가독성 / 유지보수성 등): 학과 명칭 기반 후보 탐색으로 조건 분기 가독성을 높이고, 졸업 요건 조회 성공률과 장애 복원력을 향상한다. 캐시 키 재사용으로 추가 비용을 최소화한다.

## 변경 허용 범위
- 허용되는 변경: `GraduationService`의 주전공 결정 로직, 졸업 요건 조회 플로우에 필요한 헬퍼·DTO·캐시 활용 코드, `DepartmentRepository/Service`에 동일 전통 학과명 조회 기능 추가, 해당 도메인 서비스 단위 테스트
- 금지되는 변경: `/api/graduation/progress` 응답 스키마·URI·인증 흐름 변경, `AcademicCache` TTL/Key 규칙 변경, Graduation Query SQL/모델을 목적 외로 확장, 다른 도메인/애플리케이션 서비스의 비관련 로직 수정
- 수정 대상 Layer: `domain.graduation`, `domain.department`, 필요 시 `domain.student` 단위 테스트
- 수정 금지 Layer: `application`, `global`, `infrastructure`(캐시·controller·security 등 전역 구성)

## 동작 불변 조건 (최우선)
- 외부 API 동작: `GET /api/graduation/progress`는 여전히 `SuccessResponse<GraduationProgressResponse>`를 200으로 반환하며 필드/구조/로깅 계약이 바뀌지 않는다.
- 반환 값 / 예외: 조건에 맞는 졸업 요건이 없으면 기존과 동일하게 `CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND)`를 던지고, 편입생 검증·MDC 처리·`hasDifferentGraduationRequirement` 플래그 판단도 기존 기준(특정 학과+2025년) 그대로 유지한다.
- 트랜잭션 경계: `GraduationService`는 계속 `@Transactional(readOnly = true)`로 동작하며 controller/application/global에서 새로운 트랜잭션을 열지 않는다.
- 성능 특성 (있다면): 캐시는 여전히 `departmentId + admissionYear` 키로 저장하며, 후보 학과 ID 조회는 주전공 미정인 경우에만 수행하고 첫 성공 시 즉시 종료하여 기존 SLA를 유지한다.

## 기존 문제점 (As-Is)
- 구조적 문제: `resolvePrimaryMajorId`가 Major 존재 여부에 따라 단일 학과 ID만 반환해 학과 개편(동일 계열명, 상이한 ID) 시 요구사항 데이터를 한 번만 탐색하고 실패하면 회복하지 못한다. Major가 존재하는 학생도 동일 문제를 겪는다.
- 테스트 문제: 주전공 부재 시 졸업 요건 탐색 성공/실패 여부를 다루는 단위 테스트가 없어 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND` 회귀를 사전에 포착하지 못한다.
- 기술 부채: 요구사항 탐색 실패 시 재시도 전략이나 로그 컨텍스트에 후보 정보가 남지 않아 운영 중 원인 분석이 어렵다.

- 구조 변화 요약: `GraduationService`는 Major 존재 여부와 관계없이 `Department.establishedDepartmentName` 기반으로 동일 명칭 학과 ID 리스트를 가져와 순차적으로 졸업 요건 데이터를 조회하고, 최초로 성공하는 ID를 `primaryDepartmentId`로 사용한다. Major 정보가 있으면 해당 Department를 우선 후보에 포함한다.
- 책임 이동 여부: 후보 학과 ID 조회는 `DepartmentRepository`(또는 이를 감싸는 서비스)에서 담당하고, 졸업 요건 존재 여부 판단과 캐시 활용은 `GraduationQueryRepository`와 `GraduationService`의 기존 책임을 유지한다.
- 삭제/통합 대상: 현 `resolvePrimaryMajorId` 단일 분기 로직은 후보 탐색 로직으로 대체하며, 중복되는 null 처리/예외 메시지는 통합한다. 기타 레이어 삭제/통합은 없다.

## 회귀 방지 전략
- 유지해야 할 테스트: 기존 글로벌/보안 테스트(`JwtAuthenticationFilterTests`, `AuthTokenCacheTests`, `AuthControllerTests`, `ChukchukHaksaApplicationTests`)는 그대로 유지한다.
- 추가해야 할 테스트: `GraduationServiceTests`(또는 동등 단위)에서
  1) 주전공이 있는 학생은 해당 Major ID가 후보 목록 선두로 배치되고 기존 학과 ID 그대로 사용,
  2) 주전공이 없고 동일 명칭 학과 후보 중 두 번째 ID에서 졸업 요건을 찾는 경우 성공,
  3) 모든 후보에서 졸업 요건이 없으면 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND` 예외가 유지됨
  을 검증한다.
- 절대 삭제하면 안 되는 테스트: 위에서 언급한 글로벌/보안 테스트 + 신규 추가되는 `GraduationService` 리팩터링 검증 테스트

## Phase별 체크리스트
- [x] Phase 1 Context 확정
- [ ] Phase 2 Regression Test 확보
- [ ] Phase 3 Domain/Application 리팩토링
- [ ] Phase 4 Infrastructure 정리
- [ ] Phase 5 Global 영향 점검
- [ ] Phase 6 API 동작 검증

## 생성/변경 파일 목록
- 경로: `docs/context/graduation-progress-refactor.md`
  - 변경 유형 (수정/이동/삭제): 신규 작성
  - 영향 Layer: 문서
- 경로: `src/main/java/com/chukchuk/haksa/domain/graduation/service/GraduationService.java`
  - 변경 유형: 수정 (후보 학과 탐색/졸업 요건 조회 흐름)
  - 영향 Layer: domain - graduation
- 경로: `src/main/java/com/chukchuk/haksa/domain/department/repository/DepartmentRepository.java` (+ 필요 시 Service)
  - 변경 유형: 수정 (establishedDepartmentName 기반 조회 메서드 추가)
  - 영향 Layer: domain - department
- 경로: `src/main/java/com/chukchuk/haksa/domain/graduation/repository/GraduationQueryRepository.java`
  - 변경 유형: 수정 (다중 학과 요건 조회 유틸/캐시 활용)
  - 영향 Layer: domain - graduation
- 경로: `src/test/java/com/chukchuk/haksa/domain/graduation/service/GraduationServiceTests.java` (신규)
  - 변경 유형: 신규 작성
  - 영향 Layer: test - domain graduation
