-- 포털에서 사용하는 선교 8영역 기준정보를 보장한다.
UPDATE public.liberal_arts_area_codes
SET area_name = '8영역',
    is_active = TRUE
WHERE code = 8;

INSERT INTO public.liberal_arts_area_codes (code, is_active, area_name, description)
SELECT 8, TRUE, '8영역', '포털 선교 영역 기준정보'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.liberal_arts_area_codes
    WHERE code = 8
);
