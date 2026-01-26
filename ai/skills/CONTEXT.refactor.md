# Refactoring Context Template

이 작업은 리팩토링이다.
리팩토링의 목적은 동작 보존이다.

Context는 모든 판단의 기준이다.
Context에 명시되지 않은 변경은 금지된다.

---

## 리팩토링 목적
- 대상:
- 리팩토링 이유:
- 기대 효과 (성능 / 가독성 / 유지보수성 등):

## 변경 허용 범위
- 허용되는 변경:
- 금지되는 변경:
- 수정 대상 Layer:
- 수정 금지 Layer:

## 동작 불변 조건 (최우선)
- 외부 API 동작:
- 반환 값 / 예외:
- 트랜잭션 경계:
- 성능 특성 (있다면):

## 기존 문제점 (As-Is)
- 구조적 문제:
- 테스트 문제:
- 기술 부채:

## 목표 구조 (To-Be)
- 구조 변화 요약:
- 책임 이동 여부:
- 삭제/통합 대상:

## 회귀 방지 전략
- 유지해야 할 테스트:
- 추가해야 할 테스트:
- 절대 삭제하면 안 되는 테스트:

## Phase별 체크리스트
- [ ] Phase 1 Context 확정
- [ ] Phase 2 Regression Test 확보
- [ ] Phase 3 Domain/Application 리팩토링
- [ ] Phase 4 Infrastructure 정리
- [ ] Phase 5 Global 영향 점검
- [ ] Phase 6 API 동작 검증

## 생성/변경 파일 목록
- 경로:
- 변경 유형 (수정/이동/삭제):
- 영향 Layer: