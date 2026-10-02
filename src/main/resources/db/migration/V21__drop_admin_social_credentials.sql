-- Phase A 코드가 dev live로 전환된 뒤 남은 관리자 Kakao 식별자와 제약을 제거한다.
ALTER TABLE public.admin_accounts
    DROP CONSTRAINT uq_admin_accounts_provider_social_id;

ALTER TABLE public.admin_accounts
    DROP CONSTRAINT chk_admin_accounts_provider;

ALTER TABLE public.admin_accounts
    DROP COLUMN provider;

ALTER TABLE public.admin_accounts
    DROP COLUMN social_id;
