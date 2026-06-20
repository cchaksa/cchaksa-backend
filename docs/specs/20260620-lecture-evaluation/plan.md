# Issue 245 Plan

## Architecture / Layering
- Domain impact:
  - `domain/lectureevaluations` 신규 패키지를 만들고 controller, docs, dto, model, repository, service를 둔다.
  - `SemesterAcademicRecord`에 강의평가 필요/완료 상태와 상태 변경 메서드를 추가한다.
  - `SyncAcademicRecordService`의 기존 수강 기록 갱신 지점에서 `IP -> completed grade` 변경을 감지한다.
- Application orchestration:
  - 별도 application layer는 추가하지 않고 controller -> `LectureEvaluationService` -> repositories 경로로 처리한다.
  - 포털 동기화는 기존 application service에서 semester record 상태만 갱신한다.
- Infrastructure touchpoints:
  - Flyway `V5__create_lecture_evaluation_tables.sql` 추가
  - `StudentCourseRepository`에 강의평가 대상 과목 fetch query 추가
  - `SemesterAcademicRecordRepository`에 상태 변경 대상 조회 재사용
- Global/config changes:
  - 강의평가 제출 불가/과목 불일치용 ErrorCode 추가
  - `LectureEvaluationProperties`를 추가해 `lecture-evaluation.target-year`, `lecture-evaluation.target-semester`를 읽는다.
  - `application.yml`에 기본 target 학기 설정을 둔다.
  - `src/main/resources/public/openapi.yaml`에 신규 API schema 추가

## Data / Transactions
- Tables changed:
  - `semester_academic_records`
  - `course_evaluations`
  - `course_evaluation_tags`
- Repositories touched:
  - `SemesterAcademicRecordRepository`
  - `StudentCourseRepository`
  - 신규 `CourseEvaluationRepository`
  - 신규 `CourseEvaluationTagRepository`
- Transaction scope:
  - 조회 API는 read-only transaction
  - 제출 API는 평가 row 저장, tag row 저장, semester 상태 갱신을 하나의 transaction으로 처리
  - 포털 동기화는 기존 sync transaction 안에서 required 상태를 갱신
- Consistency expectations:
  - POST 성공 후 같은 학기 재제출은 service check와 DB unique constraint 양쪽에서 차단한다.

## Data Model
- `course_evaluations`
  - `id BIGINT IDENTITY PRIMARY KEY`
  - `student_id UUID NOT NULL`
  - `course_id BIGINT NOT NULL`
  - `professor_id BIGINT NOT NULL`
  - `year INTEGER NOT NULL`
  - `semester INTEGER NOT NULL`
  - `review VARCHAR(2000) NULL`
  - `created_at TIMESTAMP WITH TIME ZONE NULL`
  - `updated_at TIMESTAMP WITH TIME ZONE NULL`
  - unique: `(student_id, year, semester, course_id, professor_id)`
  - index: `(course_id, professor_id)`
- `course_evaluation_tags`
  - `id BIGINT IDENTITY PRIMARY KEY`
  - `course_evaluation_id BIGINT NOT NULL`
  - `tag VARCHAR(64) NOT NULL`
  - unique: `(course_evaluation_id, tag)`
  - index: `(course_evaluation_id)`

## API Contracts
- `GET /api/lecture-evaluations/required`
  - Returns:
    - `lectureEvaluationRequired`
    - `year`
    - `semester`
    - `grades`
  - `grades.score` is nullable and maps from `student_courses.original_score`.
  - Uses configured target year/semester. If target semester record is absent or not pending, returns `lectureEvaluationRequired=false` and `grades=[]`.
- `POST /api/lecture-evaluations`
  - Body:
    - `year`
    - `semester`
    - `evaluations[].courseId`
    - `evaluations[].professorId`
    - `evaluations[].selectedTags`
    - `evaluations[].review`
  - Returns:
    - `MessageOnlyResponse`

## Testing Strategy
- Domain/model tests:
  - `SemesterAcademicRecord` 상태 변경 메서드
  - `CourseEvaluationTag` 중복 제거 또는 service tag set 처리
- Service tests:
  - 평가 필요 조회 true/false
  - `score` null 유지
  - `IP` 과목 제외
  - 제출 성공 시 평가 저장과 completed 상태 갱신
  - 누락/중복/미수강 과목 제출 실패
- Sync tests:
  - 현재 수료 학기 `IP -> completed grade` 변경 시 required true
  - completed 상태 학기는 required 재설정하지 않음
- Migration/OpenAPI tests:
  - `./gradlew test --tests 'com.chukchuk.haksa.global.db.FlywayMigrationTest'`
  - `./gradlew test --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'`
- Final command:
  - `./gradlew test`

## Rollout Considerations
- Backward compatibility:
  - 기존 `/api/academic/record` 응답은 변경하지 않는다.
  - 강의평가 조회는 전용 DTO를 사용한다.
- Observability / metrics:
  - 신규 API에는 slow log 기준의 비즈니스 로그를 추가한다.
- Feature flags / toggles:
  - 없음.
- Rollback:
  - 기능 롤백 시 신규 API 사용을 중단하고, DB는 추가 컬럼/테이블만 남아도 기존 기능에 영향이 없도록 설계한다.
