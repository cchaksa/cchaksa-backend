# 포털 REFRESH 학생 소유권 충돌 방지 설계

## 연결 이슈

- GitHub Issue [#313](https://github.com/cchaksa/cchaksa-backend/issues/313)
- Sentry `JAVA-SPRING-BOOT-58`, `JAVA-SPRING-BOOT-57`

## 확인된 원인

최신 두 Sentry 이벤트는 같은 `REFRESH` job의 첫 번째 콜백이다. 두 번째 학생을 INSERT한 것이 아니라, `UserService.tryMergeWithExistingUser`가 다른 사용자의 학생을 현재 사용자로 옮기는 UPDATE를 실행하다가 기존 `students.user_id` 유니크 제약과 충돌했다.

`REFRESH`는 이미 포털 연결된 사용자의 기존 학생을 갱신하는 흐름이다. 이 단계에서 payload의 `student_code`로 다른 사용자를 병합하면 현재 사용자가 가진 학생과 병합 대상 학생이 동시에 같은 사용자를 가리키게 된다.

## 처리 정책

- 사용자당 학생 한 명이라는 기존 `students.user_id` 제약을 유지한다.
- 기존 계정 복구를 위한 사용자 병합은 최초 `LINK` 흐름에서만 수행한다.
- `REFRESH`는 job에 기록된 현재 사용자와 현재 학생만 갱신한다.
- payload 학번이 현재 학생 학번과 다르면 학생 정보나 학업 이력을 쓰기 전에 실패한다.
- 학번 불일치 시 기존 학생을 삭제하거나 다른 학생으로 자동 교체하지 않는다.
- 동일 callback의 재처리와 동시 처리는 기존 scrape job 행 잠금과 완료 상태 검사에 맡긴다.

## 변경 범위

- `PortalSyncService.refreshFromPortal`에서 사용자 병합을 제거한다.
- `RefreshPortalConnectionService`에서 현재 학생과 payload 학번의 일치 여부를 검증한다.
- REFRESH 회귀 테스트와 학번 불일치 방어 테스트를 추가한다.

DB 스키마와 공개 API 계약은 변경하지 않는다.

## 검증

- 같은 학번의 REFRESH가 기존 학생을 갱신한다.
- REFRESH는 사용자 병합을 호출하지 않는다.
- 다른 학번의 REFRESH는 저장소를 호출하기 전에 실패한다.
- 기존 최초 LINK와 LINK 사용자 병합 테스트가 유지된다.
- `./gradlew test --stacktrace --no-daemon`이 통과한다.

## 운영 확인

배포 후 `JAVA-SPRING-BOOT-58`, `JAVA-SPRING-BOOT-57` 재발 여부와 REFRESH 실패 job을 확인한 뒤 Sentry resolve 여부를 결정한다.
