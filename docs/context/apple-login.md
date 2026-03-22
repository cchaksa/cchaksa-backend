# Context: Apple Login (feat/78)

## 1. Feature Overview (Required)

- Purpose: 기존 `/api/users/signin`에 Apple 로그인을 추가하여 동일 엔드포인트에서 Kakao/Apple을 분기 처리한다.
- Scope:
  - In:
    - `/api/users/signin` 요청 DTO에 `provider` 필드 추가 (`OidcProvider` 사용)
    - Apple identity token 검증 로직 추가 (블로그 기준: header `alg/kid` 추출, 공개키 조회/매칭, `iss/aud/exp/nonce` 검증)
    - `OidcProvider`에 `APPLE` 추가 및 OIDC 서비스 매핑
  - Out:
    - 새로운 엔드포인트 추가
    - 사용자 생성/병합 정책 변경
- Expected Impact: Apple 로그인 지원 및 기존 Kakao 로그인과 동일한 사용자 처리 정책 유지

If this section is empty, stop the work.

---

## 2. Domain Rules (Highest Priority, Required)

- Rule 1: 로그인 성공 후 사용자 처리(기존 사용자 매칭/신규 생성/연동)는 현재 정책과 동일하게 유지한다.
- Rule 2: `/api/users/signin`은 요청된 `provider`에 따라 OIDC 검증 로직을 분기한다.
- Rule 3: Apple identity token은 공개키 서명 검증 및 클레임 검증을 통과해야 한다.

- Mutable Rules: OIDC Provider 종류 확장 가능
- Immutable Rules: 사용자 처리 정책 변경 금지

If at least one rule is not specified, stop the work.

---

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: Apple 로그인 성공
  - Trigger: 클라이언트가 `/api/users/signin`으로 `provider=APPLE`, `id_token`, `nonce` 전달
  - Actor: Mobile Client
  - Steps:
    - 서버는 Apple identity token의 header에서 `alg`, `kid` 추출
    - Apple 공개키(`https://appleid.apple.com/auth/keys`)를 조회하여 `kid`+`alg` 매칭 키를 선택
    - 선택된 공개키로 서명 검증 및 클레임 파싱
    - `iss/aud/exp/nonce`를 검증
    - 기존 정책대로 사용자 조회/생성 및 토큰 발급
  - Expected Result: access/refresh token 및 포털 연동 여부 반환

### Exception / Boundary Flow
- Scenario Name: Apple identity token 검증 실패
  - Condition: 서명 검증 실패, `iss/aud/exp/nonce` 불일치, 공개키 매칭 실패
  - Expected Behavior: 적절한 `ErrorCode`로 인증 실패 응답

---

## 4. Transaction and Consistency Policy (Required)

- Transaction Start Point: `UserService.signIn`
- Transaction End Point: access/refresh token 생성 및 저장 완료
- Atomicity Scope: 사용자 조회/생성 및 RefreshToken 저장
- Eventual Consistency Allowed: 없음

---

## 5. API List (Optional / Required When Present)

- Endpoint: `/api/users/signin`
  - Method: `POST`
  - Request DTO: `UserDto.SignInRequest`
    - `provider: OidcProvider` (KAKAO/APPLE)
    - `id_token: String`
    - `nonce: String`
  - Response DTO: `UserDto.SignInResponse`
  - Authorization: 없음
  - Idempotency: 없음

---

## 6. Exception Policy (Required)

- Error Code:
  - `TOKEN_INVALID_FORMAT`: JWT 형식 오류
  - `TOKEN_NO_MATCHING_KEY`: 공개키 매칭 실패
  - `TOKEN_PARSE_ERROR`: 파싱/검증 중 예외
  - `TOKEN_EXPIRED`: exp 만료
  - `TOKEN_INVALID_ISS`: iss 불일치
  - `TOKEN_INVALID_AUD`: aud 불일치
  - `TOKEN_INVALID_AUD_FORMAT`: aud 형식 오류
  - `TOKEN_INVALID_NONCE`: nonce 불일치
  - `TOKEN_HASH_ERROR`: nonce 해시 오류
  - `TOKEN_INVALID`: 기타 토큰 검증 실패
- If Apple 전용 에러가 필요하면 신규 `ErrorCode`를 추가한다.

---

## 7. Phase Checklist

- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [x] Phase 2 Domain: models, services, exceptions, pure tests written
- [x] Phase 3 Application: orchestration, transactions, repository interface validation
- [x] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [x] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [x] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

---

## 8. Generated File List (Required)

- Path: `src/main/java/com/chukchuk/haksa/domain/user/dto/UserDto.java`
  - Description: `SignInRequest`에 `provider` 필드 추가
  - Layer: domain
- Path: `src/main/java/com/chukchuk/haksa/global/security/service/OidcProvider.java`
  - Description: `APPLE` 추가
  - Layer: global
- Path: `src/main/java/com/chukchuk/haksa/domain/user/service/UserService.java`
  - Description: provider 분기 처리
  - Layer: domain
- Path: `src/main/java/com/chukchuk/haksa/domain/user/controller/docs/UserControllerDocs.java`
  - Description: 로그인 API 설명 갱신
  - Layer: domain
- Path: `src/main/java/com/chukchuk/haksa/infrastructure/oidc/AppleOidcService.java` (신규)
  - Description: Apple identity token 검증
  - Layer: infrastructure
- Path: `src/main/java/com/chukchuk/haksa/global/config/OIDCConfig.java`
  - Description: OIDC 서비스 매핑 갱신
  - Layer: global
- Path: `src/main/resources/application.yml`
  - Description: Apple client_id/issuer/keys URL 설정
  - Layer: global
- Path: `src/main/resources/public/openapi.yaml`
  - Description: `/api/users/signin` 요청/응답 스키마 갱신
  - Layer: global
- Path: `build.gradle`
  - Description: 테스트용 H2 의존성 추가
  - Layer: global
- Path: `src/test/resources/application.yml`
  - Description: 테스트 환경 H2 및 보안 설정
  - Layer: test
- Path: `src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceTests.java`
  - Description: provider 분기 및 토큰 응답 테스트
  - Layer: test
- Path: `src/test/java/com/chukchuk/haksa/infrastructure/oidc/AppleOidcServiceTests.java`
  - Description: Apple identity token 검증 테스트
  - Layer: test

---

## Stakeholder Confirmation Memo

- Confirmed: `/api/users/signin`에 `provider` 추가하여 Kakao/Apple 분기
- Confirmed: nonce 사용
- Confirmed: Apple token 검증은 블로그 내용과 동일한 절차 적용
- Confirmed: 사용자 처리 정책은 기존과 동일
- Confirmed: ErrorCode는 기존 재사용, 없으면 신규 추가
- Confirmed: Apple nonce는 Kakao와 동일하게 SHA-256 해시 후 토큰 `nonce`와 비교
- Confirmed: Apple 설정은 `application.yml`에 추가
