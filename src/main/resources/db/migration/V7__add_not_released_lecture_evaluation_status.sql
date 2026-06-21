ALTER TABLE public.semester_academic_records
    DROP CONSTRAINT IF EXISTS chk_semester_academic_records_lecture_evaluation_status;

ALTER TABLE public.semester_academic_records
    ADD CONSTRAINT chk_semester_academic_records_lecture_evaluation_status
    CHECK (
        lecture_evaluation_status IS NULL
        OR lecture_evaluation_status IN ('NOT_RELEASED', 'PENDING', 'SKIPPED', 'COMPLETED')
    );

UPDATE public.semester_academic_records sar
SET lecture_evaluation_status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM public.student_courses sc
        JOIN public.course_offerings co ON co.id = sc.offering_id
        WHERE sc.student_id = sar.student_id
          AND co.year = sar.year
          AND co.semester = sar.semester
          AND sc.grade IS NOT NULL
          AND sc.grade <> 'IP'
    ) THEN 'PENDING'
    ELSE 'NOT_RELEASED'
END
WHERE sar.lecture_evaluation_status IS NULL
  AND EXISTS (
      SELECT 1
      FROM public.student_courses sc
      JOIN public.course_offerings co ON co.id = sc.offering_id
      WHERE sc.student_id = sar.student_id
        AND co.year = sar.year
        AND co.semester = sar.semester
  );
