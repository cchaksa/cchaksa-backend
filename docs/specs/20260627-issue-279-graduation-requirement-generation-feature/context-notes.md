# Context Notes

- PoC baseline은 `/private/tmp/haksa-feat-279-func`의 `feat/279`에서 `./gradlew test` 통과 상태로 시작한다.
- 기존 PoC는 PDF JSON 리소스, 생성 서비스, controller, OpenAPI, 인증 테스트를 이미 포함한다.
- 기능화 작업은 PDF 데이터를 DB에 넣거나 다시 추출하지 않는다.
- 실제 호출자가 오해할 수 있는 응답 의미와 누락 학과 정보 방어를 우선 보강한다.
- `dryRun=false`는 저장이 수행된 요청이므로 응답의 생성 대상 상태가 호출 후 상태인지 호출 전 상태인지 명확해야 한다.
- `alreadyExists`는 요청 처리 전 존재 여부로 유지하고, `created`를 추가해 이번 요청으로 생성된 row인지 구분한다.
- 관련 테스트 묶음과 전체 `./gradlew test`가 통과했다.
