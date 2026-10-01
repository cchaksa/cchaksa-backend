-- 관리자 답변 감사 식별자와 관리자 목록 조회용 B-tree 인덱스를 추가한다.
ALTER TABLE public.reports
    ADD COLUMN answered_by_admin_id UUID NULL;

ALTER TABLE public.reports
    ADD CONSTRAINT fk_reports_answered_by_admin
    FOREIGN KEY (answered_by_admin_id) REFERENCES public.admin_accounts (id) ON DELETE SET NULL;

CREATE INDEX idx_reports_admin_created_id_desc
    ON public.reports (created_at DESC, id DESC);

CREATE INDEX idx_reports_admin_status_created_id_desc
    ON public.reports (status, created_at DESC, id DESC);

CREATE INDEX idx_reports_admin_student_code_created_id_desc
    ON public.reports (student_code, created_at DESC, id DESC);

