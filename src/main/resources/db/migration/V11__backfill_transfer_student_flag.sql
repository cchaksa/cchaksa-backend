-- 기존 포털 입학 구분으로 편입생 여부를 보정한다.
UPDATE public.students
SET is_transfer_student = TRUE
WHERE TRIM(admission_type) = '2'
   OR admission_type LIKE '%편입%';
