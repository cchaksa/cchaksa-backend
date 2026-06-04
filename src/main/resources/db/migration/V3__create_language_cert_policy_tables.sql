CREATE TABLE IF NOT EXISTS public.language_cert_policy_groups (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    group_key VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT pk_language_cert_policy_groups PRIMARY KEY (id),
    CONSTRAINT uk_language_cert_policy_groups_group_key UNIQUE (group_key)
);

CREATE TABLE IF NOT EXISTS public.language_cert_requirements (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    policy_group_id UUID NOT NULL,
    test_type VARCHAR(255) NOT NULL,
    minimum_score INTEGER NULL,
    minimum_grade VARCHAR(255) NULL,
    display_text VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL,
    CONSTRAINT pk_language_cert_requirements PRIMARY KEY (id),
    CONSTRAINT uk_language_cert_requirements_group_test UNIQUE (policy_group_id, test_type),
    CONSTRAINT fk_language_cert_requirements_policy_group
        FOREIGN KEY (policy_group_id) REFERENCES public.language_cert_policy_groups (id)
);

CREATE TABLE IF NOT EXISTS public.department_language_cert_policy_mappings (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    department_code VARCHAR(255) NOT NULL,
    admission_year_from INTEGER NOT NULL,
    admission_year_to INTEGER NOT NULL,
    policy_group_id UUID NULL,
    match_status VARCHAR(255) NOT NULL,
    note VARCHAR(255) NULL,
    CONSTRAINT pk_department_language_cert_policy_mappings PRIMARY KEY (id),
    CONSTRAINT uk_dept_lang_cert_policy_year_range UNIQUE (
        department_code,
        admission_year_from,
        admission_year_to
    ),
    CONSTRAINT fk_dept_lang_cert_policy_mapping_group
        FOREIGN KEY (policy_group_id) REFERENCES public.language_cert_policy_groups (id)
);

CREATE INDEX IF NOT EXISTS idx_dept_lang_cert_policy_lookup
    ON public.department_language_cert_policy_mappings (
        department_code,
        admission_year_from,
        admission_year_to
    );
