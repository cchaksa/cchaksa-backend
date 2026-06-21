-- Create NOT@test.com.
-- State: 2026/10 courses exist, student_courses.grade = IP, lecture_evaluation_status = NOT_RELEASED.
-- Prerequisite: run 00_shared_catalog.sql first.

BEGIN;

INSERT INTO public.users (
    id, created_at, updated_at, email, profile_nickname, profile_image,
    is_deleted, portal_connected, connected_at, deleted_at, last_synced_at
)
VALUES (
    '00000000-0000-0000-0000-000000245001', NOW(), NOW(), 'NOT@test.com',
    'LEVALPHA NOT_RELEASED', NULL, FALSE, TRUE, NOW(), NULL, NOW()
);

INSERT INTO public.social_accounts (provider, social_id, email, user_id)
VALUES ('KAKAO', 'levalpha-not-245', 'NOT@test.com', '00000000-0000-0000-0000-000000245001');

INSERT INTO public.students (
    student_id, created_at, updated_at, student_code, name, is_graduated,
    admission_type, target_gpa, reconnection_required, admission_year,
    semester_enrolled, is_transfer_student, status, grade_level,
    completed_semesters, department_id, major_id, secondary_major_id, user_id
)
SELECT
    '00000000-0000-0000-0000-000000245101', NOW(), NOW(), 'LEVALPHA245001',
    '강의평가 NOT_RELEASED', FALSE, '신입학', 4.0, FALSE, 2023,
    7, FALSE, '재학', 4, 6, d.id, d.id, NULL,
    '00000000-0000-0000-0000-000000245001'
FROM public.departments d
WHERE d.department_code = 'LEVALPHA';

INSERT INTO public.student_academic_records (
    id, created_at, updated_at, attempted_credits_gpa, percentile,
    cumulative_gpa, total_attempted_credits, total_earned_credits, student_id
)
VALUES (
    '00000000-0000-0000-0000-000000245301', NOW(), NOW(),
    4.10, 90.00, 4.10, 18, 18, '00000000-0000-0000-0000-000000245101'
);

INSERT INTO public.semester_academic_records (
    id, created_at, updated_at, semester, year, total_students, class_rank,
    attempted_credits_gpa, semester_percentile, semester_gpa,
    attempted_credits, earned_credits, student_id, lecture_evaluation_status
)
VALUES (
    '00000000-0000-0000-0000-000000245201', NOW(), NOW(),
    10, 2026, 50, 5, 4.10, 90.00, 4.10, 6, 0,
    '00000000-0000-0000-0000-000000245101', 'NOT_RELEASED'
);

INSERT INTO public.student_courses (
    grade, points, is_retake, original_score, created_at,
    is_retake_deleted, offering_id, student_id
)
SELECT
    'IP', 3, FALSE, NULL, NOW(), FALSE, co.id,
    '00000000-0000-0000-0000-000000245101'
FROM public.course_offerings co
WHERE co.year = 2026
  AND co.semester = 10
  AND co.class_section IN ('LEVALPHA-A', 'LEVALPHA-B');

COMMIT;
