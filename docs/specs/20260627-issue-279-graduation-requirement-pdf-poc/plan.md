# Graduation Requirement PDF PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use test-driven-development before production code.

**Goal:** dev admin API가 PDF 추출 리소스를 기준으로 학생의 누락 졸업요건을 dry-run 또는 실제 생성한다.

**Architecture:** 2020~2026 PDF main table을 정규화 JSON 리소스로 저장한다. API는 `studentCode`로 학생을 찾고, 학과/전공명을 리소스의 `matchNames`와 비교한 뒤 누락 row만 `department_area_requirements`와 `dual_major_requirements`에 저장한다.

**Tech Stack:** Java 17, Spring MVC, Spring Data JPA, Jackson resource loading, pdfplumber extraction, JUnit 5.

## Global Constraints

- 이슈 279의 `feat/279` 브랜치에서 이어서 작업한다.
- dev/test profile의 `/api/admin` API로만 노출한다.
- PDF 원본은 DB에 넣지 않고 정규화 JSON 리소스로 저장한다.
- `전필`은 `전핵`으로 매핑한다.
- 복수전공 요구 학점은 `dual_major_requirements`에 `복교`, `복핵`, `복선`으로 저장한다.
- 단일전공의 일반선택 전공심화 컬럼은 `일선`으로 저장한다.
- `graduation_requirements` 테이블은 현재 진행도 계산 흐름에서 쓰지 않으므로 PoC 저장 대상에서 제외한다.
- production 배포와 운영 DB 직접 반영은 제외한다.

## Tasks

- Task 1. PDF에서 2020~2026 정규화 JSON 리소스를 생성한다.
- Task 2. 저장 대상 엔티티 생성 메서드와 repository를 TDD로 추가한다.
- Task 3. JSON 리소스 조회 서비스를 TDD로 추가한다.
- Task 4. studentCode 기반 dry-run/생성 서비스를 TDD로 추가한다.
- Task 5. admin controller와 OpenAPI를 연결한다.
- Task 6. 관련 테스트와 전체 `./gradlew test`를 실행한다.
