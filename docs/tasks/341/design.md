# 편입생 영역별 학점 및 지정과목 이수 현황 설계

## 기준과 상태

- 작업 이슈는 [#341](https://github.com/cchaksa/cchaksa-backend/issues/341)이다.
- 원래 요구사항은 [노션 작업 사항](https://app.notion.com/p/3d01a2480da18046853de1e393ed876d)을 기준으로 하며, 적용 연도·전핵 비교 방식은 2026-09-08 사용자의 후속 확정사항을 우선한다.
- 이 문서는 2026-09-08 사용자가 확정한 3학년 편입생 기준을 반영한 현재 최종 설계다.
- 이전에 차단사항으로 둔 적용 연도 검증과 3·4학년 전핵 전체 과목 목록 확보는 아래 학점 기준으로 대체됐으며 더 이상 선행 조건이 아니다.
- 기존 편입 영역 응답·유효 이수 정규화·지정과목 집계를 유지하면서 전핵·전선의 실제 기준 조회를 연결한다.

## 목표와 범위

3학년 편입생에게 전핵·전선의 기준 대비 이수 현황, 나머지 영역의 취득학점, 지정과목의 취득학점 합계와 과목별 이수 상태를 제공한다.

| 대상 | 제공할 정보 | 판정 범위 |
| --- | --- | --- |
| 전핵 | 정규 입학 코호트 전핵 요구학점의 50%와 본인의 전핵 취득학점 | 취득학점이 절반 기준 이상인지 비교한다. |
| 전선 | 정규 입학 코호트 전선 요구학점의 50%와 본인의 전선 취득학점 | 취득학점이 절반 기준 이상인지 비교한다. |
| 교양·전취·일선 등 | 취득학점과 이수 과목 목록 | 면제·충족 여부를 판정하지 않는다. |
| 지정과목 | 별도 취득학점 합계와 과목별 상태 | 실제 수강 기록과 대조한다. |

총 취득학점, 편입 인정학점, GPA, 외국어 인증의 기존 기능을 유지한다. 130학점·GPA 진행 상태도 그대로 제공하되 이를 편입생의 최종 졸업 가능 여부로 결합하지 않는다.

최종 졸업 자격, 등록학기·졸업심사 추가 판정, 학생별 수동 정보 저장 API, 교양 일괄 면제, 범용 정책 엔진과 관리자 편집 UI는 제외한다. 복수전공 편입생의 전핵·전선 절반 정책은 확인된 근거가 없으므로 단일전공 규칙을 확장하지 않는다. 이 경우 확인 가능한 취득 내역과 기존 GPA·외국어 인증 등은 계속 제공한다.

## 현재 코드의 근거와 통합 지점

경로는 저장소 루트 기준이다.

| 파일 | 현재 동작과 후속 변경 |
| --- | --- |
| `src/main/java/com/chukchuk/haksa/domain/graduation/service/GraduationService.java` | 캐시 조회 전 편입 전용 분기를 유지한다. 일반 재학생 경로는 변경하지 않는다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisService.java` | 적용 연도와 학과를 해석하고 실제 `Requirements`를 조립한다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolver.java` | 현재 학과와 `establishedDepartmentName`이 같은 개편 학과를 후보로 조회한다. 이 별칭 해석을 그대로 재사용한다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/repository/GraduationQueryRepository.java` | `department_area_requirements`를 학과 ID와 입학 연도로 조회하는 `getAreaRequirementsWithCache`를 그대로 재사용한다. 새 저장소나 migration은 만들지 않는다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/policy/TransferAreaEvaluator.java` | 전핵 과목 목록 기반 `Requirements`를 전핵·전선 학점 기준으로 단순화하고 두 영역에 같은 비교 규칙을 적용한다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/dto/TransferAreaProgressDto.java` | 기존 공개 필드를 유지한다. `requiredCourses`는 호환성을 위해 빈 목록을 반환하고 schema 설명을 현재 의미에 맞게 고친다. |
| `src/main/java/com/chukchuk/haksa/domain/graduation/policy/TransferCourseEvaluator.java` | 영역별 실제 취득학점, 유효 성적, 중복과 누락 처리 결과를 그대로 제공한다. |

조회와 조립은 `TransferGraduationAnalysisService`의 private 메서드 하나에 모은다. 새 공개 서비스나 범용 정책 계층은 추가하지 않는다.

## 계산 규칙

### 적용 코호트와 요구학점 조회

- 지원 범위는 3학년 편입이다. `Student.academicInfo.admissionYear`에 저장된 입학/편입 연도를 `transferYear`로 보고 `cohortYear = transferYear - 2`를 적용한다. 예를 들어 2026년 편입생은 2024년 정규 입학 코호트 요건을 사용한다. 이후 현재 `gradeLevel`이 4로 동기화돼도 적용 코호트는 바뀌지 않는다.
- 학적 정보나 `transferYear`가 없으면 전핵·전선 기준을 만들지 않고 두 영역을 각각 `UNAVAILABLE`로 반환한다.
- 주전공은 `student.major`가 있으면 우선하고 없으면 `student.department`를 사용하는 기존 `GraduationMajorResolver` 계약을 따른다. 두 값 모두 없으면 기준을 만들지 않는다.
- 단일전공일 때 `GraduationMajorResolver.resolve(student, cohortYear)`로 현재 학과와 개편 학과 별칭 후보를 해석한 뒤, 확정된 주전공 ID와 `cohortYear`로 `GraduationQueryRepository.getAreaRequirementsWithCache`를 호출한다.
- 복수전공이 있으면 이번 정책의 지원 범위 밖이므로 단일전공 `department_area_requirements` 기준을 적용하지 않는다. 전핵·전선은 `UNAVAILABLE`로 남기고 다른 계산은 계속한다.
- resolver가 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`를 반환하면 기준 부재로 바꿔 편입 전체 응답을 유지한다. 다른 예외는 데이터 부재로 오인해 삼키지 않고 기존 예외 처리로 전달한다.
- 조회 결과에서 `areaType=전핵`, `areaType=전선`을 서로 독립적으로 찾는다. 한 영역 행이 없으면 그 영역만 `UNAVAILABLE`이며, 다른 영역의 정상 기준은 사용한다. 조회 DTO와 테이블의 영역·요구학점 NOT NULL 불변조건은 기존 계약을 따른다.
- 같은 학과·연도·영역에 중복 행이 있으면 서로 다른 `requiredCredits` 값의 개수를 확인한다. 값이 하나면 같은 기준의 중복으로 보고 그 절반을 사용하며, 서로 다른 값이 둘 이상이면 임의 선택하지 않고 해당 영역만 `UNAVAILABLE`로 둔다.

### 전핵·전선 절반 기준

- 전핵과 전선의 필요학점은 각각 정규 입학 코호트의 `department_area_requirements.required_credits × 0.5`다.
- 계산은 `BigDecimal.valueOf(requiredCredits).multiply(new BigDecimal("0.5"))`처럼 10진수로 정확히 수행한다. 올림, 내림, 반올림하거나 정수로 바꾸지 않는다.
- 홀수 기준도 정확한 절반을 유지한다. 예를 들어 45학점의 절반은 `22.5`다.
- 두 영역 모두 `earnedCredits`와 `countedCredits`는 해당 영역의 유효 개인 취득학점이며, `fulfilled`는 `BigDecimal.valueOf(earnedCredits).compareTo(requiredCredits) >= 0`으로 계산한다.
- 전핵 전체 과목 목록을 확보할 수 없으므로 과목별 필수 이수 여부를 판정하지 않는다. 다른 전핵 과목을 포함한 전체 전핵 취득학점을 절반 기준과 비교한다.
- 공개 API의 `requiredCourses`는 기존 클라이언트 호환을 위해 전핵에서도 항상 빈 목록이다. 이 필드를 전핵 전체 과목 목록으로 설명하거나 사용하지 않는다.
- 기준은 있지만 해당 영역의 개인 학점이 불완전하면 `evaluationType=COMPARISON`과 `requiredCredits`는 유지하고 `earnedCredits`, `countedCredits`, `fulfilled`는 null, 사유는 `COURSE_DATA_INCOMPLETE`로 반환한다.

### 유효 이수, 기타 영역과 기존 계산

- 편입 경로에서만 `F`, `R`, `NP`, `IP`, 성적 미확인과 재수강 삭제 기록을 취득학점 집계에서 제외한다. 일반 재학생 SQL과 공용 성적 정책은 바꾸지 않는다.
- 과목코드를 trim·대문자로 정규화하고 같은 과목을 여러 번 더하지 않는다. 정상 수신한 빈 영역의 합계는 0이다. 코드·개인 학점·동일 시점 분류가 불완전하면 영향을 받는 합계를 null로 유지한다.
- 같은 과목의 최신 유효 수강 기록은 연도·학기로만 판단한다. 최신 학기 안에서 개인 학점·성적등급·이수구분이 다르면 원점수와 입력 순서에 관계없이 충돌로 처리한다. 더 최신 학기의 기록으로 교체하면 과거 학기의 충돌을 해제하며, 같은 학기의 일치 기록이 추가돼도 충돌을 해제하지 않는다.
- 취득학점은 `StudentCourse.points`를 사용하고 offering 학점으로 대체하지 않는다. 인정학점 코드 `07045`, `07046`, `00111`, `07050`은 영역 합계에서 제외하고 기존 규칙으로 별도 표시한다.
- 전취를 전핵에 합치지 않는다. 전핵·전선 이외 영역은 `EARNED_ONLY`로 취득학점과 과목을 제공하며 면제 또는 충족을 뜻하지 않는다.
- 총 취득학점은 `StudentAcademicRecord.totalEarnedCredits`를 유지하고 영역·인정학점·지정과목 합계로 재구성하지 않는다.
- 지정과목의 순서·코드·명칭·원본 학점·이수 상태, 개인 취득학점 합계, 미수신과 빈 목록 구분은 기존 구현을 유지한다.
- GPA, 복수전공 시 기존 GPA 기준, 외국어 인증도 기존 동작을 유지한다.

## API 계약과 미확인 상태

`GET /api/graduation/progress`, `analysisType=TRANSFER`, 편입 캐시 우회와 일반 재학생의 `graduationProgress` 계약을 유지한다. 편입 영역은 `transferProgress.areas`에 둔다.

| `TransferAreaProgressDto` 필드 | 타입 | 의미 |
| --- | --- | --- |
| `areaType` | `FacultyDivision` | 기존 이수구분이다. |
| `evaluationType` | `TransferAreaEvaluationType` | `COMPARISON`, `EARNED_ONLY`, `UNAVAILABLE`다. |
| `earnedCredits` | nullable `Integer` | 영역 전체 실제 취득학점이다. |
| `countedCredits` | nullable `Integer` | 전핵·전선 비교에 쓰는 취득학점이며 `earnedCredits`와 같다. 기타 영역은 null이다. |
| `requiredCredits` | nullable `BigDecimal` | 정규 코호트 요구학점의 정확한 절반이다. |
| `fulfilled` | nullable `Boolean` | 전핵·전선 학점 기준의 충족 여부다. |
| `courses` | `List<CourseDto>` | 해당 영역의 유효 이수 과목 목록이다. |
| `requiredCourses` | `List<DesignatedCourseProgressDto>` | 호환성 유지 필드이며 현재 정책에서는 항상 빈 목록이다. |
| `unavailableReasons` | `List<String>` | 기준 또는 개인 데이터 미확인 사유다. |

기존 사유 문자열의 API 호환성을 유지한다. `CORE_CURRICULUM_UNAVAILABLE`은 이제 “적용 코호트의 전핵 요구학점을 조회할 수 없음”을 뜻하고, `ELECTIVE_REQUIREMENT_UNAVAILABLE`은 전선 요구학점 부재를 뜻한다. `COURSE_DATA_INCOMPLETE`는 개인 이수 데이터가 불완전함을 뜻한다. 한 영역의 사유를 다른 영역에 전파하지 않는다.

`analysisStatus=MANUAL_REVIEW_REQUIRED`는 편입 부분 진단이 최종 졸업 자격을 확정하지 않는다는 의미로 유지한다. 3학년 편입 정책을 명시적으로 적용하므로 `TRANSFER_ENTRY_GRADE_UNKNOWN`을 항상 추가하던 동작은 제거한다. 기준 부재에 따른 기존 전핵·전선 수동 확인 사유는 해당 영역이 `UNAVAILABLE`일 때만 남긴다.

## 완료 조건과 Wiki

1. 2026년 편입생이 2024년 학과 요건을 조회하고, 학과 개편 별칭도 기존 resolver를 통해 해석한다.
2. 전핵과 전선 요구학점의 정확한 50%를 독립적으로 계산하며 홀수 기준도 반올림하지 않는다.
3. 취득학점이 기준 미만·같음·초과일 때 `fulfilled`가 각각 false·true·true다.
4. 한 영역 기준이 누락돼도 다른 영역과 기타 취득학점·지정과목·총학점·GPA·외국어 인증을 제공한다.
5. 같은 영역의 동일 기준 중복은 허용하고 상충 기준 중복은 해당 영역만 `UNAVAILABLE`로 둔다.
6. 연도·주전공·요건이 없거나 복수전공이면 지원하지 않는 기준만 `UNAVAILABLE`로 남는다.
7. 전핵 과목 목록을 만들거나 `requiredCourses`에 추정 데이터를 넣지 않는다.
8. 일반 재학생 API·계산·캐시가 회귀하지 않으며 실행 중인 `/v3/api-docs`와 인증 GET으로 실제 계약을 확인한다.
9. 구현 diff와 검증 증거를 독립 Sol 검토자가 확인하고, 관련 Wiki를 실제 동작과 일치시킨다.

구현 완료 시 Wiki `master`의 `API-and-Authentication`, `Core-Domain-Flows`, `Troubleshooting`을 갱신한다. DB schema는 바뀌지 않으므로 `Project-Architecture` 갱신은 필요하지 않다.

## 결정 기록

- 2026-09-06. 노션 우선, 전체 졸업 판정·수동 입력 제외, 편입 전용 영역 DTO와 유효 이수 집계를 선택했다.
- 2026-09-07. `TransferCourseEvaluator`가 유효 수강 기록을 한 번 정규화하고 영역·지정과목 평가기가 같은 결과를 소비하도록 연결했다. 전핵·전선 기준 입력은 원천 결정 전까지 `UNAVAILABLE`로 반환했다.
- 2026-09-07. 최종 판정 필드와 편입생 수동 PATCH·쓰기 서비스 경로를 제거하고 V14 엔티티·migration은 보존했다. Java 17 기준 `check`와 관련 API 계약 테스트를 통과했다.
- 2026-09-08. 이전의 “적용 교육과정 연도 미확인”과 “3·4학년 전핵 전체 과목 목록 필수” 결정을 폐기했다. 3학년 편입 연도에서 2를 뺀 정규 코호트의 `department_area_requirements` 전핵·전선 학점을 각각 정확히 50% 적용하는 정책으로 대체했다.
- 2026-09-08. 복수전공 편입 정책은 근거 없이 확장하지 않으며 후속 정책 확인 대상으로 남겼다.
- 문서 검사와 후속 구현 증거는 [계획의 실행 기록](plan.md#실행-기록)에 기록한다.
