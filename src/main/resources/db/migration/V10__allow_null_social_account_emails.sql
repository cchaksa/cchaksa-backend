-- 이메일 claim이 없는 소셜 사용자의 실제 이메일 부재 상태를 저장한다.
ALTER TABLE public.users
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE public.social_accounts
    ALTER COLUMN email DROP NOT NULL;
