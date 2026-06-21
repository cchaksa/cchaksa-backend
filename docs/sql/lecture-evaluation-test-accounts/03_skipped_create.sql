-- Create SKIPPED@test.com.
-- State: 2026/10 courses exist, completed grades exist, lecture_evaluation_status = SKIPPED.
-- Prerequisite: run 00_shared_catalog.sql first.

BEGIN;

INSERT INTO public.users (
    id, created_at, updated_at, email, profile_nickname, profile_image,
    is_deleted, portal_connected, connected_at, deleted_at, last_synced_at
)
VALUES (
    '00000000-0000-0000-0000-000000245003', NOW(), NOW(), 'SKIPPED@test.com',
    'LEVALPHA SKIPPED', NULL, FALSE, TRUE, NOW(), NULL, NOW()
);

INSERT INTO public.social_accounts (provider, social_id, email, user_id)
VALUES ('KAKAO', 'levalpha-skipped-245', 'SKIPPED@test.com', '00000000-0000-0000-0000-000000245003');

INSERT INTO public.students (
    student_id, created_at, updated_at, student_code, name, is_graduated,
    admission_type, target_gpa, reconnection_required, admission_year,
    semester_enrolled, is_transfer_student, status, grade_level,
    completed_semesters, department_id, major_id, secondary_major_id, user_id
)
SELECT
    '00000000-0000-0000-0000-000000245103', NOW(), NOW(), 'LEVALPHA245003',
    '강의평가 SKIPPED', FALSE, '신입학', 4.0, FALSE, 2023,
    7, FALSE, '재학', 4, 6, d.id, d.id, NULL,
    '00000000-0000-0000-0000-000000245003'
FROM public.departments d
WHERE d.department_code = 'LEVALPHA';

INSERT INTO public.student_academic_records (
    id, created_at, updated_at, attempted_credits_gpa, percentile,
    cumulative_gpa, total_attempted_credits, total_earned_credits, student_id
)
VALUES (
    '00000000-0000-0000-0000-000000245303', NOW(), NOW(),
    4.10, 90.00, 4.10, 18, 18, '00000000-0000-0000-0000-000000245103'
);

INSERT INTO public.semester_academic_records (
    id, created_at, updated_at, semester, year, total_students, class_rank,
    attempted_credits_gpa, semester_percentile, semester_gpa,
    attempted_credits, earned_credits, student_id, lecture_evaluation_status
)
VALUES (
    '00000000-0000-0000-0000-000000245203', NOW(), NOW(),
    10, 2026, 50, 5, 4.10, 90.00, 4.10, 6, 6,
    '00000000-0000-0000-0000-000000245103', 'SKIPPED'
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
    '00000000-0000-0000-0000-000000245103'
FROM public.course_offerings co
WHERE co.year = 2026
  AND co.semester = 10
  AND co.class_section IN ('LEVALPHA-A', 'LEVALPHA-B');

COMMIT;
