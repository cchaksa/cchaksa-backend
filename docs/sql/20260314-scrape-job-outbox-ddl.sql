CREATE TABLE scrape_job_outbox (
    outbox_id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE NULL,
    sent_at TIMESTAMP WITH TIME ZONE NULL,
    queue_message_id VARCHAR(255) NULL,
    last_error TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT uk_scrape_job_outbox_job_id UNIQUE (job_id),
    CONSTRAINT fk_scrape_job_outbox_job_id FOREIGN KEY (job_id) REFERENCES scrape_jobs (job_id)
);

CREATE INDEX idx_scrape_job_outbox_status_next_attempt_at
    ON scrape_job_outbox (status, next_attempt_at);

CREATE INDEX idx_scrape_job_outbox_status_sent_at
    ON scrape_job_outbox (status, sent_at);
