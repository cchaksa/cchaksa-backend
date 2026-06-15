-- 리프레시 토큰을 로그인 세션 단위로 저장하도록 전환한다
ALTER TABLE public.refresh_token
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(255);

UPDATE public.refresh_token
SET session_id = user_id
WHERE session_id IS NULL;

ALTER TABLE public.refresh_token
    ALTER COLUMN session_id SET NOT NULL;

ALTER TABLE public.refresh_token
    DROP CONSTRAINT IF EXISTS pk_refresh_token;

ALTER TABLE public.refresh_token
    ADD CONSTRAINT pk_refresh_token PRIMARY KEY (session_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id
    ON public.refresh_token (user_id);
