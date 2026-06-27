# Graduation Requirement Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use test-driven-development before production code.

**Goal:** dev admin API에서 학번 기준 졸업요건 누락 원인을 진단한다.

**Architecture:** 기존 `/api/admin` dev 전용 컨트롤러에 학생별 진단 조회를 추가한다. 실제 졸업 진행도 조회와 같은 학과 후보 탐색 규칙과 졸업요건 repository 조회를 사용한다.

**Tech Stack:** Java 17, Spring MVC, Spring Security, JPA repository, springdoc OpenAPI, JUnit 5.

## Global Constraints

- 브랜치와 커밋 메시지는 이슈 279 기준으로 작성한다.
- 커밋은 가능한 한 30줄 내외의 의미 있는 단위로 쪼갠다.
- `전필`은 `전핵`으로 본다.
- 복수전공 요구 학점은 `dual_major_requirements`에 `복교`, `복핵`, `복선`으로 저장하는 전제를 따른다.
- 학과/전공명은 `departments`의 name 계열 필드와 매칭한다.
- 학생별 진단 API는 학생 데이터를 다루므로 인증 필요 endpoint로 둔다.
- 운영 DB 반영, PDF 자동 파싱 저장, production 배포는 이번 단위에서 제외한다.

## Tasks

- Task 1. 문서 기준 확정 사항을 기록하고 체크리스트를 만든다.
- Task 2. 진단 DTO와 controller 계약 테스트를 먼저 추가한다.
- Task 3. 진단 서비스를 테스트 먼저 추가하고 최소 구현한다.
- Task 4. controller/docs/OpenAPI를 연결하고 관련 테스트를 통과시킨다.
- Task 5. 최종 관련 테스트와 `git diff --check`를 실행한다.
