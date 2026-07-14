# Null 학점 강의 Hotfix 배포 계획

## 목표

포털의 `point`가 null이고 `gainPoint`가 있는 강의를 정상 학점으로 저장하고, 두 값이 모두 없는 경우에도 졸업진단이 500 오류 없이 동작하게 한다.

## 범위

- 스크래퍼는 병합 결과의 `point`가 null일 때 `gainPoint`로 보정한다.
- 백엔드는 수신한 `gainPoint`를 `point` 보정값으로 사용하고, 기존 null 학점 강의도 재스크래핑 시 갱신한다.
- 백엔드 졸업진단 집계는 null 학점을 0으로 처리한다.
- DB 스키마와 공개 API 계약은 변경하지 않는다.

## 브랜치와 배포 순서

1. 스크래퍼 `main`을 `develop`에 fast-forward하여 기준 커밋을 맞춘다.
2. 스크래퍼 `feat/301`의 병합·테스트 후 `develop`을 개발 환경에 배포한다.
3. 스크래퍼 `develop`을 `main`에 fast-forward하고 운영 환경에 배포한다.
4. 백엔드 `main`의 변경을 `dev`에 병합하고 `feat/301`을 최신 `dev`에 rebase한다.
5. 백엔드 `feat/301`을 `dev`에 병합한 뒤 개발 환경에 배포한다.
6. 백엔드 `dev`를 `main`에 fast-forward하고 운영 환경에 배포한다.

## 검증과 롤백

- 스크래퍼는 회귀 테스트와 TypeScript 빌드를 실행한다.
- 백엔드는 `./gradlew test`를 실행한다.
- 각 GitHub Actions 배포 완료 후 대상 브랜치의 배포 SHA와 원격 SHA를 확인한다.
- 개발과 운영 브랜치는 최종적으로 같은 SHA인지 확인한다.
- 배포 실패 시 해당 환경의 워크플로를 중단하고, 이전 검증 SHA로 되돌리는 별도 hotfix를 만든다.
