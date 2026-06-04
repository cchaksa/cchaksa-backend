ALTER TABLE public.scrape_jobs
    ADD COLUMN link_started_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE public.scrape_jobs
    ADD COLUMN link_ended_at TIMESTAMP WITH TIME ZONE NULL;

UPDATE public.scrape_jobs
SET link_started_at = COALESCE(created_at, NOW())
WHERE link_started_at IS NULL;

UPDATE public.scrape_jobs
SET link_ended_at = COALESCE(finished_at, updated_at, link_started_at)
WHERE status IN ('SUCCEEDED', 'FAILED')
  AND link_ended_at IS NULL;

ALTER TABLE public.scrape_jobs
    ALTER COLUMN link_started_at SET NOT NULL;
