# Context Notes

- 사용자는 dev에서 PDF 기반 졸업요건 생성 PoC를 원한다.
- PDF 원본은 서버 런타임에서 직접 읽지 않고, 추출한 JSON 리소스를 repo에 저장한다.
- PDF main table 추출 결과는 총 427개 템플릿이다.
- 2020년은 60개, 2021~2025년은 각 56개, 2026년은 자유전공학부 표를 포함해 87개다.
- DB 저장 대상은 `department_area_requirements`와 `dual_major_requirements`다.
- `graduation_requirements`는 현재 `GraduationService`와 `StudentAcademicRecordService` 흐름에서 직접 사용하지 않는다.
- 단일전공 저장 영역은 `중핵`, `기교`, `선교`, `소교`, `전교`, `전취`, `전핵`, `전선`, `일선`이다.
- 복수전공 저장 영역은 `PRIMARY/복선`, `SECONDARY/복교`, `SECONDARY/복핵`, `SECONDARY/복선`이다.
- PDF의 `전필`은 `전핵` 또는 `복핵`으로 저장한다.
