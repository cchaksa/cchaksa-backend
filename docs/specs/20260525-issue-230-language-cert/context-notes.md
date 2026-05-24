# Issue 230 Context Notes

- 2026-05-25: 사용자 확인으로 `flangPassGb`는 `Y/N` 계약으로 고정했다.
- 2026-05-25: 기존 사용자는 값이 없을 수 있으므로 `null`과 `languageCertNeedsRefresh`로 미확인 상태를 표시한다.
- 2026-05-25: 백그라운드 backfill은 범위에서 제외하고 사용자 포털 새로고침으로 갱신한다.
- 2026-05-25: raw payload는 기존 `student`와 사용자 언급의 `studentInfo`를 모두 받을 수 있도록 `@JsonAlias("studentInfo")`를 사용했다.
- 2026-05-25: 테스트 실행은 Java 17로 Gradle을 실행해야 통과한다. 기본 Java 24에서는 Gradle test task 구성 오류가 발생한다.
- 2026-05-25: 정적 Swagger 문서 `src/main/resources/public/openapi.yaml`에 졸업 요건 조회 endpoint와 외국어 인증 응답 필드를 추가했다.
- 2026-05-25: `checked_at`은 이번 기능 요구사항에 필요 없고 의미가 애매해 외국어 인증 동기화에서 변경하지 않도록 수정했다.
