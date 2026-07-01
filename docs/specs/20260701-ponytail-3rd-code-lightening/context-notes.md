# Context Notes

## 2026-07-01

- 부모 이슈는 #289이고, 새 하위 이슈는 #295다.
- `feat/295` worktree는 `/private/tmp/haksa-feat-295`에 만들었다.
- 기준선 `./gradlew test`는 통과했다.
- OpenAPI 문서 계층은 프론트가 `/v3/api-docs`를 사용하므로 계약 테스트를 유지하는 범위에서만 줄인다.
- 먼저 동작 영향이 낮은 미사용 코드와 local-only 설정부터 줄인다.
- `StudentAcademicRecordService`의 2-인자 졸업 학점 helper는 호출 지점이 없어 제거했다.
- `CourseEnrollment`는 `@Getter`가 생성하는 accessor를 직접 다시 선언하고 있었고, 미사용 grade helper도 함께 제거했다.
- `cache.type`, `portal.credential.store`는 남은 구현이 local 하나뿐이므로 conditional bean 설정과 yml 키를 제거했다.
- stale TODO 주석은 코드 동작을 바꾸지 않고 제거했다.
- OpenAPI docs interface와 `*ApiResponse` wrapper는 이번 PR에서 제거하지 않는다. 현재 계약 테스트가 전용 wrapper schema 이름을 검증하고 있고, 프론트가 `/v3/api-docs`를 사용하므로 단순 삭제가 아니라 schema 이름 보존 customizer나 프론트 계약 변경이 필요하다. 이번 #295의 목표는 낮은 위험의 삭제이므로 여기서는 보류한다.
- 관련 테스트와 전체 `./gradlew test`가 통과했다.
