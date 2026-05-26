-- course_offerings에 미지원 이수구분 원본 문자열을 보존하는 컬럼을 추가한다
ALTER TABLE course_offerings
    ADD COLUMN IF NOT EXISTS raw_faculty_division_name VARCHAR(64);

COMMENT ON COLUMN course_offerings.raw_faculty_division_name IS
    '미지원 이수구분일 때만 보존하는 포털 원본 문자열';
