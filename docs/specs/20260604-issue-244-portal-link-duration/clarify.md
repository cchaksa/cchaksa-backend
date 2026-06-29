# Clarify

## Decisions

| # | Decision | Reason | Date |
|---|---|---|---|
| 1 | Use `GET /portal/link/jobs/{jobId}/duration`. | Existing job status and summary endpoints are under `/portal/link/jobs/{jobId}`. | 2026-06-04 |
| 2 | Store duration timestamps on `scrape_jobs`, not `scrape_job_outbox`. | Outbox timestamps describe queue publication attempts, not end-to-end link processing. | 2026-06-04 |
| 3 | Keep `finished_at` compatibility semantics and add `link_started_at`/`link_ended_at`. | Current success flow can use worker payload `finished_at`; the new API needs server-side timing. | 2026-06-04 |
| 4 | Do not use Spring AOP for terminal timestamp writes. | Terminal status and terminal timestamp must be written in the same transaction; AOP would hide or split that boundary. | 2026-06-04 |
