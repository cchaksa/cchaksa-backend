# Clarify Template

## Open Questions
| # | Question | Owner | Status | Decision |
|---|----------|-------|--------|----------|
| 1 | S3 key 신뢰 수준: prefix/validation 규칙을 어떻게 제한할지? | BE 팀 | Decided | `s3_key`는 신뢰하지 않는다. 백엔드는 `SCRAPE_RESULT_PREFIX` 하위 key만 허용하고, key 정규화 후 `..`, leading `/`, URL 형태, 다른 prefix를 거부한다. 권장 key 형식은 `develop-shadow/{jobId}/result.json`이며, key 내부 `jobId`와 요청 `jobId`도 매칭한다. |
| 2 | S3 read 실패 시 job 상태를 즉시 `FAILED`로 전환할지, 재시도 큐를 둘지? | BE 팀 | Decided | 별도 재시도 큐는 두지 않는다. `NoSuchKey`, S3 `5xx`, timeout은 3회 exponential backoff 후 `FAILED_S3_READ`로 전환하고, `AccessDenied` 및 validation 실패는 즉시 실패 처리한다. |
| 3 | 기존 payload 직접 수신 방식과의 호환이 필요한가? 필요 시 기간/전환 전략? | BE 팀 | Decided | 기존 payload 직접 수신 방식은 완전히 폐기한다. `s3_key` 누락 또는 direct payload 요청은 계약 위반으로 실패 처리하며, 롤백은 워커/IaC 배포 롤백으로 대응한다. |
| 4 | 결과 JSON schema 버전을 어디에서 관리하고 후방 호환성을 어떻게 보장할지? | BE 팀 | Decided | 결과 JSON에는 필수 `schema_version`을 포함한다. schema 정의/검증은 백엔드 repo에서 관리하며, additive 변경만 허용하고 breaking change는 `v2` 등 새 버전으로 분리한다. |
| 5 | after-processing 실패 시 상태 반영 정책 (예: `FAILED_POST_PROCESSING`)이 필요한가? | BE 팀 | Decided | after-processing 실패를 구분하기 위해 `FAILED_POST_PROCESSING`을 추가한다. S3 read 실패는 `FAILED_S3_READ`, JSON/schema 실패는 `FAILED_RESULT_SCHEMA`, 후처리 실패는 `FAILED_POST_PROCESSING`으로 기록한다. |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | callback 최소 동기 처리는 검증 + 상태 저장까지만 수행, S3 read는 비동기 처리 파이프라인으로 분리 | timeout 리스크 경감 및 스크래핑 서버 SLA 확보 | 2026-04-10 |
| 2 | 2026-04-10 사용자 "작업 시작" 지시에 따라 OK to implement 승인 확보 | Phase 2 진행 승인 | 2026-04-10 |

## Risks / Unknowns
- Item: S3 key에 대한 인증/권한 검증 미흡 시 외부 리소스 접근 가능성
  - Impact: 악의적 key 주입으로 시스템이 임의 파일을 다운로드할 수 있음
  - Mitigation: 허용 prefix/bucket allowlist, key validate, signature 검증
- Item: duplicate callback이 빈번할 경우 상태 전이 race condition
  - Impact: job 상태가 예기치 않게 덮어써질 수 있음
  - Mitigation: DB 레벨 unique constraint + version check + 멱등 저장 로직

## Follow-ups
- [ ] Open 질문 1~5 답변 확보 (Owner: BE 팀)
