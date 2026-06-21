-- Shared catalog rows required by all lecture evaluation FE test accounts.
-- Run this once before running any per-account create script.

BEGIN;

INSERT INTO public.departments (
    department_code,
    established_department_name,
    created_at,
    updated_at
)
VALUES (
    'LEVALPHA',
    '강의평가 테스트학부',
    NOW(),
    NOW()
)
ON CONFLICT (department_code) DO UPDATE
SET established_department_name = EXCLUDED.established_department_name,
    updated_at = NOW();

INSERT INTO public.professor (
    professor_code,
    professor_name,
    created_at,
    department_id
)
SELECT
    'LEVALPHA-PROF-01',
    '강의평가테스트교수A',
    NOW(),
    d.id
FROM public.departments d
WHERE d.department_code = 'LEVALPHA'
ON CONFLICT (professor_name) DO UPDATE
SET professor_code = EXCLUDED.professor_code,
    department_id = EXCLUDED.department_id;

INSERT INTO public.professor (
    professor_code,
    professor_name,
    created_at,
    department_id
)
SELECT
    'LEVALPHA-PROF-02',
    '강의평가테스트교수B',
    NOW(),
    d.id
FROM public.departments d
WHERE d.department_code = 'LEVALPHA'
ON CONFLICT (professor_name) DO UPDATE
SET professor_code = EXCLUDED.professor_code,
    department_id = EXCLUDED.department_id;

INSERT INTO public.courses (
    course_code,
    course_name,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'LEVALPHA-101',
    '강의평가 테스트 과목 A',
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM public.courses
    WHERE course_code = 'LEVALPHA-101'
);

INSERT INTO public.courses (
    course_code,
    course_name,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'LEVALPHA-102',
    '강의평가 테스트 과목 B',
    NOW(),
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM public.courses
    WHERE course_code = 'LEVALPHA-102'
);

INSERT INTO public.course_offerings (
    subject_establishment_semester,
    is_video_lecture,
    year,
    semester,
    host_department,
    class_section,
    schedule_summary,
    original_area_code,
    points,
    deleted_at,
    evaluation_type_code,
    faculty_division_name,
    raw_faculty_division_name,
    course_id,
    department_id,
    professor_id,
    area_code,
    created_at,
    updated_at
)
SELECT
    10,
    FALSE,
    2026,
    10,
    '강의평가 테스트학부',
    'LEVALPHA-A',
    '월 1,2',
    NULL,
    3,
    NULL,
    'RELATIVE',
    '전선',
    '전선',
    c.id,
    d.id,
    p.id,
    NULL,
    NOW(),
    NOW()
FROM public.courses c
JOIN public.departments d ON d.department_code = 'LEVALPHA'
JOIN public.professor p ON p.professor_name = '강의평가테스트교수A'
WHERE c.course_code = 'LEVALPHA-101'
  AND NOT EXISTS (
      SELECT 1
      FROM public.course_offerings co
      WHERE co.course_id = c.id
        AND co.year = 2026
        AND co.semester = 10
        AND co.class_section = 'LEVALPHA-A'
  );

INSERT INTO public.course_offerings (
    subject_establishment_semester,
    is_video_lecture,
    year,
    semester,
    host_department,
    class_section,
    schedule_summary,
    original_area_code,
    points,
    deleted_at,
    evaluation_type_code,
    faculty_division_name,
    raw_faculty_division_name,
    course_id,
    department_id,
    professor_id,
    area_code,
    created_at,
    updated_at
)
SELECT
    10,
    FALSE,
    2026,
    10,
    '강의평가 테스트학부',
    'LEVALPHA-B',
    '화 3,4',
    NULL,
    3,
    NULL,
    'ABSOLUTE',
    '전선',
    '전선',
    c.id,
    d.id,
    p.id,
    NULL,
    NOW(),
    NOW()
FROM public.courses c
JOIN public.departments d ON d.department_code = 'LEVALPHA'
JOIN public.professor p ON p.professor_name = '강의평가테스트교수B'
WHERE c.course_code = 'LEVALPHA-102'
  AND NOT EXISTS (
      SELECT 1
      FROM public.course_offerings co
      WHERE co.course_id = c.id
        AND co.year = 2026
        AND co.semester = 10
        AND co.class_section = 'LEVALPHA-B'
  );

COMMIT;
