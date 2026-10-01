-- 기존 관리자 Kakao 행과 이전 Lambda를 유지하면서 로컬 로그인 자격증명을 추가한다.
ALTER TABLE public.admin_accounts
    ADD COLUMN login_id VARCHAR(255) NULL;

ALTER TABLE public.admin_accounts
    ADD COLUMN password_hash VARCHAR(255) NULL;

ALTER TABLE public.admin_accounts
    ALTER COLUMN provider DROP NOT NULL;

ALTER TABLE public.admin_accounts
    ALTER COLUMN social_id DROP NOT NULL;

ALTER TABLE public.admin_accounts
    ADD CONSTRAINT uq_admin_accounts_login_id UNIQUE (login_id);

ALTER TABLE public.admin_accounts
    ADD CONSTRAINT chk_admin_accounts_local_credentials
        CHECK ((login_id IS NULL) = (password_hash IS NULL));
