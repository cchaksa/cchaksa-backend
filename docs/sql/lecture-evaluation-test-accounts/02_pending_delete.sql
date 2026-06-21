-- Delete only PENDING@test.com and dependent test rows.

BEGIN;

DELETE FROM public.course_evaluation_tags cet
USING public.course_evaluations ce
JOIN public.students s ON s.student_id = ce.student_id
JOIN public.users u ON u.id = s.user_id
WHERE cet.course_evaluation_id = ce.id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.course_evaluations ce
USING public.students s
JOIN public.users u ON u.id = s.user_id
WHERE ce.student_id = s.student_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.student_courses sc
USING public.students s
JOIN public.users u ON u.id = s.user_id
WHERE sc.student_id = s.student_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.semester_academic_records sar
USING public.students s
JOIN public.users u ON u.id = s.user_id
WHERE sar.student_id = s.student_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.student_academic_records ar
USING public.students s
JOIN public.users u ON u.id = s.user_id
WHERE ar.student_id = s.student_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.student_graduation_progress sgp
USING public.students s
JOIN public.users u ON u.id = s.user_id
WHERE sgp.student_id = s.student_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.scrape_job_outbox outbox
USING public.scrape_jobs job
JOIN public.users u ON u.id = job.user_id
WHERE outbox.job_id = job.job_id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.scrape_jobs job
USING public.users u
WHERE job.user_id = u.id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.refresh_token rt
USING public.users u
WHERE rt.user_id = u.id::text
  AND u.email = 'PENDING@test.com';

DELETE FROM public.social_accounts sa
USING public.users u
WHERE sa.user_id = u.id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.students s
USING public.users u
WHERE s.user_id = u.id
  AND u.email = 'PENDING@test.com';

DELETE FROM public.users
WHERE email = 'PENDING@test.com';

COMMIT;
