# #361 관리자 Kakao 레거시 스키마 단계적 제거 설계

## 목표와 경계

관리자 인증은 `loginId/password`와 DB 저장형 `admin_sessions`를 사용한다. 관리자 Kakao 로그인에서만 사용하던 `admin_accounts.provider`, `admin_accounts.social_id`, 관련 제약과 `admin_login_challenges`를 제거한다.

일반 사용자 `social_accounts`, `UserService`, Kakao/Apple OIDC와 관리자 `admin_sessions`는 변경하지 않는다. 적용된 V16~V19는 수정하거나 rollback하지 않는다.

## 배포 제약

`deploy-dev-lambda`는 Flyway migration을 먼저 적용하고 새 Lambda를 게시한다. 현재 dev `live` version 99의 `AdminAccount`는 `provider/social_id`를 매핑하므로 두 컬럼을 먼저 삭제하면 migration과 alias 전환 사이의 cold start, 게시 실패 시 기존 alias 유지, 수동 rollback이 깨진다.

따라서 하나의 이슈와 PR에서 다음 두 phase를 순차적으로 진행한다.

1. Phase A는 코드가 두 컬럼을 더 이상 매핑하지 않도록 한 뒤, 런타임에서 참조하지 않는 challenge 테이블만 V20으로 삭제한다. 두 컬럼과 제약은 유지한다.
2. Phase B는 Phase A SHA가 dev `live`에 배포되고 새 Lambda가 정상 동작하는 것을 확인한 뒤 V21로 두 제약과 두 컬럼을 삭제한다.

`origin/dev`에는 아직 #352~#355가 병합되지 않았으므로 `feat/361`은 배포된 `origin/feat/355` 위에 쌓는다. PR base는 `feat/355`로 두고, 선행 PR 병합 후 최종 base를 `dev`로 변경한다.

## 검토한 접근

### 1. 한 migration에서 엔티티와 모든 레거시 스키마 제거

변경은 작지만 migration 선실행 구간에 version 99가 없는 컬럼을 검증하므로 채택하지 않는다. 새 Lambda 게시 실패 시 alias 99도 다시 시작할 수 없어 recovery 경계를 훼손한다.

### 2. Phase A/B 순차 확장-축소

Phase A에서 코드 의존성을 먼저 제거하고 독립적인 challenge 테이블만 삭제한다. 새 코드가 dev `live`가 된 후 Phase B에서 컬럼을 축소한다. 두 배포 사이에는 구 스키마와 신 코드가 호환되므로 채택한다.

### 3. 컬럼을 영구 유지하고 엔티티만 정리

배포 위험은 가장 낮지만 승인된 데이터 모델 정리 목표를 달성하지 못하고 관리자 Kakao 권한 원본으로 오해될 여지를 남겨 채택하지 않는다.

## Phase A 상세

- `AdminAccount`에서 `provider`, `socialId` 필드와 JPA 매핑을 제거한다.
- V20은 `DROP TABLE public.admin_login_challenges`만 수행한다. 테이블 소유 unique 제약과 인덱스는 PostgreSQL이 함께 제거하며 외부 FK가 없으므로 `CASCADE`는 사용하지 않는다.
- `provider/social_id`, `uq_admin_accounts_provider_social_id`, `chk_admin_accounts_provider`는 그대로 둔다.
- H2 fresh migration과 실제 PostgreSQL V19 fixture에서 challenge 테이블 제거, 관리자 컬럼 유지, Hibernate 전체 schema validation을 확인한다.
- 관리자 로그인·세션 테스트와 전체 `check`를 실행한다.

Phase A 실패 시 적용된 V20을 rollback하거나 수정하지 않는다. Lambda 게시 전 migration만 성공해도 version 99는 challenge 테이블을 참조하지 않으므로 계속 동작한다. 보정이 필요하면 V21 이후의 새 forward migration을 사용하고 Phase B 번호를 다시 배정한다.

## Phase B 진입 조건

- Phase A 커밋이 포함된 Lambda가 dev에서 `Active/Successful`이어야 한다.
- `live` alias가 해당 version을 가리켜야 한다.
- CSRF, 비인증 `/me`, 관리자 인증·세션 smoke 결과가 정상이어야 한다.
- 이 조건을 오케스트레이터가 확인하기 전에는 V21 파일을 추가하지 않는다.

Phase B에서는 dev PostgreSQL `pg_constraint`와 `information_schema.columns`를 읽어 실제 이름을 재확인한다. V16에 선언된 이름과 일치하면 제약을 먼저 삭제한 뒤 컬럼을 삭제한다. 예상 이름이 명시적이고 drift를 숨기면 안 되므로 기본안은 `IF EXISTS` 없이 실패를 드러내는 것이다. 실제 read-back에서 환경별 drift가 확인될 때만 근거를 문서화하고 조정한다.

## Wiki

관리자 인증, DB 스키마, 순차 배포와 장애 복구 이력에 영향을 주므로 별도 Wiki 저장소의 관련 문서를 후속 갱신해야 한다. 이 PR에는 Wiki 미갱신과 후속 항목을 명시한다.
