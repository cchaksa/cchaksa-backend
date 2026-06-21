ALTER TABLE public.semester_academic_records
    ADD COLUMN IF NOT EXISTS lecture_evaluation_status VARCHAR(32) NULL;

UPDATE public.semester_academic_records
SET lecture_evaluation_status = CASE
    WHEN lecture_evaluation_completed = TRUE THEN 'COMPLETED'
    WHEN lecture_evaluation_required = TRUE THEN 'PENDING'
    ELSE NULL
END
WHERE lecture_evaluation_status IS NULL;

ALTER TABLE public.semester_academic_records
    ADD CONSTRAINT chk_semester_academic_records_lecture_evaluation_status
    CHECK (
        lecture_evaluation_status IS NULL
        OR lecture_evaluation_status IN ('PENDING', 'SKIPPED', 'COMPLETED')
    );

ALTER TABLE public.semester_academic_records
    DROP COLUMN IF EXISTS lecture_evaluation_required;

ALTER TABLE public.semester_academic_records
    DROP COLUMN IF EXISTS lecture_evaluation_completed;
