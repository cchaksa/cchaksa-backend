-- 사용자 문의와 생성 시점 사용자·학적 스냅샷을 저장한다.
CREATE TABLE public.reports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    submitted_user_id UUID NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    answer TEXT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NULL,
    department_id BIGINT NULL,
    department_name VARCHAR(255) NULL,
    student_code VARCHAR(255) NULL,
    primary_major_id BIGINT NULL,
    primary_major_name VARCHAR(255) NULL,
    secondary_major_id BIGINT NULL,
    secondary_major_name VARCHAR(255) NULL,
    is_transfer_student BOOLEAN NULL,
    admission_year INTEGER NULL,
    graduation_requirement_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reports_user_id
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE NO ACTION,
    CONSTRAINT chk_reports_status
        CHECK (status IN ('PENDING', 'ANSWERED')),
    CONSTRAINT chk_reports_graduation_requirement_status
        CHECK (graduation_requirement_status IN ('AVAILABLE', 'NOT_AVAILABLE', 'UNKNOWN')),
    CONSTRAINT chk_reports_answer_state
        CHECK (
            (status = 'PENDING' AND answer IS NULL AND answered_at IS NULL)
            OR
            (status = 'ANSWERED' AND answer IS NOT NULL
                AND CHAR_LENGTH(TRIM(answer)) > 0 AND answered_at IS NOT NULL)
        )
);

CREATE INDEX idx_reports_user_created_id_desc
    ON public.reports (user_id, created_at DESC, id DESC);
