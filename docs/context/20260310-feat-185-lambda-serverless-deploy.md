# Context: Lambda 서버리스 배포 지원 및 EC2 배포 경로 분리

## 1. Feature Overview (Required)
- Purpose: 백엔드 앱을 `develop-shadow` 서버리스 환경에서 실행 가능하게 만들기 위해 Lambda 핸들러, Lambda 전용 패키징, 프로필 연결, Lambda 배포 workflow를 추가하고 기존 EC2 배포 경로는 rollback 용도로 유지한다.
- Scope:
  - In:
    - `RequestStreamHandler` 기반 Lambda 진입점 추가
    - Spring Boot 3용 AWS Serverless Java Container 의존성 및 `lambdaZip` task 추가
    - `develop-shadow -> dev` profile group 연결
    - Lambda 호환 로그 경로 설정으로 변경
    - EC2 workflow를 EC2 전용으로 명확히 유지하고, dev/prod Lambda workflow 추가
    - Lambda 프로필 관련 독립 테스트 추가
  - Out:
    - API Gateway, custom domain, Lambda alias, 환경변수 인프라 생성/수정
    - 운영 DB/JWT 등 런타임 비밀값 자체의 provisioning
    - 기존 비즈니스 로직 및 외부 API 계약 변경
- Expected Impact: `./gradlew lambdaZip -x test`만으로 Lambda 업로드용 zip 산출물을 만들 수 있고, dev/prod Lambda 배포 workflow를 같은 구조로 운영할 수 있으며, 기존 EC2 배포 경로는 rollback 용도로 유지된다.
- Stakeholder Confirmation: Requirement provided by requester on 2026-03-10 for Lambda/serverless support, profile/logging fixes, and parallel EC2/Lambda deployment workflows.

## 2. Domain Rules (Highest Priority, Required)
- Rule 1: Lambda 런타임에서는 반드시 servlet 모드로 부팅해야 하며, 핸들러 초기화 전에 `spring.main.web-application-type=servlet`를 강제한다.
- Rule 2: Lambda 산출물은 Spring Boot fat jar가 아니라 `classes + resources + lib/*.jar` 구조의 zip이어야 하며, 출력 경로는 `build/distributions/haksa-lambda.zip`로 고정한다.
- Rule 3: `develop-shadow` 프로필은 반드시 `dev` 설정을 포함해 shadow 환경이 dev 설정을 함께 읽도록 한다.
- Rule 4: 기존 EC2 배포 경로는 삭제하지 않고 EC2 전용 workflow로 명확히 분리한다.
- Rule 5: Lambda workflow의 책임은 빌드, S3 업로드, Lambda 코드 publish, alias 전환, alias 검증까지만 포함하며 인프라 리소스 생성/수정은 포함하지 않는다.
- Rule 6: Lambda 빌드 단계는 런타임 secret 없이 동작해야 하며, 필요한 경우는 명시적으로 설명 가능해야 한다.

- Mutable Rules:
  - Lambda active profile 결정 방식은 환경변수/시스템 프로퍼티 기반으로 조정 가능하다.
  - Workflow의 metadata 출력 형식(summary vs artifact)은 구현 편의에 맞게 조정 가능하다.
- Immutable Rules:
  - 핸들러 클래스 FQCN은 `com.chukchuk.haksa.global.lambda.StreamLambdaHandler`를 유지한다.
  - `develop-shadow`는 `dev` 설정을 포함해야 한다.
  - EC2 workflow는 rollback 경로로 유지한다.

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: Dev Lambda 배포
  - Trigger: 수동으로 dev Lambda workflow를 실행한다.
  - Actor: GitHub Actions / AWS Lambda
  - Steps:
    1. workflow가 `dev` 브랜치를 checkout한다.
    2. `./gradlew clean lambdaZip -x test`로 Lambda zip을 만든다.
    3. zip을 S3 artifact bucket에 업로드한다.
    4. `aws lambda update-function-code --publish`로 새 version을 생성한다.
    5. `live` alias를 새 version으로 전환하고 검증한다.
  - Expected Result: alias가 새 published version을 가리키고, 배포 metadata가 summary 또는 artifact로 남는다.

