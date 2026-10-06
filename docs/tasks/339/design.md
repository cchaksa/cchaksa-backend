# 편입생 졸업요건 부분 진단 설계

## 연결 이슈

- [#339 편입생 졸업 요건 분석 및 지정 과목 이수 현황 제공](https://github.com/cchaksa/cchaksa-backend/issues/339)
- [수원대학교 졸업요건 안내](https://www.suwon.ac.kr/index.html?menuno=734)

## 목표

편입생에게 일반 재학생의 영역별 졸업요건을 잘못 적용하지 않으면서, 현재 저장된 데이터로 확정할 수 있는 총 취득학점, 편입 인정학점, GPA, 외국어 인증, 지정과목 이수 현황을 제공한다. 자동 판정 근거가 없는 필수과목, 전공선택 비율, 등록학기, 졸업논문·시험은 수동 확인 사유로 명시하며 최종 졸업 가능 여부를 단정하지 않는다.

## 현재 상태

- `GraduationService`는 편입생을 학생별 캐시 조회 전에 `TRANSFER_STUDENT_UNSUPPORTED`로 차단한다.
- 일반 재학생 진단은 학과·입학년도별 영역 요건과 수강 과목의 영역 분류를 결합한다.
- 포털 누적 취득학점과 누적 GPA는 `StudentAcademicRecord`에 저장된다.
- 편입 인정과목도 `StudentCourse`로 저장되며 `point`가 없으면 `gainPoint`를 학점으로 사용한다.
- #337에서 지정과목 원본, 배열 순서, 중복, 최신 스냅샷 버전이 저장되도록 구현했다.
- `StudentDesignatedCourse`는 지정 의무의 원본이며 실제 수강이나 취득학점을 의미하지 않는다.
- 학생별 졸업진단 응답은 로컬 메모리에 30일간 캐시되지만 서버 간 무효화 계약은 없다.
- 현재 학적에는 편입 당시 학년, 부전공·연계전공, 졸업논문·시험 결과가 없다.

## 정책 결정

### 분석 경로

- 일반 재학생은 기존 영역별 졸업진단을 그대로 사용한다.
- 편입생은 `TransferGraduationAnalysisService`로 분기한다.
- 편입생 분기는 학생별 졸업진단 캐시 조회보다 먼저 실행한다.
- 편입생은 일반 재학생의 `getStudentAreaProgress`와 `getDualMajorAreaProgress`를 호출하지 않는다.
- 편입생 응답은 이번 이슈에서 학생별 졸업진단 캐시에 저장하지 않는다.

### 총 취득학점

- 총 취득학점의 단일 기준은 `StudentAcademicRecord.totalEarnedCredits`다.
- 현재 확정 정책에 따라 편입생의 졸업 필요 총학점은 130학점으로 계산한다.
- 남은 학점은 `max(0, 130 - totalEarnedCredits)`로 계산한다.
- 편입 인정과목 학점은 포털 누적 취득학점에 포함된 값이므로 다시 더하지 않는다.
- 누적 성적 행이 없으면 기존 `STUDENT_ACADEMIC_RECORD_NOT_FOUND` 계약을 사용한다.
- 누적 취득학점이나 GPA가 `null`이면 해당 충족 여부를 `null`로 반환하고 `ACADEMIC_SUMMARY_INCOMPLETE`를 수동 확인 사유에 추가한다.

### 편입 인정학점

- 인정학점 과목 코드는 `07045`, `07046`, `00111`, `07050`이다.
- 유효한 인정과목의 학점을 과목 코드별 한 번만 합산해 `recognizedTransferCredits`로 노출한다.
- 같은 인정 코드가 여러 번 있으면 중복 합산하지 않고 해당 코드의 최대 학점을 사용한다.
- 인정학점은 사용자에게 별도 현황으로 보여주되 총 취득학점과 영역별 학점에 다시 합산하지 않는다.

### GPA

- `StudentAcademicRecord.cumulativeGpa`를 사용한다.
- 복수전공이 없으면 최소 GPA는 2.0이다.
- `Student.secondaryMajor`가 있으면 최소 GPA는 2.5다.
- 현재 모델에 없는 부전공·연계전공 조건은 자동 판정하지 않는다.

### 지정과목

- 지정과목과 실제 이수 과목은 과목 코드의 앞뒤 공백을 제거하고 `Locale.ROOT` 기준 대문자로 변환해 비교한다.
- `F`, `R`, `NP`, `IP`와 `isRetakeDeleted=true`인 수강 기록은 이수로 인정하지 않는다.
- 하나 이상의 유효 수강 기록이 있으면 `COMPLETED`, 없으면 `NOT_COMPLETED`다.
- 과목 코드가 비어 있는 레거시 지정과목은 `UNKNOWN`이다. 신규 #337 입력은 필수 코드 검증을 통과하므로 정상 입력에서는 발생하지 않는다.
- 지정과목 응답은 `sourceOrder` 순서와 중복 행을 그대로 유지한다.
- 지정과목의 `point`는 원본 지정 정보이며 지정과목 조회만으로 취득학점에 반영하지 않는다.
- 학생의 `designatedCoursesSnapshotVersion`이 `null`이면 `designatedCoursesNeedsRefresh=true`다.
- 최신 빈 배열을 수신한 학생은 스냅샷 버전이 있고 지정과목 목록만 비어 있으므로 새로고침 필요 상태가 아니다.

### 수동 확인

편입생 MVP는 다음 사유를 구조화해 반환한다.

- `TRANSFER_ENTRY_GRADE_UNKNOWN`: 편입 당시 학년을 확인할 수 없다.
- `REGISTERED_SEMESTERS_NOT_VERIFIED`: `completedSemesters`가 규정상 등록학기와 같은지 확인되지 않았다.
- `REQUIRED_COURSES_NOT_ASSESSABLE`: 편입학년 이후 필수과목을 판정할 교육과정 데이터가 없다.
- `ELECTIVE_RATIO_NOT_ASSESSABLE`: 전공선택 50% 또는 80%의 분모와 대체과목 데이터가 없다.
- `MINOR_OR_LINKED_MAJOR_NOT_ASSESSABLE`: 부전공·연계전공 정보를 확인할 수 없다.
- `GRADUATION_REVIEW_NOT_AVAILABLE`: 졸업논문·시험·실기 결과가 없다.
- `ACADEMIC_SUMMARY_INCOMPLETE`: 누적 취득학점 또는 GPA가 비어 있다.

현재 편입생 분석에는 항상 자동 판정 불가능한 항목이 있으므로 `manualReviewRequired=true`, `analysisStatus=MANUAL_REVIEW_REQUIRED`를 반환한다. 이 상태는 졸업 불가를 의미하지 않고 최종 확인이 필요하다는 의미다.

## API 계약

기존 `GET /api/graduation/progress`를 유지하고 응답을 additive하게 확장한다.

- `analysisType`: `REGULAR` 또는 `TRANSFER`.
- `analysisStatus`: `CALCULATED` 또는 `MANUAL_REVIEW_REQUIRED`.
- `graduationProgress`: 일반 재학생은 기존 값, 편입생은 빈 배열.
- `transferProgress`: 일반 재학생은 JSON에서 생략하고 편입생만 제공.
- 기존 `languageCertFulfilled`, `languageCertNeedsRefresh`, `hasDifferentGraduationRequirement`를 유지한다.

```json
{
  "analysisType": "TRANSFER",
  "analysisStatus": "MANUAL_REVIEW_REQUIRED",
  "graduationProgress": [],
  "languageCertFulfilled": true,
  "languageCertNeedsRefresh": false,
  "hasDifferentGraduationRequirement": false,
  "transferProgress": {
    "requiredTotalCredits": 130,
    "totalEarnedCredits": 112,
    "remainingCredits": 18,
    "creditsFulfilled": false,
    "recognizedTransferCredits": 65,
    "cumulativeGpa": 3.2,
    "requiredGpa": 2.0,
    "gpaFulfilled": true,
    "completedSemesters": 3,
    "designatedCoursesNeedsRefresh": false,
    "designatedCourses": [
      {
        "courseCode": "ABC123",
        "courseName": "자료구조",
        "credits": 3,
        "status": "COMPLETED"
      }
    ],
    "manualReviewRequired": true,
    "manualReviewReasons": [
      "TRANSFER_ENTRY_GRADE_UNKNOWN",
      "REGISTERED_SEMESTERS_NOT_VERIFIED",
      "REQUIRED_COURSES_NOT_ASSESSABLE",
      "ELECTIVE_RATIO_NOT_ASSESSABLE",
      "MINOR_OR_LINKED_MAJOR_NOT_ASSESSABLE",
      "GRADUATION_REVIEW_NOT_AVAILABLE"
    ]
  }
}
```

기존 생성자는 일반 재학생 응답 생성자로 유지한다. 편입생 응답은 `GraduationProgressResponse.forTransfer(...)` 정적 팩터리로만 생성해 두 계약이 섞이지 않게 한다.

## 조회와 계산 구조

`TransferGraduationAnalysisService`는 한 요청에서 다음 조회만 수행한다.

1. 누적 성적 한 건 조회.
2. 지정과목 목록 한 번 조회.
3. 과목과 개설 정보를 fetch join한 전체 수강 기록 한 번 조회.
4. 외국어 인증 한 건 조회.

`DesignatedCourseEvaluator`는 저장소를 호출하지 않는 순수 계산 객체다. 전달받은 수강 기록에서 유효 과목 코드와 인정학점 인덱스를 한 번 만들고 지정과목 목록을 평가한다.

## 캐시 정책

- 일반 재학생의 기존 학생별 캐시는 유지한다.
- 편입생은 학생별 캐시를 읽거나 쓰지 않는다.
- 학과·입학년도별 정적 졸업요건 캐시는 일반 재학생 경로에서만 기존대로 사용한다.
- DB 버전과 다중 서버 캐시 정합성은 별도 이슈로 분리한다.

## 문서 변경

- Springdoc 스키마와 `GraduationControllerDocs`에 일반·편입생 응답 차이를 기록한다.
- 실행 중인 애플리케이션의 `/v3/api-docs`에서 신규 enum과 편입 응답 객체를 확인한다.
- 별도 Wiki 저장소의 졸업요건 및 포털 데이터 계약 문서에 부분 진단 범위와 수동 확인 사유를 반영한다.

## 제외 범위

- 편입생 최종 졸업 가능 여부 자동 판정.
- 편입 당시 학년 저장.
- 편입학년 이후 필수과목 자동 판정.
- 전공선택 50% 또는 80% 자동 판정.
- 폐지·대체과목 자동 처리.
- 부전공·연계전공 모델 추가.
- 졸업논문·시험·실기 결과 연동.
- 학과별 편입생 예외 정책 테이블.
- 범용 졸업요건 정책 엔진이나 규칙 DSL.
- 통합 데이터 버전과 다중 서버 캐시 정합성 처리.

## 남은 위험

- 130학점 예외 학과가 확인되면 현재 상수 정책을 학과별 정책 데이터로 교체해야 한다.
- 포털 누적 취득학점에 편입 인정학점이 포함된다는 계약은 실제 편입생 fixture와 통합 테스트로 보호해야 한다.
- `completedSemesters`는 화면 참고값일 뿐 등록학기 충족 판정에 사용하지 않는다.
- 학교 규정이 변경되면 공식 졸업요건 안내의 시행 시점과 코드 정책을 함께 갱신해야 한다.
