ALTER TABLE public.course_offerings
    ADD COLUMN IF NOT EXISTS raw_faculty_division_name VARCHAR(64);

COMMENT ON COLUMN public.course_offerings.raw_faculty_division_name IS
    '미지원 이수구분일 때만 보존하는 포털 원본 문자열';
