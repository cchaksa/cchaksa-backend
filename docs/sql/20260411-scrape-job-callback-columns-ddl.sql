ALTER TABLE scrape_jobs
    ADD COLUMN result_checksum VARCHAR(255) NULL;

ALTER TABLE scrape_jobs
    ADD COLUMN callback_metadata_json TEXT NULL;
