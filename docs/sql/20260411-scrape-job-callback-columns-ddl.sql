CREATE TABLE IF NOT EXISTS public.scrape_jobs (
    job_id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    error_code VARCHAR(255) NULL,
    error_message TEXT NULL,
    finished_at TIMESTAMP WITH TIME ZONE NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(255) NOT NULL,
    portal_type VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    request_payload_json TEXT NOT NULL,
    result_payload_json TEXT NULL,
    retryable BOOLEAN NULL,
    status VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    callback_attempt INTEGER NULL,
    callback_received_at TIMESTAMP WITH TIME ZONE NULL,
    result_s3_key VARCHAR(255) NULL,
    result_checksum VARCHAR(255) NULL,
    callback_metadata_json TEXT NULL,
    CONSTRAINT uk_scrape_jobs_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS public.scrape_job_outbox (
    outbox_id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    attempt_count INTEGER NOT NULL,
    job_id VARCHAR(255) NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE NULL,
    last_error TEXT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NULL,
    payload_json TEXT NOT NULL,
    queue_message_id VARCHAR(255) NULL,
    sent_at TIMESTAMP WITH TIME ZONE NULL,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT uk_scrape_job_outbox_job_id UNIQUE (job_id),
    CONSTRAINT fk_scrape_job_outbox_job_id FOREIGN KEY (job_id) REFERENCES public.scrape_jobs (job_id)
);

CREATE INDEX IF NOT EXISTS idx_scrape_job_outbox_status_next_attempt_at
    ON public.scrape_job_outbox (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_scrape_job_outbox_status_sent_at
    ON public.scrape_job_outbox (status, sent_at);

ALTER TABLE public.scrape_jobs
    ADD COLUMN IF NOT EXISTS result_s3_key VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS result_checksum VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS callback_attempt INTEGER NULL,
    ADD COLUMN IF NOT EXISTS callback_received_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS callback_metadata_json TEXT NULL;
