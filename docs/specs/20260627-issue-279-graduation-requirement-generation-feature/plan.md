# Graduation Requirement Generation Feature Plan

**Goal:** PoC로 만든 PDF 기반 졸업요건 생성 API를 dev에서 실제로 반복 사용할 수 있는 기능으로 다듬는다.

**Architecture:** PDF 추출 JSON 리소스와 기존 생성 서비스는 유지한다. 학생, 학과, 템플릿, 중복 row 상태를 더 명확히 검증하고, dry-run/apply 응답이 프론트와 운영자가 그대로 판단할 수 있는 계약이 되도록 보강한다.

**Verification:** 변경마다 실패 테스트를 먼저 확인하고, 관련 service/controller/security/openapi 테스트와 전체 `./gradlew test`를 통과시킨다.

## Scope

- dev/test profile의 인증된 admin API만 대상으로 한다.
- PDF 원본 재추출과 DB schema 변경은 제외한다.
- production 배포와 운영 DB 직접 반영은 제외한다.
- 템플릿 매칭 기준이나 저장 정책을 바꿔야 하면 작업을 멈추고 질문한다.

## Tasks

- Task 1. 현재 PoC 구현과 테스트의 기능화 gap을 고정한다.
- Task 2. 학생의 주전공/소속 학과 정보가 없는 경우를 테스트로 방어한다.
- Task 3. apply 응답에서 생성 후 상태가 오해되지 않도록 테스트와 응답 계약을 보강한다.
- Task 4. controller/OpenAPI 문서를 기능 계약에 맞춰 정리한다.
- Task 5. 관련 테스트와 전체 테스트를 실행하고 의미 단위로 커밋한다.
