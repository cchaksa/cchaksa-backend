# Checklist

- [x] `flangPassGb` 한글 값 매퍼 테스트 추가
- [x] 알 수 없는 `flangPassGb` 콜백 실패 확정 테스트 추가
- [x] RED 테스트 실행 및 실패 원인 확인
- [x] `통과`/`미통과` 매핑 구현
- [x] 매퍼 검증 실패를 schema invalid로 변환
- [x] 관련 테스트 실행
- [x] 전체 테스트 실행
- [x] PR 리뷰 반영 테스트 추가 및 RED 확인
- [x] 매핑 중 RuntimeException 실패 확정 반영
- [x] `Y`/`N` 매핑 제거
- [x] 리뷰 반영 후 관련 테스트 실행
- [x] 리뷰 반영 후 전체 테스트 실행

## Verification Log

| Command | Result | Date |
|---------|--------|------|
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*PortalDataMapperTests" --tests "*ScrapeResultCallbackServiceUnitTests"` | Fail as RED: Korean `flangPassGb` throws `IllegalArgumentException`; unknown value is not converted to schema invalid | 2026-06-02 |
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*PortalDataMapperTests" --tests "*PortalCallbackPostProcessorTests" --tests "*ScrapeResultCallbackServiceUnitTests"` | Pass | 2026-06-02 |
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test` | Pass | 2026-06-02 |
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*PortalDataMapperTests" --tests "*ScrapeResultCallbackServiceUnitTests"` | Fail as RED: `Y`/`N` still accepted; mapper `RuntimeException` not converted to schema invalid | 2026-06-02 |
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*PortalDataMapperTests" --tests "*PortalCallbackPostProcessorTests" --tests "*ScrapeResultCallbackServiceUnitTests"` | Pass | 2026-06-02 |
| `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test` | Pass | 2026-06-02 |
