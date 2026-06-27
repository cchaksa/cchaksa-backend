# Context Notes

- PR #285 리뷰는 Gemini Code Assist의 inline comment 7개다.
- 템플릿 리소스는 정적 JSON이므로 연도별 캐싱이 타당하다.
- 중복 signature는 클라이언트 입력 오류가 아니라 서버 리소스 정합성 문제이므로 로그를 남긴다.
- 진단 API와 생성 API는 입학년도, 주전공/학과 누락을 동일하게 `INVALID_ARGUMENT`로 처리한다.
- 생성 후 캐시는 템플릿이 아니라 repository에서 조회한 DB 최종 row로 채운다.
- `studentCode` 공통 유틸 분리는 이번 리뷰 대응에서 제외한다.
- 관련 테스트 묶음은 통과했고, 전체 테스트로 최종 검증한다.
- 템플릿 누락은 요청 데이터 부재 가능성이 있어 `INVALID_ARGUMENT`를 유지하되 학과와 입학년도 로그를 남긴다.
