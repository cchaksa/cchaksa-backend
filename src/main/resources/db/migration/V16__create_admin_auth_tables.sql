-- 일반 사용자 인증과 분리된 관리자 허용 목록, 로그인 challenge, 폐기 가능한 세션을 저장한다.
CREATE TABLE public.admin_accounts (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    admin_role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_admin_accounts_provider_social_id UNIQUE (provider, social_id),
    CONSTRAINT chk_admin_accounts_provider CHECK (provider = 'KAKAO'),
    CONSTRAINT chk_admin_accounts_role CHECK (admin_role IN ('ADMIN', 'CS_AGENT')),
    CONSTRAINT chk_admin_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE public.admin_login_challenges (
    id UUID PRIMARY KEY,
    nonce VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_admin_login_challenges_nonce UNIQUE (nonce)
);

CREATE INDEX idx_admin_login_challenges_expires_at
    ON public.admin_login_challenges (expires_at);

CREATE TABLE public.admin_sessions (
    id UUID PRIMARY KEY,
    admin_account_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    idle_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_admin_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_admin_sessions_admin_account
        FOREIGN KEY (admin_account_id) REFERENCES public.admin_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_sessions_account_active
    ON public.admin_sessions (admin_account_id, revoked_at, expires_at);

