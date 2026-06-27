# Graduation Requirement Review Comments Plan

**Goal:** PR #285 리뷰에서 지적된 졸업요건 API 안정성 문제를 반영한다.

**Scope:** 템플릿 캐싱, 리소스 정합성 로그, 진단/생성 API의 입력 데이터 방어, 생성 후 DB 기반 캐시 갱신, DTO 검증을 처리한다.

**Out of Scope:** `studentCode` trim 공통 유틸 분리는 현재 중복 규모가 작아 이번 리뷰 대응에서 제외한다.

## Tasks

- Task 1. 진단 API가 입학년도 또는 주전공/학과 누락 시 명시적으로 실패하도록 TDD로 보강한다.
- Task 2. PDF 템플릿 리소스 로딩을 연도별 메모리 캐시로 변경하고 정합성 오류 로그를 남긴다.
- Task 3. 템플릿 누락 예외에 학과명과 입학년도 로그를 남긴다.
- Task 4. 생성 후 캐시를 템플릿이 아니라 DB 최종 조회 결과로 갱신한다.
- Task 5. `CreateMissingGraduationRequirementsRequest.studentCode`에 선언적 검증을 추가한다.
- Task 6. 관련 테스트와 전체 `./gradlew test`를 통과시키고 push한다.
