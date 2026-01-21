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

- ✅ **4,000명 이상** 수원대 재학생 사용 중
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

👉 [Swagger API 문서 보기](https://api.cchaksa.com/swagger-ui/index.html?cache=false)

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
- Redis, Caffeine 
- Docker, Nginx

### Tools
- Git, GitHub
- Swagger (OpenAPI)
- Gradle, Tomcat

---

## 아키텍처

<p align="center">
  <img width="1141" height="692" alt="Image" src="https://github.com/user-attachments/assets/192c997a-06ef-4de7-8a64-a32f9c0cea51" />
</p>

---

## 핵심 로직 및 흐름

척척학사의 핵심 기능인 **포털 동기화**, **데이터 적재**, **졸업 요건 분석** 프로세스에 대한 시퀀스 다이어그램입니다.

<details>
<summary><b>1. 포털 동기화 시퀀스 (Click)</b></summary>
<div markdown="1" align="center">
  <img src="https://github.com/user-attachments/assets/78b5a3f9-9c44-452c-a102-67ca14e9a411" width="80%" alt="포털 동기화 시퀀스"/>
</div>
</details>

<details>
<summary><b>2. 학업 데이터 적재 및 매핑 흐름 (Click)</b></summary>
<div markdown="1" align="center">
  <img src="https://github.com/user-attachments/assets/fa8870cb-1e08-4d4f-af28-10c18be4c525" width="80%" alt="학업 데이터 적재 및 매핑 흐름"/>
</div>
</details>

<details>
<summary><b>3. 졸업 요건 조회 및 캐싱 흐름 (Click)</b></summary>
<div markdown="1" align="center">
  <img src="https://github.com/user-attachments/assets/ca167533-2fa4-4022-9d11-953e5671e3f4" width="80%" alt="졸업 요건 조회 및 캐싱 흐름"/>
</div>
</details>

---

## ERD

<div align="center">
  <img src="https://github.com/user-attachments/assets/2aa04ba2-a23b-4474-af69-c29a010b869a" width="80%" alt="ERD"/>
</div>

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

👉 [척척학사 블로그 시리즈 전체보기](https://velog.io/@pp8817/series/척척학사)

<details>
<summary><b>🚀 성능 최적화 & 트러블슈팅</b></summary>
<div markdown="1">

- [1차 쿼리 튜닝: P6Spy 기반 병목 진단과 캐싱 설계](https://velog.io/@pp8817/척척학사-쿼리-튜닝-1차)
- [크롤링 로직 비동기 처리 대신 Redis 캐싱 도입한 이유](https://velog.io/@pp8817/척척학사-크롤링-로직-비동기-처리-대신-Redis-캐싱을-도입한-이유)
- [포털 데이터 Redis 캐싱 전략 도입기](https://velog.io/@pp8817/척척학사-포털-데이터-Redis-캐싱-전략-도입기)
- [부하테스트 (1) t3.micro 서버의 한계 측정](https://velog.io/@pp8817/척척학사-t3.micro-서버의-한계-측정-7-TPS는-버티고-10-TPS는-왜-무너졌나)
- [부하 테스트 (2) Redis vs Local Cache](https://velog.io/@pp8817/척척학사-Redis-vs-Local-Cache-t3.micro에서-캐시는-어디까지-의미가-있을까)
- [부하 테스트 (3) 7 TPS에서 96 TPS까지 — 병목을 제거하며 t3.micro의 한계를 밀어본 기록](https://velog.io/@pp8817/척척학사-인증-필터-캐시-적용으로-병목은-사라졌을까)

</div>
</details>

<details>
<summary><b>🛠️ 아키텍처 & 인프라</b></summary>
<div markdown="1">

- [Next.js 기반 서버를 Spring으로 갈아엎은 이유](https://velog.io/@pp8817/척척학사-Next.js-Spring-Boot-백엔드-마이그레이션-회고)
- [ELK의 오버엔지니어링을 걷어내고: Grafana Loki와 Sentry로 로그 스택 재설계하기](https://velog.io/@pp8817/척척학사-로그-시스템-리빌드-ELK-Grafana-Loki)
- [CI/CD 적용 과정 With GitHub Actions](https://velog.io/@pp8817/척척학사-GitHub-Actions-적용)
- [트래픽이 몰리는 서비스는 어떻게 버텨야 할까: 서버 장애 대응](https://velog.io/@pp8817/척척학사-트래픽이-몰리는-서비스는-어떻게-버텨야-할까-서버-장애-대응)
- [디스크 용량 부족으로 인한 JVM 실행 실패 해결기](https://velog.io/@pp8817/척척학사-디스크-용량-부족-문제)

</div>
</details>

<details>
<summary><b>💡 기능 구현 & 설계</b></summary>
<div markdown="1">

- [Supabase를 알아보자 With Java, Spring](https://velog.io/@pp8817/DB-Supabase를-알아보자-With-Java-Spring)
- [OIDC 인증, 인가 처리 With Kakao 소셜 로그인](https://velog.io/@pp8817/척척학사-OIDC-인증-인가-처리-With-Kakao-소셜-로그인)
- [Entity 연관관계 분석 및 리팩토링](https://velog.io/@pp8817/척척학사-Entity-연관관계-분석-및-리팩토링)
- [API 응답 모듈 분리 과정](https://velog.io/@pp8817/척척학사-API-응답-모듈-분리-과정)
- [복수전공생도 사용할 수 있게 만들기](https://velog.io/@pp8817/척척학사-복수전공생도-사용할-수-있게-만들기)
- [소셜 로그인 정보 변경으로 인한 중복 계정 생성 문제 해결](https://velog.io/@pp8817/척척학사-소셜-로그인-시-email-변경-또는-Provider-변경-시-발생하는-중복-계정-문제-해결기)
- [PostgreSQL Unique Constraint 에러 해결하기](https://velog.io/@pp8817/척척학사-PostgreSQL-duplicate-key-value-violates-unique-constraint-에러-해결하기)
- [포털 연동 시 이수 구분 오류 & 재수강 중복 문제 해결](https://velog.io/@pp8817/척척학사-트러블슈팅-포털-연동-시-이수-구분-오류-재수강-중복-문제-해결)

</div>
</details>