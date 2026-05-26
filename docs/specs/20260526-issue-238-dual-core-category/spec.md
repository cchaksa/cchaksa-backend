# Issue 238 Spec

## Goal
- `/api/academic/record` 과목 분류에서 복수전공 핵심 과목(`복핵`)을 전공 목록으로 반환한다.
- 복수전공 교양 과목(`복교`)은 기존처럼 교양 목록으로 반환한다.

## Scope
- In scope: `AcademicRecordService`의 전공/교양 분류 기준 수정.
- In scope: `복핵`, `복교` 분류 단위 테스트 추가.
- Out of scope: API 응답 구조 변경, 졸업요건 분석 로직 변경, DB 스키마 변경.

## Acceptance Criteria
- `FacultyDivision.복핵` 과목은 `courses.major`에 포함된다.
- `FacultyDivision.복교` 과목은 `courses.liberal`에 포함된다.
- 기존 `전핵`, `전선`, `복선` 전공 분류는 유지된다.
