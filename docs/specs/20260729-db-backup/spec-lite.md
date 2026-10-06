# DB 백업

## 목적

Supabase PostgreSQL에서 서비스가 생성한 원천 데이터를 일관되게 보관하고, 장애 또는 운영 실수 후 복원할 수 있는 최소 백업 체계를 제공한다.

## 범위

- GitHub Actions에서 KST 05:00에 하루 한 번 Supabase CLI logical dump를 실행한다.
- 수동 실행(`workflow_dispatch`)을 지원한다.
- `roles.sql`, `schema.sql`, `data.sql`을 생성해 gzip 및 GPG 대칭 암호화 후 전용 S3 버킷에 올린다.
- 날짜별 backup, `latest/` 사본, 매월 1일 KST 실행 시 monthly 사본을 만든다.
- runbook에 복원 순서, GitHub 설정값, S3/IAM/Terraform 선행 조건과 보관 정책을 기록한다.

## 비범위

- 백업 버킷, OIDC IAM Role, KMS Key의 Terraform 생성.
- S3 객체를 workflow에서 삭제하는 cleanup job.
- Supabase 관리 스키마(`auth`, `storage`, extension 생성 스키마)의 전체 백업.
- EventBridge Scheduler 또는 ECS 기반 실행 환경.

## 설계

### 실행

- workflow는 `prod` GitHub Environment에서 실행하며, `id-token: write`로 AWS OIDC Role을 AssumeRole 한다.
- GitHub scheduled workflow는 기본 브랜치에서만 실행되므로, 이 저장소의 기본 브랜치인 `dev`에 workflow를 둔다.
- 동일한 prod backup은 `concurrency`로 직렬화한다.

### 저장 구조

```text
s3://<bucket>/supabase-db/daily/YYYY/MM/DD/<utc-timestamp>/{roles,schema,data}.sql.gz.gpg
s3://<bucket>/supabase-db/latest/{roles,schema,data}.sql.gz.gpg
s3://<bucket>/supabase-db/monthly/YYYY/MM/<utc-timestamp>/{roles,schema,data}.sql.gz.gpg
```

- Daily와 monthly는 서로 다른 prefix를 사용해 독립 lifecycle을 적용한다.
- `latest/`는 편의용 사본이므로 덮어쓴다.
- workflow에는 `DeleteObject` 권한을 부여하지 않는다.

### 복원

- GPG 복호화와 gzip 해제 후 `roles.sql`, `schema.sql`, `data.sql` 순서로 `psql`에 전달한다.
- 새 프로젝트 또는 격리된 임시 DB에서 월 1회 리허설한다.
- schema와 data dump는 Supabase CLI가 기본적으로 제외하는 관리 스키마를 포함하지 않는다는 점을 리허설에서 확인한다.

## 필요한 GitHub 설정

Secrets.

- `PROD_SUPABASE_DB_URL`.
- `BACKUP_ENCRYPTION_PASSPHRASE`.

Variables.

- `PROD_AWS_REGION`.
- `PROD_BACKUP_S3_BUCKET`.
- `PROD_DB_BACKUP_ROLE_ARN`.

## S3/IAM 선행 조건

- 전용 private bucket, Block Public Access, versioning, default encryption을 설정한다.
- OIDC Role은 `cchaksa/cchaksa-backend`의 `dev` 브랜치 workflow만 AssumeRole하도록 trust policy를 제한한다.
- Role에는 백업 bucket의 `supabase-db/*`에 대한 `PutObject`, `GetObject`, bucket의 `ListBucket`과 `HeadObject`에 필요한 최소 권한만 부여한다.
- S3 Lifecycle은 `daily/` 30일, `monthly/` 365일 만료를 적용한다. `latest/`는 lifecycle 대상에서 제외한다.

## 검증과 롤백

- workflow YAML과 shell script 문법을 검증한다.
- 애플리케이션 회귀 방지를 위해 `./gradlew test`를 실행한다.
- 실제 production dump는 PR 단계에서 실행하지 않는다.
- 롤백은 workflow와 script, runbook을 되돌리고 S3 lifecycle을 Terraform 상태에서 별도로 되돌린다. 이미 저장된 backup object는 삭제하지 않는다.

## 리스크

- GitHub Actions cron은 정확한 시각 실행을 보장하지 않는다. 누락 또는 지연이 관찰되면 EventBridge Scheduler + ECS Fargate 전환을 검토한다.
- `pg_dump` 계열 백업은 일반 DML을 중단시키지 않지만, 강한 DDL과 대기할 수 있다. backup 시간에는 Flyway migration과 수동 DDL을 실행하지 않는다.