### Exception / Boundary Flow
- Scenario Name: Lambda 배포 검증 실패
  - Condition: 코드 publish, alias 전환, alias 검증 중 하나라도 실패한다.
  - Expected Behavior: workflow는 실패 처리되고, 성공 검증 전에는 최종 배포 성공으로 간주하지 않는다.

- Scenario Name: Shadow 프로필로 Lambda 부팅
  - Condition: Lambda가 `develop-shadow` 프로필로 시작한다.
  - Expected Behavior: `dev` 설정을 함께 읽고, `LOG_PATH=/tmp`가 주입되면 파일 appender 초기화가 실패하지 않는다.

## 4. Transaction and Consistency Policy (Required)
- Transaction Start Point: 기존 API 요청 단위 서비스 트랜잭션 경계를 그대로 사용한다.
- Transaction End Point: 기존 서비스 응답 반환 시점까지 유지한다.
- Atomicity Scope: 비즈니스 트랜잭션 경계는 변경하지 않으며, 배포 단위는 Lambda alias 전환 단위로 관리한다.
- Eventual Consistency Allowed: 배포 측면에서는 Lambda published version과 alias 전환 간 짧은 대기만 허용하며, 애플리케이션 비즈니스 로직의 eventual consistency 정책은 변경하지 않는다.

## 5. API List (Optional / Required When Present)
- Endpoint:
  - Method: 기존과 동일
  - Request DTO: 변경 없음
  - Response DTO: 변경 없음
  - Authorization: 변경 없음
  - Idempotency: 변경 없음

## 6. Exception Policy (Required)
- Error Code: 신규 추가 없음
  - Condition: 이번 작업은 빌드/패키징/배포 구조 정리이며 사용자 응답용 새 에러 코드를 도입하지 않는다.
  - Message Convention: 기존 ErrorCode 체계를 유지한다.
  - Handling Layer: 기존 Global exception handling 유지
  - User Exposure: 변경 없음

## 7. Phase Checklist
- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

## 8. Generated File List (Required)
- Path: docs/context/20260310-feat-185-lambda-serverless-deploy.md
  - Description: Lambda 서버리스 배포 지원과 EC2 workflow 분리를 위한 Context 문서
  - Layer: Context documentation
- Path: src/main/java/com/chukchuk/haksa/global/lambda/LambdaProfiles.java
  - Description: Lambda에서 사용할 active profile 해석 유틸리티
  - Layer: Global
- Path: src/main/java/com/chukchuk/haksa/global/lambda/StreamLambdaHandler.java
  - Description: HTTP API v2 이벤트를 처리하는 Lambda 스트림 핸들러
  - Layer: Global
- Path: src/test/java/com/chukchuk/haksa/global/lambda/LambdaProfilesTest.java
  - Description: `develop-shadow` profile group과 Lambda profile 해석을 검증하는 독립 테스트
  - Layer: Test
- Path: build.gradle
  - Description: Lambda 의존성과 `lambdaZip` packaging task 추가
  - Layer: Build
- Path: src/main/resources/application.yml
  - Description: `develop-shadow -> dev` profile group 연결
  - Layer: Global config
- Path: src/main/resources/logback-spring.xml
  - Description: Lambda에서 `/tmp` 경로를 사용할 수 있도록 로그 경로를 환경변수 기반으로 변경
  - Layer: Global config
- Path: .github/workflows/deploy-dev-ec2.yml
  - Description: 기존 dev EC2 배포 workflow를 rollback 용도로 명확히 유지
  - Layer: CI/CD
- Path: .github/workflows/deploy-prod-ec2.yml
  - Description: 기존 prod EC2 배포 workflow를 rollback 용도로 명확히 유지
  - Layer: CI/CD
- Path: .github/workflows/deploy-dev-lambda.yml
  - Description: dev Lambda 배포 workflow
  - Layer: CI/CD
- Path: .github/workflows/deploy-prod-lambda.yml
  - Description: prod Lambda 배포 workflow
  - Layer: CI/CD
