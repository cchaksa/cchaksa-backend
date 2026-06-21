-- Verify lecture evaluation FE test accounts.

SELECT
    u.email,
    u.id AS user_id,
    s.student_id,
    s.student_code,
    sar.year,
    sar.semester,
    sar.lecture_evaluation_status,
    COUNT(sc.id) AS course_count,
    COUNT(sc.id) FILTER (WHERE sc.grade = 'IP') AS ip_course_count,
    COUNT(sc.id) FILTER (WHERE sc.grade IS NOT NULL AND sc.grade <> 'IP') AS completed_course_count
FROM public.users u
JOIN public.students s ON s.user_id = u.id
JOIN public.semester_academic_records sar
    ON sar.student_id = s.student_id
   AND sar.year = 2026
   AND sar.semester = 10
LEFT JOIN public.student_courses sc ON sc.student_id = s.student_id
WHERE u.email IN ('NOT@test.com', 'PENDING@test.com', 'SKIPPED@test.com', 'COMPLETED@test.com')
GROUP BY
    u.email,
    u.id,
    s.student_id,
    s.student_code,
    sar.year,
    sar.semester,
    sar.lecture_evaluation_status
ORDER BY u.email;
