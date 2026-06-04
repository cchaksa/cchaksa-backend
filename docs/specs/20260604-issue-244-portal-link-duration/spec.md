# Spec

## Context

Issue: https://github.com/cchaksa/cchaksa-backend/issues/244

`POST /portal/link` accepts an asynchronous portal link job and returns a `job_id`. Clients can already poll job status through `GET /portal/link/jobs/{jobId}` and fetch the completed summary through `GET /portal/link/jobs/{jobId}/summary`.

This work adds a separate API for the elapsed time between server-side job acceptance and server-side terminal-state decision.

## Goal

Expose a job duration API that reports:

- whether the job is still pending or terminal
- whether the terminal result succeeded or failed
- server-recorded start and end timestamps
- elapsed time as both milliseconds and a display string

Completion means `scrape_jobs.status == SUCCEEDED`. Terminal states are `SUCCEEDED` and `FAILED`.

## Non-Goals

- Do not change `POST /portal/link`.
- Do not change `GET /portal/link/jobs/{jobId}`.
- Do not change `GET /portal/link/jobs/{jobId}/summary`.
- Do not use worker callback `finished_at` as the duration API's server end timestamp.
- Do not move SQS dispatch, scraping, S3 fetch, or portal sync behavior.

## API

Endpoint:

```text
GET /portal/link/jobs/{jobId}/duration
```

Pending response:

```json
{
  "job_id": "job-123",
  "status": "pending",
  "success": null,
  "started_at": "2026-06-04T10:00:00Z",
  "ended_at": null,
  "elapsed_millis": null,
  "elapsed_time": null
}
```

Succeeded response:

```json
{
  "job_id": "job-123",
  "status": "succeeded",
  "success": true,
  "started_at": "2026-06-04T10:00:00Z",
  "ended_at": "2026-06-04T10:00:12.345Z",
  "elapsed_millis": 12345,
  "elapsed_time": "12s 345ms"
}
```

Failed response:

```json
{
  "job_id": "job-123",
  "status": "failed",
  "success": false,
  "started_at": "2026-06-04T10:00:00Z",
  "ended_at": "2026-06-04T10:00:03.120Z",
  "elapsed_millis": 3120,
  "elapsed_time": "3s 120ms"
}
```

The endpoint uses the existing authenticated job ownership check. Unknown or non-owned jobs return the same not-found behavior as existing job query APIs.

## Data Model

Add columns to `scrape_jobs`:

- `link_started_at TIMESTAMP WITH TIME ZONE`
- `link_ended_at TIMESTAMP WITH TIME ZONE NULL`

`link_started_at` is recorded when a new job is accepted. Existing rows can be backfilled from `created_at` in the migration.

`link_ended_at` is recorded when the server transitions the job into `SUCCEEDED` or `FAILED`.

Existing `finished_at` stays unchanged. It may continue to reflect worker callback timing and compatibility semantics.

## AOP Decision

Duration measurement is separate from portal link business processing, but the end timestamp must be written atomically with the terminal status transition. A Spring AOP advice around the service methods would either need a second repository update or would obscure the transaction boundary.

Therefore this implementation does not use AOP. It keeps the concern separated by adding explicit duration fields and domain methods on `ScrapeJob`, then calling them only at the existing terminal transition points.

## Acceptance Criteria

- `QUEUED`, `RUNNING`, and `POST_PROCESSING` jobs return `status=pending`.
- `SUCCEEDED` jobs return `status=succeeded` and `success=true`.
- `FAILED` jobs return `status=failed` and `success=false`.
- Pending jobs return null elapsed fields.
- Terminal jobs return `elapsed_millis` and `{seconds}s {millis}ms`.
- Server end timestamp is independent of callback payload `finished_at`.
- Existing portal link status and summary API contracts remain unchanged.
