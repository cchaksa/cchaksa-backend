# Clarifications

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 없음. 구현 착수에 필요한 정책은 현재 모두 확정됨 | requester / codex | Closed |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 현재 기능은 현재 수료 학기 1개만 대상으로 한다 | 과거 학기 평가는 추후 별도 엔트리포인트로 분리하기 위해 | 2026-06-20 |
| 2 | `IP -> completed grade`가 하나라도 있으면 평가 필요 상태로 표시한다 | 모든 과목 변경을 기다리면 누락 가능성이 있고 비용도 커지기 때문에 | 2026-06-20 |
| 3 | 제출은 학기 단위 일괄 제출만 허용한다 | FE가 모든 과목 평가 완료 후 한 번에 전송하는 정책과 맞추기 위해 | 2026-06-20 |
| 4 | `course_evaluations`와 `course_evaluation_tags`를 분리한다 | review text 중복 저장을 막고 태그 확장성을 확보하기 위해 | 2026-06-20 |
| 5 | `offering_id`는 평가 저장 테이블에 저장하지 않는다 | 장기 조회 목적이 개설 분반이 아니라 `course_id + professor_id` 누적 평가이기 때문에 | 2026-06-20 |
| 6 | 중복 제출은 `(student_id, year, semester, course_id, professor_id)` unique constraint로 막는다 | 재수강 가능성을 고려하면 `student_id + offering_id` 기준은 부적절하기 때문에 | 2026-06-20 |
| 7 | 조회 API 점수 필드는 `score` 하나만 사용한다 | `originalScore`와 중복 응답을 피하기 위해 | 2026-06-20 |
| 8 | `score`는 `student_courses.original_score`를 nullable `Integer`로 내려준다 | 0 대체는 실제 0점과 미수집 상태를 혼동시킬 수 있기 때문에 | 2026-06-20 |
| 9 | POST 성공 응답은 기존 코드 스타일에 맞춰 `MessageOnlyResponse`를 사용한다 | 기존 controller 응답 패턴과 일관성을 유지하기 위해 | 2026-06-20 |
| 10 | 신규 기능은 `lectureevaluations` 패키지에서 구현한다 | 기존 도메인과 분리된 기능 경계를 명확히 하기 위해 | 2026-06-20 |
| 11 | 메인 노출 대상 학기는 Spring 설정값 `lecture-evaluation.target-year/target-semester`로 운영한다 | DB에 노출 대상 상태를 추가하지 않으면서도 과거 학기 pending row와 메인 진입점을 분리하기 위해 | 2026-06-20 |
| 12 | `target-semester`는 DB 저장 semester code와 동일한 값을 사용한다 | 현재 포털 payload는 `2024-10`, `cretSmrCd=10`처럼 학기 코드를 저장하므로 표시값 1/2와 다를 수 있기 때문에 | 2026-06-20 |
| 13 | 과제 관련 태그는 `HOMEWORK` 용어로 통일한다 | FE/팀 공유 시 `ASSIGNMENT`보다 직관적이고 문구와 의미가 맞기 때문에 | 2026-06-20 |

## Risks / Unknowns
- Item:
  - Impact:
    - `target-semester`에 화면 표시값 1/2를 넣으면 DB의 10/20 코드와 매칭되지 않을 수 있다.
  - Mitigation:
    - 운영 설정에는 `course_offerings.semester`, `semester_academic_records.semester`에 실제 저장되는 값을 넣는다.
- Item:
  - Impact:
    - 현재 `StudentCourseDto.CourseDetailDto.score`는 실제 점수가 아니라 `points`를 매핑한다.
  - Mitigation:
    - 강의평가 조회는 전용 DTO를 사용하고 `score = originalScore`로 매핑한다.
- Item:
  - Impact:
    - `course_id + professor_id`만으로 제출 검증하면 현재 학기 수강 여부를 놓칠 수 있다.
  - Mitigation:
    - 제출 검증 시 `student_id`, `year`, `semester`, `grade != IP` 조건으로 서버 평가 대상 set을 먼저 계산한다.

## Follow-ups
- [ ] 구현 후 테스트 실행 결과를 `tasks.md`에 기록한다.
- [ ] OpenAPI 변경이 `OpenApiResponseContractTest`와 충돌하지 않는지 확인한다.
