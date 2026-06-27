# Context Notes

- 사용자는 졸업요건 문의 처리 자동화를 원한다.
- 현재 문의 흐름은 학번으로 student를 찾고, 학과 ID와 입학년도 기준 졸업요건 존재 여부를 확인한 뒤, 없으면 PDF를 보고 DB에 직접 넣는 방식이다.
- 이번 첫 구현 단위는 DB 자동 생성이 아니라 누락 원인을 진단하는 dev admin API다.
- `전필`은 코드와 DB에서는 `전핵`으로 취급한다.
- 복수전공 요구 학점은 `dual_major_requirements`에 `복교`, `복핵`, `복선`으로 저장하는 정책이다.
- 학과/전공명은 `departments.established_department_name` 같은 name 계열 필드와 매칭한다.
- 학생별 진단 API는 학생 정보를 조회하므로 공개 조회가 아니라 bearer token 필요 endpoint로 둔다.
- 복수전공 진행 가능 여부는 주전공 단일요건과 복수전공 조합요건이 모두 있을 때 true로 계산한다.
