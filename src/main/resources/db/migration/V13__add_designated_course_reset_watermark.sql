-- 지정과목 초기화 이전에 생성된 포털 스냅샷을 차단하는 기준 시각을 저장한다.
ALTER TABLE public.students
    ADD COLUMN designated_courses_reset_at TIMESTAMP WITH TIME ZONE NULL;
