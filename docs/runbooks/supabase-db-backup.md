# Supabase DB 백업 Runbook

## 목적

이 문서는 GitHub Actions 기반 Supabase PostgreSQL logical backup의 사전 설정, 운영 확인, 복원 리허설 절차를 정의한다.

## 자동 백업 정책

- 실행: 매일 KST 05:00. GitHub Actions cron은 UTC `0 20 * * *`을 사용한다.
- 수동 실행: Actions의 `Database Backup` workflow에서 `Run workflow`를 실행한다.
- 저장: 날짜별 daily backup을 누적하고, `latest/`에는 가장 최근 성공 backup을 덮어쓴다.
- monthly: KST 매월 1일에 생성된 daily backup을 `monthly/` prefix에도 보관한다.
- 삭제: daily는 30일, monthly는 365일 후 S3 Lifecycle로 만료한다. Lifecycle은 정확한 KST 03:00 실행을 보장하지 않으며 비동기로 객체를 만료한다.
- 삭제 권한: GitHub Actions IAM Role에는 `s3:DeleteObject`를 부여하지 않는다.

## GitHub Environment 설정

`prod` GitHub Environment에 다음 값을 설정한다.

| 종류 | 이름 | 설명 |
| --- | --- | --- |
| Secret | `PROD_SUPABASE_DB_URL` | Supabase Session Pooler PostgreSQL 연결 문자열. 실제 값은 절대 로그나 문서에 남기지 않는다. |
| Secret | `BACKUP_ENCRYPTION_PASSPHRASE` | GPG 대칭 암호화 passphrase. 충분한 길이의 무작위 값으로 생성하고 별도 비밀 관리 도구에도 보관한다. |
| Variable | `PROD_AWS_REGION` | backup bucket의 AWS 리전. |
| Variable | `PROD_BACKUP_S3_BUCKET` | 전용 backup bucket 이름. |
| Variable | `PROD_DB_BACKUP_ROLE_ARN` | GitHub OIDC가 AssumeRole할 backup 전용 IAM Role ARN. |

Supabase Free 플랜에서 GitHub-hosted runner는 direct DB endpoint 대신 IPv4 접근이 가능한 Session Pooler 연결 문자열을 사용한다. Transaction Pooler는 dump 작업에 사용하지 않는다.

## Terraform 선행 조건

Terraform 저장소에서 다음을 프로비저닝한 후 workflow를 수동 실행한다.

1. 기존 Lambda artifact bucket과 분리된 private S3 backup bucket을 만든다.
2. Block Public Access, versioning, default server-side encryption을 켠다. KMS를 사용한다면 bucket default encryption을 SSE-KMS로 설정하고 key 정책에 backup role을 포함한다.
3. lifecycle rule을 설정한다.
   - `supabase-db/daily/`: 30일 후 만료.
   - `supabase-db/monthly/`: 365일 후 만료.
   - `supabase-db/latest/`: lifecycle 만료 대상에서 제외.
4. GitHub OIDC provider와 backup 전용 IAM Role을 만든다.
   - trust policy의 audience는 `sts.amazonaws.com`으로 제한한다.
   - subject는 `repo:cchaksa/cchaksa-backend:ref:refs/heads/dev`로 제한한다.
5. Role permission은 bucket에 `s3:ListBucket`, object prefix `supabase-db/*`에 `s3:PutObject`, `s3:GetObject`만 허용한다. KMS 사용 시 필요한 encrypt/decrypt 권한만 별도 부여한다.

Workflow의 `HeadObject` 검증은 `s3:GetObject` 권한으로 수행한다. S3 object versioning과 `latest/` overwrite가 함께 동작하므로 이전 latest object version은 lifecycle의 noncurrent-version 정책으로 별도 관리한다.

## 성공 확인

1. Actions workflow가 성공 상태인지 확인한다.
2. 실행 summary에 daily prefix와 3개 파일 검증 결과가 기록됐는지 확인한다.
3. S3에 `roles.sql.gz.gpg`, `schema.sql.gz.gpg`, `data.sql.gz.gpg`가 non-zero size로 있는지 확인한다.
4. 월 1일 실행이면 monthly prefix도 확인한다.
5. 최초 도입과 이후 월 1회는 아래 복원 리허설을 실행한다.

## 복원 리허설

운영 DB에 직접 복원하지 않는다. 새 Supabase 프로젝트 또는 격리된 임시 PostgreSQL DB를 사용한다.

1. 복원 대상 timestamp의 세 파일을 다운로드한다.
2. 암호화 passphrase를 사용해 각 파일을 복호화하고 gzip을 해제한다.

```bash
gpg --batch --decrypt roles.sql.gz.gpg | gzip --decompress > roles.sql
gpg --batch --decrypt schema.sql.gz.gpg | gzip --decompress > schema.sql
gpg --batch --decrypt data.sql.gz.gpg | gzip --decompress > data.sql
```

3. 대상 DB의 연결 문자열을 설정한 뒤 SQL을 `roles -> schema -> data` 순서로 적용한다.

```bash
psql \
  --single-transaction \
  --variable ON_ERROR_STOP=1 \
  --file roles.sql \
  --file schema.sql \
  --command 'SET session_replication_role = replica' \
  --file data.sql \
  --dbname "$RESTORE_DB_URL"
```

4. 핵심 테이블 row count, Flyway migration history, 강의평가 관련 테이블을 확인한다.
5. 복원에 사용한 timestamp, 결과, 오류와 조치 사항을 이슈 또는 운영 기록에 남긴다.

## 실패와 복구

- dump가 실패하면 Actions job을 실패 상태로 끝낸다. 실패한 daily prefix는 복원 대상으로 사용하지 않는다.
- dump 시간에 Flyway migration, 수동 DDL, 대량 정리 작업을 동시에 실행하지 않는다. `pg_dump` 계열 작업은 일반 DML을 중단하지 않지만 강한 DDL과 대기할 수 있다.
- GitHub Actions schedule 지연이나 누락이 반복되거나 backup 시간이 길어지면 EventBridge Scheduler + ECS Fargate 실행 환경으로 전환을 검토한다.
- workflow 또는 script를 롤백해도 이미 생성된 S3 backup은 삭제하지 않는다.
