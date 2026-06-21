-- Create COMPLETED@test.com.
-- State: 2026/10 courses exist, completed grades exist, lecture_evaluation_status = COMPLETED,
--        and course_evaluations/course_evaluation_tags are pre-populated.
-- Prerequisite: run 00_shared_catalog.sql first.

BEGIN;

INSERT INTO public.users (
    id, created_at, updated_at, email, profile_nickname, profile_image,
    is_deleted, portal_connected, connected_at, deleted_at, last_synced_at
)
VALUES (
    '00000000-0000-0000-0000-000000245004', NOW(), NOW(), 'COMPLETED@test.com',
    'LEVALPHA COMPLETED', NULL, FALSE, TRUE, NOW(), NULL, NOW()
);

INSERT INTO public.social_accounts (provider, social_id, email, user_id)
VALUES ('KAKAO', 'levalpha-completed-245', 'COMPLETED@test.com', '00000000-0000-0000-0000-000000245004');

INSERT INTO public.students (
    student_id, created_at, updated_at, student_code, name, is_graduated,
    admission_type, target_gpa, reconnection_required, admission_year,
    semester_enrolled, is_transfer_student, status, grade_level,
    completed_semesters, department_id, major_id, secondary_major_id, user_id
)
SELECT
    '00000000-0000-0000-0000-000000245104', NOW(), NOW(), 'LEVALPHA245004',
    '강의평가 COMPLETED', FALSE, '신입학', 4.0, FALSE, 2023,
    7, FALSE, '재학', 4, 6, d.id, d.id, NULL,
    '00000000-0000-0000-0000-000000245004'
FROM public.departments d
WHERE d.department_code = 'LEVALPHA';

INSERT INTO public.student_academic_records (
    id, created_at, updated_at, attempted_credits_gpa, percentile,
    cumulative_gpa, total_attempted_credits, total_earned_credits, student_id
)
VALUES (
    '00000000-0000-0000-0000-000000245304', NOW(), NOW(),
    4.10, 90.00, 4.10, 18, 18, '00000000-0000-0000-0000-000000245104'
);

INSERT INTO public.semester_academic_records (
    id, created_at, updated_at, semester, year, total_students, class_rank,
    attempted_credits_gpa, semester_percentile, semester_gpa,
    attempted_credits, earned_credits, student_id, lecture_evaluation_status
)
VALUES (
    '00000000-0000-0000-0000-000000245204', NOW(), NOW(),
    10, 2026, 50, 5, 4.10, 90.00, 4.10, 6, 6,
    '00000000-0000-0000-0000-000000245104', 'COMPLETED'
);

INSERT INTO public.student_courses (
    grade, points, is_retake, original_score, created_at,
    is_retake_deleted, offering_id, student_id
)
SELECT
    CASE WHEN co.class_section = 'LEVALPHA-A' THEN 'A+' ELSE 'B+' END,
    3,
    FALSE,
    CASE WHEN co.class_section = 'LEVALPHA-A' THEN 95 ELSE 88 END,
    NOW(),
    FALSE,
    co.id,
    '00000000-0000-0000-0000-000000245104'
FROM public.course_offerings co
WHERE co.year = 2026
  AND co.semester = 10
  AND co.class_section IN ('LEVALPHA-A', 'LEVALPHA-B');

INSERT INTO public.course_evaluations (
    created_at, updated_at, student_id, course_id, professor_id,
    year, semester, review
)
SELECT
    NOW(),
    NOW(),
    s.student_id,
    co.course_id,
    co.professor_id,
    2026,
    10,
    'COMPLETED@test.com seed evaluation'
FROM public.students s
JOIN public.student_courses sc ON sc.student_id = s.student_id
JOIN public.course_offerings co ON co.id = sc.offering_id
WHERE s.student_code = 'LEVALPHA245004';

INSERT INTO public.course_evaluation_tags (course_evaluation_id, tag)
SELECT
    ce.id,
    tag_values.tag
FROM public.course_evaluations ce
JOIN public.students s ON s.student_id = ce.student_id
JOIN LATERAL (
    VALUES ('INTERESTING_LECTURE'), ('LOW_HOMEWORK')
) AS tag_values(tag) ON TRUE
WHERE s.student_code = 'LEVALPHA245004'
  AND ce.year = 2026
  AND ce.semester = 10;

COMMIT;
