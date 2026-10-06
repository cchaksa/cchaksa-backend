# 복수전공 졸업요건 및 탈퇴 계정 재가입 Hotfix 설계

## 연결 정보

- GitHub Issue [#333](https://github.com/cchaksa/cchaksa-backend/issues/333)
- 작업 브랜치 `hotfix/333`
- 기준 및 최초 병합 대상 브랜치 `main`

## 문제 1. 복수전공 졸업요건 후보 오선택

학과 개편으로 학생의 현재 주전공 ID와 입학 연도별 졸업요건이 저장된 과거 학과 ID가 다를 수 있다. `GraduationMajorResolver`는 동일한 `established_department_name`을 가진 학과를 후보로 탐색하지만, 복수전공에서는 주전공 영역 요건을 확인하지 않고 복수전공 요건 조회 결과가 비어 있지 않은 첫 조합을 선택한다.

복수전공 요건 조회는 후보 주전공의 `PRIMARY` 요건과 후보 복수전공의 `SECONDARY` 요건을 OR 조건으로 조회한다. 따라서 후보 주전공에 영역 요건이 없어도 복수전공의 `SECONDARY` 요건만 존재하면 그 조합이 선택된다. 이후 진행률 조회가 선택된 주전공의 영역 요건을 찾지 못해 `G02 GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`를 반환한다.

운영 재현 사례에서는 현재 주전공 후보에는 영역 요건이 없고 동일 학과의 과거 후보에는 영역 요건이 있었다. 기존 로직은 현재 후보를 선택해 404를 반환했지만, 과거 후보를 선택하면 정상 계산할 수 있었다.

## 문제 2. 레거시 탈퇴 계정 재가입 시 탈퇴 상태 전파

현재 탈퇴 처리에서는 학생 학번을 익명화한다. 그러나 익명화 정책 적용 전에 생성된 일부 레거시 탈퇴 데이터에는 원문 학번이 남아 있을 수 있다.

이 상태에서 사용자가 재가입하고 포털을 연동하면 `UserService.tryMergeWithExistingUser`가 같은 학번의 탈퇴 계정을 정상 병합 대상으로 찾는다. `User.absorbFrom`은 기존 계정의 `isDeleted`와 `deletedAt`까지 신규 계정으로 복사하므로 활성 신규 계정이 다시 탈퇴 상태가 된다.

카카오 로그인 API는 공개 경로이므로 토큰 발급 자체는 성공할 수 있다. 하지만 이후 보호 API에서 `JwtAuthenticationFilter`가 `UserDetails.isEnabled()`를 검사하고 탈퇴 사용자를 401로 차단한다. 그 결과 메인 화면에는 진입하지만 후속 데이터 요청은 모두 실패할 수 있다.

소셜 계정 연결이 탈퇴 사용자에게 남아 있는 레거시 상태에서도 같은 문제가 생긴다. 현재 `findOrCreateUser`는 기존 소셜 계정의 사용자가 탈퇴 상태인지 확인하지 않고 그대로 반환하므로 탈퇴 사용자용 토큰을 다시 발급할 수 있다.

## 목표

- 복수전공 Resolver가 실제 진행률을 계산할 수 있는 주전공 후보만 선택한다.
- 레거시 탈퇴 계정은 활성 계정에 병합하지 않고 현재 탈퇴 정책에 맞게 정리한다.
- 활성 신규 계정에 `isDeleted`와 `deletedAt`이 전파되지 않도록 도메인 수준에서도 방어한다.
- 탈퇴 사용자에 연결된 레거시 소셜 계정으로 로그인하면 기존 탈퇴 계정을 재활성화하지 않고 신규 활성 계정을 생성한다.
- 공개 API, 오류 코드, DB 스키마와 정상 사용자의 기존 병합 동작은 유지한다.

## 비목표

- 탈퇴 계정을 복구하거나 과거 활동 데이터를 신규 계정으로 이관하지 않는다.
- 졸업요건 기준 데이터를 다른 학과 ID로 복제하지 않는다.
- 이번 Hotfix에서 DB 제약 조건이나 Flyway migration을 추가하지 않는다.
- 운영 레거시 데이터 전체를 자동 일괄 수정하지 않는다. 조회로 대상을 분류한 뒤 검증된 건만 보정한다.

## 설계 1. 복수전공 주전공 후보 선검증

`GraduationMajorResolver.resolveDualMajor`의 바깥 반복문에서 기존 `hasSingleMajorRequirement`를 호출한다. 영역 요건이 없는 주전공 후보는 복수전공 조합 조회 전에 건너뛴다.

```java
for (Long primaryId : primaryCandidates) {
    if (primaryId == null) continue;
    if (!hasSingleMajorRequirement(primaryId, admissionYear)) continue;

    for (Long secondaryId : secondaryCandidates) {
        if (secondaryId == null) continue;

        if (hasDualMajorRequirement(primaryId, secondaryId, admissionYear)) {
            return new MajorResolutionResult(primaryId, secondaryId);
        }
    }
}
```

검사는 바깥 반복문에서 한 번만 실행한다. 단일전공 처리, 후보 순서, 모든 후보가 실패했을 때의 G02 응답과 MDC 기록은 유지한다.

## 설계 2. 탈퇴 계정은 병합 대신 레거시 정리

`UserService.tryMergeWithExistingUser`가 동일 학번 사용자를 찾은 뒤 자기 자신인지 확인하고, 기존 사용자가 탈퇴 상태라면 정상 병합을 수행하지 않는다.

대신 private helper인 `cleanupLegacyWithdrawnUser`에서 다음 처리를 수행하고 현재 활성 사용자를 그대로 반환한다.

1. 기존 Student를 현재 정책과 같은 방식으로 익명화한다.
2. 기존 User에 연결된 SocialAccount를 삭제한다.
3. 기존 User의 RefreshToken과 인증 캐시를 제거한다.
4. 변경 내용을 flush해 원문 학번과 소셜 로그인 고유키를 신규 계정이 안전하게 사용할 수 있게 한다.
5. 기존 탈퇴 User와 익명화된 Student는 보존한다.

Student는 수강평 등 다른 데이터가 참조할 수 있으므로 삭제하지 않는다. 기존 탈퇴 계정의 프로필, 학생 정보, 탈퇴 상태는 신규 계정으로 이관하지 않는다.

학생 학번에는 UNIQUE 제약이 있으므로 익명화 UPDATE가 신규 Student INSERT보다 먼저 DB에 반영되어야 한다. `StudentDeletionService.anonymizeByStudent`는 `saveAndFlush`를 사용해 학번을 즉시 해제한다. 소셜 계정 삭제 후에도 동일 provider/social ID 재등록 전에 persistence context를 flush한다.

## 설계 3. `User.absorbFrom`의 생명주기 상태 방어

정상 계정 병합은 기존 프로필과 Student 이관 동작을 유지한다. 다만 `isDeleted`와 `deletedAt`은 계정 생명주기 상태이므로 `absorbFrom`에서 복사하지 않는다.

서비스 분기에서 탈퇴 계정 병합을 차단하더라도, 다른 호출부가 실수로 탈퇴 사용자를 전달했을 때 활성 대상 사용자가 탈퇴 상태가 되지 않도록 도메인 불변식을 보강한다.

## 설계 4. 탈퇴 사용자에 연결된 소셜 계정 로그인 방어

`findOrCreateUser`를 명시적 분기로 변경한다.

- 기존 SocialAccount의 User가 활성 상태면 현재처럼 그 User를 반환한다.
- 기존 SocialAccount의 User가 탈퇴 상태면 `cleanupLegacyWithdrawnUser`를 실행한 뒤 새 User와 SocialAccount를 생성한다.
- SocialAccount가 없으면 현재처럼 새 User와 SocialAccount를 생성한다.

신규 생성 코드는 `createUserWithSocialAccount`로 추출해 두 생성 경로에서 공유한다. 로그인 성공 응답 계약은 변경하지 않는다.

## 트랜잭션과 실패 처리

병합과 로그인 경로는 기존 트랜잭션 안에서 처리한다. 익명화, 연결 삭제, 신규 생성 중 하나라도 실패하면 전체 트랜잭션을 rollback해 중간 상태를 남기지 않는다.

레거시 정리 후 신규 Student 저장에서 동일 학번 UNIQUE 충돌이 발생하지 않는지 통합 테스트로 검증한다. 같은 provider/social ID의 신규 SocialAccount 생성도 삭제 flush 이후 성공하는지 단위 테스트에서 확인한다.

## 테스트 정책

### 졸업요건

- 첫 주전공 후보에는 영역 요건이 없고 과거 동일 학과 후보에는 영역 요건이 있는 상황을 재현한다.
- 수정 후 첫 후보를 건너뛰고 계산 가능한 과거 후보 조합을 반환하는지 확인한다.
- 무효한 첫 후보에는 복수전공 요건 조회를 수행하지 않는지 확인한다.
- 기존 단일전공, G02와 MDC 테스트가 모두 통과하는지 확인한다.

### 사용자 병합과 로그인

- 동일 학번의 활성 기존 사용자는 현재처럼 정상 병합되는지 확인한다.
- 동일 학번의 탈퇴 기존 사용자는 병합하지 않고 익명화와 인증 연결 정리를 수행하는지 확인한다.
- `User.absorbFrom`에 탈퇴 사용자를 전달해도 활성 대상의 탈퇴 상태가 바뀌지 않는지 확인한다.
- 탈퇴 사용자에 연결된 SocialAccount로 로그인하면 신규 활성 User와 SocialAccount를 생성하는지 확인한다.
- 정리 직후 신규 Student가 같은 원문 학번을 사용할 수 있고 기존 Student는 익명화 상태로 남는지 통합 테스트한다.
- 기존 `JwtAuthenticationFilterTests`의 탈퇴 사용자 토큰 401 동작은 유지한다.

## 운영 데이터 조치

운영에서는 `is_deleted=true`이면서 연결 Student의 학번이 익명화되지 않은 계정을 조회 전용 쿼리로 찾는다.

- `portal_connected=false`인 계정은 병합되지 않은 레거시 탈퇴 계정 후보로 분류하고 현재 정책에 맞게 익명화한다.
- `portal_connected=true`인 계정은 재가입 병합으로 활성 계정에 탈퇴 상태가 전파된 후보로 본다. 가입·연동 이력을 수동 확인한 뒤 신규 계정임이 확인된 건만 `is_deleted=false`, `deleted_at=NULL`로 복구한다.
- 복구 후 기존 인증 캐시와 토큰 영향을 제거하기 위해 재로그인을 안내한다.

장애 확인을 위해 임시 변경한 학생의 주전공은 Hotfix 운영 배포 후 실제 학적 값으로 복원한다. 저장소 문서, 커밋과 PR에는 운영 학생의 학번이나 UUID를 기록하지 않는다.

## 변경 범위

- `GraduationMajorResolver`와 회귀 테스트.
- `UserService`의 병합 및 로그인 분기와 단위 테스트.
- `User.absorbFrom`과 도메인 테스트.
- `StudentDeletionService`의 익명화 flush 처리.
- 재가입 및 동일 학번 재사용 통합 테스트.
- 관련 Wiki의 탈퇴·재가입 정책 문서.

공개 API, DTO, DB 스키마, Flyway migration, 오류 코드와 응답 형식은 변경하지 않는다.

## Wiki 판단

이번 변경은 인증과 사용자 생명주기 규칙을 수정하므로 관련 Wiki를 확인하고 다음 내용을 반영한다.

- 탈퇴 시 학번 익명화와 인증 연결 제거 정책.
- 레거시 탈퇴 계정과 재가입 계정은 병합하지 않는 정책.
- 재가입 시 과거 활동 데이터가 이관되지 않는 정책.

PR에는 갱신한 Wiki 링크를 포함한다. 적절한 기존 문서가 없으면 새 운영·도메인 문서를 작성하고 링크를 남긴다.
