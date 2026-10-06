-- 편입생 졸업진단에서 포털로 확인할 수 없는 학생별 수동 확인 값을 저장한다.
ALTER TABLE public.students
    ADD COLUMN IF NOT EXISTS transfer_registered_semesters INTEGER NULL;

ALTER TABLE public.student_graduation_progress
    ADD COLUMN IF NOT EXISTS graduation_review_fulfilled BOOLEAN NULL;
