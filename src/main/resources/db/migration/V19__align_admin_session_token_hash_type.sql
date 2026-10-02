-- SHA-256 hex 해시의 실제 길이와 Hibernate String 매핑을 일치시킨다.
ALTER TABLE public.admin_sessions
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING RTRIM(token_hash)::VARCHAR(64);
