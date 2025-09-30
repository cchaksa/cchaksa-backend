# 척척학사 Backend

<div align="center">
  <img src="https://github.com/user-attachments/assets/16764352-ab1e-4cfa-b30e-6287fafde803" width="600"/>
</div>

---

## 📚 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [주요 기능](#주요-기능)
3. [API 문서](#api-문서)
4. [기술 스택](#기술-스택)
5. [아키텍처](#아키텍처)
6. [ERD](#erd)
7. [커밋 컨벤션](#커밋-컨벤션)
8. [관련 블로그 게시글](#관련-블로그-게시글)

---

## 프로젝트 소개

**척척학사**는 학생들이 학교 포털과 연동하여 본인의 이수 현황과 졸업 요건을 손쉽게 확인하고 관리할 수 있도록 도와주는 서비스입니다.

> 기존에는 학생들이 졸업 요건을 직접 확인하며 수작업으로 비교해야 했지만,  
> 척척학사는 이를 **학교 포털과 실시간 연동**, **자동 분석**, **시각적 안내** 기능으로 효율화합니다.

- ✅ **3000명 이상** 수원대 재학생 사용 중
- 🔁 **학교 포털과 실시간 동기화**
- 🧠 **졸업 요건 자동 분석 및 부족 항목 안내**

---

## 주요 기능

<p align="center">
  <img src="https://github.com/user-attachments/assets/84cf31e3-6180-495f-a183-ead0d082b4fc" width="800" alt="IA 구조도" />
  <br/>
  <img src="https://github.com/user-attachments/assets/9fe3eb54-97be-48d9-8d60-c20da2a9e19d" width="800" alt="기능 소개1" />
  <br/>
  <img src="https://github.com/user-attachments/assets/1f464920-a223-45da-9cf1-4d8c2bd04cd6" width="800" alt="기능 소개2" />
</p>

- 포털 연동을 통한 학점·성적·커리큘럼 실시간 동기화
- 졸업 요건과 사용자 학사 정보 자동 비교
- 부족한 학점 및 조건 자동 분석 및 안내

---

## API 문서

👉 [Swagger API 문서 보기](https://dev.api.cchaksa.com/swagger-ui/index.html#/)

---

## 기술 스택

### Back-end
- Java 17
- Spring Boot 3.2.5
- Spring Security, OAuth2
- JPA, Hibernate

### Infra
- AWS ASG(EC2), ALB, Route53, ACM ...
- PostgreSQL (Supabase 연동: BaaS 인증/스토리지 활용)
- Redis (세션 관리, 포털 데이터 캐싱) -> AWS ElastiCache
- Docker, Nginx

### Tools
- Git, GitHub
- Swagger (OpenAPI)
- Gradle, Tomcat

---

## 아키텍처

<p align="center">
  <img width="636" height="744" alt="Image" src="https://github.com/user-attachments/assets/43076c12-de91-479a-9152-7748dca0ea33" />
</p>

---

## ERD

<img src="https://github.com/user-attachments/assets/357f9f0c-44ff-4b7a-80c6-86aceb06853c" width="100%" alt="ERD"/>

---

## 커밋 컨벤션

### 기본 구조
```
type: subject

body (선택)
```

### type 종류
```
feat: 기능 추가
fix: 버그 수정
refactor: 코드 리팩토링
comment: 주석 추가/수정
docs: 문서 수정
test: 테스트 코드 작성 또는 수정
chore: 빌드/패키지 설정 변경
rename: 파일/폴더명 변경
remove: 파일 삭제
style: 코드 포맷팅
!BREAKING CHANGE!: 기존 API 사용에 영향을 주는 변경 (예: 응답 포맷 변경 등)
```

### 커밋 예시
```
feat: 로그인 기능 구현

Email 중복확인 API 개발

---

fix: 사용자 정보 누락 버그 해결

사용자 서비스 코드 수정
```

---

## 관련 블로그 게시글

👉 [척척학사 블로그 시리즈](https://velog.io/@pp8817/series/척척학사)

- [Supabase를 알아보자 With Java, Spring](https://velog.io/@pp8817/DB-Supabase를-알아보자-With-Java-Spring)
- [OIDC 인증, 인가 처리 With Kakao 소셜 로그인](https://velog.io/@pp8817/척척학사-OIDC-인증-인가-처리-With-Kakao-소셜-로그인)
- [Entity 연관관계 분석 및 리팩토링](https://velog.io/@pp8817/척척학사-Entity-연관관계-분석-및-리팩토링)
- [API 응답 모듈 분리 과정](https://velog.io/@pp8817/척척학사-API-응답-모듈-분리-과정)
- [무중단 배포 전략 - 프로젝트에 적용하기](https://velog.io/@pp8817/척척학사-무중단-배포-전략-프로젝트에-적용하기)
- [Next.js 기반 서버를 Spring으로 갈아엎은 이유](https://velog.io/@pp8817/척척학사-Next.js-Spring-Boot-백엔드-마이그레이션-회고)
- [크롤링 로직 비동기 처리 대신 Redis 캐싱 도입한 이유](https://velog.io/@pp8817/척척학사-크롤링-로직-비동기-처리-대신-Redis-캐싱을-도입한-이유)
- [로그 시스템 리빌드: ELK → Grafana Loki](https://velog.io/@pp8817/척척학사-ELK에서-Grafana-Loki-Stack으로-갈아탄-이유와-후기)
- [포털 데이터 Redis 캐싱 전략 도입기](https://velog.io/@pp8817/척척학사-포털-데이터-Redis-캐싱-전략-도입기)
- [복수전공생도 사용할 수 있게 만들기](https://velog.io/@pp8817/척척학사-복수전공생도-사용할-수-있게-만들기)
- [디스크 용량 부족으로 인한 JVM 실행 실패 해결기](https://velog.io/@pp8817/척척학사-디스크-용량-부족-문제)
- [CI/CD 적용 과정 With GitHub Actions](https://velog.io/@pp8817/척척학사-GitHub-Actions-적용)
- [PostgreSQL: duplicate key value violates unique constraint 에러 해결하기](https://velog.io/@pp8817/척척학사-PostgreSQL-duplicate-key-value-violates-unique-constraint-에러-해결하기)
- [포털 연동 시 이수 구분 오류 & 재수강 중복 문제 해결](https://velog.io/@pp8817/척척학사-트러블슈팅-포털-연동-시-이수-구분-오류-재수강-중복-문제-해결)