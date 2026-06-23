# Spec Lite

## Problem

선교 영역 진행도에서 `completedElectiveCourses`가 세부 교양 영역 수가 아니라 수강 과목 offering 수로 계산된다. 같은 `liberalAreaCode` 과목을 여러 개 들어도 UI에서는 하나의 영역으로 보여야 한다.

## Scope

- `선교` 영역의 `completedElectiveCourses`만 `liberalAreaCode` 고유 개수 기준으로 계산한다.
- `liberalAreaCode`가 `null`인 선교 과목은 카운트에서 제외하되, 과목 목록 응답에는 유지한다.
- 선교가 아닌 영역은 기존 `offeringId` 고유 개수 계산을 유지한다.
- 요건 `areaType`에 앞뒤 공백이 있어도 계산 기준이 달라지지 않게 한다.

## Verification

- 선교 중복 세부영역과 null 세부영역을 포함한 repository 단위 테스트를 추가한다.
- focused Gradle test를 실행한다.
