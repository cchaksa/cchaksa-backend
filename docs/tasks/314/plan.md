# 이메일 없는 소셜 사용자 로그인 구현 계획

## 목표

OIDC email claim이 없는 신규 사용자도 의미를 왜곡하지 않고 가입·로그인할 수 있도록 한다.

## 작업

- [x] Sentry 최신 이벤트와 현재 로그인 흐름을 확인한다.
- [x] nullable 영향 범위와 공개 API 계약을 확인한다.
- [x] 이메일 유무별 신규·기존 로그인 회귀 테스트를 추가한다.
- [x] User와 SocialAccount의 nullable 저장 테스트를 추가한다.
- [x] 새 Flyway migration과 엔티티 메타데이터 변경을 적용한다.
- [x] JWT null 이메일 동작을 검증한다.
- [x] 관련 테스트와 전체 테스트를 실행한다.
- [ ] `hotfix/314` Draft PR을 `main` 대상으로 올리고 작성자를 Assignee로 지정한다.

## 제외 범위

- 이메일 수집 UI 또는 프로필 수정 API.
- 임의 이메일 생성과 빈 문자열 대체.
- Sentry 이슈 resolve.
