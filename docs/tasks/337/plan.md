# 포털 지정과목 저장 및 최신 스냅샷 동기화 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 포털의 `designatedCourses`를 원본 순서와 중복을 보존해 학생별로 저장하고, 최초 연동·새로고침·초기화·탈퇴·계정 통합에서 최신 스냅샷 계약을 일관되게 적용한다.

**Architecture:** Raw DTO에서는 필드 누락·`null`과 빈 배열을 구분해 `DesignatedCourseSnapshot(received, courses)`로 변환한다. 지정과목은 수강·취득 데이터와 분리된 `student_designated_courses`에 저장하고, 학생 행의 마지막 스냅샷 버전과 `ScrapeJob.createdAt`을 비관적 잠금 아래 비교해 늦게 도착한 결과를 무시한다. 삭제·재저장·버전 갱신은 기존 포털 후처리 트랜잭션 안에서 함께 커밋하거나 함께 롤백한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Jackson, Spring Data JPA, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ.

## Global Constraints

- 이슈 #337의 `designatedCourses` 필드 계약을 기준으로 한다.
- 필드 누락 또는 `null`은 기존 목록과 스냅샷 버전을 유지한다.
- 빈 배열은 기존 목록을 삭제하고 요청 스냅샷 버전을 기록한다.
- 값이 있는 배열은 기존 목록을 원본 순서와 중복까지 포함해 교체한다.
- 지정과목으로 `Course`, `CourseOffering`, `StudentCourse`를 만들거나 취득학점을 변경하지 않는다.
- `point`, `cretGainYear`의 누락·빈 문자열은 `null`, 잘못된 숫자는 `SCRAPE_RESULT_SCHEMA_INVALID`로 처리한다.
- 지정과목 행의 비어 있지 않은 `sno`가 최상위 학생번호와 다르면 전체 payload를 거부한다.
- V12/V13 스키마는 현재 운영 중인 이전 Lambda가 새 테이블과 nullable 컬럼을 무시해도 동작하도록 additive migration으로 작성한다.
- 새 Java 파일은 첫 줄에 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 공개 API 응답은 이번 이슈에서 변경하지 않는다.

## CodeRabbit 리뷰 후속 결정

- 학생 데이터 초기화와 탈퇴는 학생 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 지정과목과 스냅샷 상태를 함께 정리한다.
- 명시적인 초기화·탈퇴 시점 이전의 늦은 스냅샷을 차단하기 위해 `students.designated_courses_reset_at` 워터마크를 V13으로 추가한다. 포털 최초 재연동 초기화는 같은 트랜잭션의 현재 스냅샷을 허용해야 하므로 워터마크를 설정하지 않는다.
- 지정과목 변경으로 인한 로컬 캐시 삭제는 트랜잭션 커밋 후 수행해 롤백 또는 동시 조회가 이전 데이터를 캐시에 재저장하는 경합을 줄인다.
- 포털 동기화 진입부에서도 학생 행을 잠그고, 탈퇴한 사용자의 지정과목 콜백은 저장하지 않는다.
- 지정과목 배열의 `null` 행과 필수 식별 코드(`orgClsCd`, `subjtCd`) 누락은 스키마 오류로 거부한다.

---

### Task 1: Raw DTO와 내부 스냅샷 계약 추가

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/infrastructure/portal/dto/raw/RawPortalDesignatedCourseDto.java`
- Modify: `src/main/java/com/chukchuk/haksa/infrastructure/portal/dto/raw/RawPortalData.java`
- Create: `src/main/java/com/chukchuk/haksa/infrastructure/portal/model/DesignatedCourseData.java`
- Create: `src/main/java/com/chukchuk/haksa/infrastructure/portal/model/DesignatedCourseSnapshot.java`
- Modify: `src/main/java/com/chukchuk/haksa/infrastructure/portal/model/PortalData.java`
- Modify: `src/main/java/com/chukchuk/haksa/infrastructure/portal/mapper/PortalDataMapper.java`
- Modify: `src/test/java/com/chukchuk/haksa/infrastructure/portal/mapper/PortalDataMapperTests.java`
- Create: `src/test/resources/fixtures/portal/designated-courses.json`

**Interfaces:**
- Consumes: JSON 필드 `designatedCourses`와 최상위 `studentInfo.sno`.
- Produces: `DesignatedCourseSnapshot(boolean received, List<DesignatedCourseData> courses)`.

- [x] **Step 1: 실제 스크래퍼 형태의 실패 테스트와 fixture를 추가한다.**

fixture에는 숫자·문자열 숫자, nullable 원본 필드, 동일 과목 코드의 중복 행을 포함한다.

```json
{
  "studentInfo": {
    "sno": "17019013",
    "studNm": "홍길동",
    "enscYear": "2021",
    "enscSmrCd": "10",
    "enscDvcd": "2"
  },
  "semesters": [],
  "academicRecords": {
    "listSmrCretSumTabYearSmr": [],
    "selectSmrCretSumTabSjTotal": {
      "gainPoint": "120",
      "applPoint": "130",
      "gainAvmk": "3.8",
      "gainTavgPont": "90"
    }
  },
  "designatedCourses": [
    {
      "orgClsCd": "01",
      "subjtCd": "C101",
      "subjtNm": "자료구조",
      "point": 3,
      "precpResnCd": "TRANSFER",
      "cretGainYear": "2024",
      "cretSmrNm": "1학기",
      "sno": "17019013"
    },
    {
      "orgClsCd": "01",
      "subjtCd": "C101",
      "subjtNm": "자료구조",
      "point": "",
      "precpResnCd": "TRANSFER",
      "cretGainYear": null,
      "cretSmrNm": "1학기",
      "sno": "17019013"
    }
  ]
}
```

`PortalDataMapperTests`에서 다음 계약을 각각 검증한다.

```java
assertThat(mapped.designatedCourses().received()).isTrue();
assertThat(mapped.designatedCourses().courses())
    .extracting(DesignatedCourseData::sourceOrder)
    .containsExactly(0, 1);
assertThat(mapped.designatedCourses().courses().get(0).point()).isEqualTo(3);
assertThat(mapped.designatedCourses().courses().get(1).point()).isNull();
```

필드 누락과 명시적 `null`은 `received=false`, `[]`는 `received=true`와 빈 목록이어야 한다. `point="3학점"`, `cretGainYear="20X4"`, 최상위 학번과 다른 `sno`는 `IllegalArgumentException`이어야 한다.

- [x] **Step 2: focused test가 현재 모델 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon
```

Expected: `designatedCourses` 접근자와 내부 모델이 없어 컴파일 또는 assertion이 실패한다.

- [x] **Step 3: Raw DTO와 내부 불변 모델을 추가한다.**

`RawPortalDesignatedCourseDto`의 숫자 후보 필드는 빈 문자열도 받을 수 있도록 문자열로 수신한다. Jackson의 scalar-to-string 변환으로 JSON 숫자도 함께 수용한다.

```java
public record RawPortalDesignatedCourseDto(
    String orgClsCd,
    String subjtCd,
    String subjtNm,
    String point,
    String precpResnCd,
    String cretGainYear,
    String cretSmrNm,
    String sno) {}
```

```java
public record DesignatedCourseData(
    String orgClsCd,
    String subjtCd,
    String subjtNm,
    Integer point,
    String precpResnCd,
    Integer cretGainYear,
    String cretSmrNm,
    String sno,
    int sourceOrder) {}
```

```java
public record DesignatedCourseSnapshot(
    boolean received, List<DesignatedCourseData> courses) {
  public DesignatedCourseSnapshot {
    courses = List.copyOf(courses);
  }

  public static DesignatedCourseSnapshot notReceived() {
    return new DesignatedCourseSnapshot(false, List.of());
  }

  public static DesignatedCourseSnapshot received(List<DesignatedCourseData> courses) {
    return new DesignatedCourseSnapshot(true, courses);
  }
}
```

`RawPortalData`에는 nullable `List<RawPortalDesignatedCourseDto> designatedCourses`를 추가하고 `PortalData`에는 non-null `DesignatedCourseSnapshot designatedCourses`를 추가한다.

- [x] **Step 4: mapper에서 수신 여부, 숫자, 순서와 학생번호를 검증한다.**

```java
private static DesignatedCourseSnapshot toDesignatedCourseSnapshot(
    List<RawPortalDesignatedCourseDto> rawCourses, String studentCode) {
  if (rawCourses == null) {
    return DesignatedCourseSnapshot.notReceived();
  }

  List<DesignatedCourseData> courses = new ArrayList<>();
  for (int sourceOrder = 0; sourceOrder < rawCourses.size(); sourceOrder++) {
    RawPortalDesignatedCourseDto raw = rawCourses.get(sourceOrder);
    if (raw.sno() != null
        && !raw.sno().isBlank()
        && !raw.sno().trim().equals(studentCode)) {
      throw new IllegalArgumentException("지정과목 학생번호가 최상위 학생번호와 다릅니다.");
    }
    courses.add(
        new DesignatedCourseData(
            raw.orgClsCd(),
            raw.subjtCd(),
            raw.subjtNm(),
            parseNullableInteger(raw.point(), "point"),
            raw.precpResnCd(),
            parseNullableInteger(raw.cretGainYear(), "cretGainYear"),
            raw.cretSmrNm(),
            raw.sno(),
            sourceOrder));
  }
  return DesignatedCourseSnapshot.received(courses);
}
```

`parseNullableInteger`는 `null`·blank만 `null`로 반환하고 그 외 파싱 실패는 필드명을 포함한 `IllegalArgumentException`을 던진다. `PortalCallbackPostProcessor`의 기존 RuntimeException 처리 경로가 이를 `SCRAPE_RESULT_SCHEMA_INVALID`로 변환한다.

- [x] **Step 5: focused test를 다시 실행해 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: 입력 계약 변경을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/infrastructure/portal src/test/java/com/chukchuk/haksa/infrastructure/portal src/test/resources/fixtures/portal/designated-courses.json
git commit -m "337 feat: 포털 지정과목 입력 계약 추가"
```

### Task 2: V12 저장 스키마와 JPA 모델 추가

**Files:**
- Create: `src/main/resources/db/migration/V12__create_student_designated_courses.sql`
- Create: `src/main/java/com/chukchuk/haksa/domain/student/model/StudentDesignatedCourse.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/student/repository/StudentDesignatedCourseRepository.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/student/model/Student.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/student/repository/StudentRepository.java`
- Modify: `src/test/java/com/chukchuk/haksa/global/db/FlywayMigrationTest.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/student/repository/StudentDesignatedCourseRepositoryTests.java`

**Interfaces:**
- Consumes: `DesignatedCourseData`와 학생 UUID.
- Produces: `student_designated_courses` 원본 행, `students.designated_courses_snapshot_version`, 학생 단위 잠금·삭제·정렬 조회 저장소.

- [x] **Step 1: V12 스키마 기대값을 실패 테스트로 추가한다.**

`FlywayMigrationTest`의 fresh migration 기대 버전을 V12까지 확장하고 다음을 검증한다.

```java
assertThat(hasTable(connection, "student_designated_courses")).isTrue();
assertThat(hasColumn(connection, "students", "designated_courses_snapshot_version")).isTrue();
assertThat(isNullable(connection, "students", "designated_courses_snapshot_version")).isTrue();
assertThat(foreignKeyDeleteRule(
        connection, "student_designated_courses", "fk_student_designated_courses_student_id"))
    .isEqualTo("CASCADE");
```

별도 migration 테스트에서는 동일 학생의 같은 `source_order` 중복 insert가 실패하고, 학생 삭제 시 지정과목 행도 사라지는지 확인한다.

- [x] **Step 2: migration test가 V12 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest --stacktrace --no-daemon
```

Expected: V12 테이블과 컬럼이 없어 실패한다.

- [x] **Step 3: backward-compatible V12 migration을 추가한다.**

```sql
-- 학생별 포털 지정과목 원본과 최신 스냅샷 버전을 저장한다.
ALTER TABLE public.students
    ADD COLUMN designated_courses_snapshot_version TIMESTAMP WITH TIME ZONE NULL;

CREATE TABLE public.student_designated_courses (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    student_id UUID NOT NULL,
    source_order INTEGER NOT NULL,
    org_cls_cd VARCHAR(255) NULL,
    subjt_cd VARCHAR(255) NULL,
    subjt_nm VARCHAR(255) NULL,
    point INTEGER NULL,
    precp_resn_cd VARCHAR(255) NULL,
    cret_gain_year INTEGER NULL,
    cret_smr_nm VARCHAR(255) NULL,
    sno VARCHAR(255) NULL,
    CONSTRAINT pk_student_designated_courses PRIMARY KEY (id),
    CONSTRAINT uk_student_designated_courses_student_order
        UNIQUE (student_id, source_order),
    CONSTRAINT fk_student_designated_courses_student_id
        FOREIGN KEY (student_id) REFERENCES public.students (student_id) ON DELETE CASCADE
);

```

기존 테이블이나 기존 컬럼의 제약은 변경하지 않는다. `(student_id, source_order)` unique constraint가 만드는 인덱스의 선두 컬럼으로 학생별 삭제·조회가 가능하므로 중복 `student_id` 인덱스는 추가하지 않는다.

- [x] **Step 4: 엔티티, 저장소와 학생 스냅샷 상태를 추가한다.**

`StudentDesignatedCourse`는 별도 `Course` 연관 없이 원본 필드와 `Student`만 가진다. 생성자는 `Student`와 `DesignatedCourseData`를 받아 모든 필드를 그대로 복사한다.

```java
@Entity
@Table(
    name = "student_designated_courses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_student_designated_courses_student_order",
            columnNames = {"student_id", "source_order"}))
public class StudentDesignatedCourse {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @Column(name = "source_order", nullable = false)
  private int sourceOrder;

  @Column(name = "org_cls_cd")
  private String orgClsCd;

  @Column(name = "subjt_cd")
  private String subjtCd;

  @Column(name = "subjt_nm")
  private String subjtNm;

  @Column(name = "point")
  private Integer point;

  @Column(name = "precp_resn_cd")
  private String precpResnCd;

  @Column(name = "cret_gain_year")
  private Integer cretGainYear;

  @Column(name = "cret_smr_nm")
  private String cretSmrNm;

  @Column(name = "sno")
  private String sno;

  public StudentDesignatedCourse(Student student, DesignatedCourseData course) {
    this.student = student;
    this.sourceOrder = course.sourceOrder();
    this.orgClsCd = course.orgClsCd();
    this.subjtCd = course.subjtCd();
    this.subjtNm = course.subjtNm();
    this.point = course.point();
    this.precpResnCd = course.precpResnCd();
    this.cretGainYear = course.cretGainYear();
    this.cretSmrNm = course.cretSmrNm();
    this.sno = course.sno();
  }
}
```

```java
public interface StudentDesignatedCourseRepository
    extends JpaRepository<StudentDesignatedCourse, Long> {
  List<StudentDesignatedCourse> findAllByStudentIdOrderBySourceOrder(UUID studentId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM StudentDesignatedCourse c WHERE c.student.id = :studentId")
  int deleteAllByStudentId(@Param("studentId") UUID studentId);
}
```

`Student`에는 nullable `Instant designatedCoursesSnapshotVersion`과 다음 도메인 메서드를 추가한다.

```java
public boolean canApplyDesignatedCourseSnapshot(Instant requestedVersion) {
  return designatedCoursesSnapshotVersion == null
      || requestedVersion.isAfter(designatedCoursesSnapshotVersion);
}

public void updateDesignatedCourseSnapshotVersion(Instant snapshotVersion) {
  this.designatedCoursesSnapshotVersion = snapshotVersion;
}

public void clearDesignatedCourseSnapshotVersion() {
  this.designatedCoursesSnapshotVersion = null;
}
```

`StudentRepository`에는 `@Lock(LockModeType.PESSIMISTIC_WRITE)`와 `user.id` 조건을 사용하는 `findForUpdateByUserId(UUID userId)`를 추가한다.

- [x] **Step 5: migration과 repository 테스트를 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest --tests com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepositoryTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`. 조회 결과가 `source_order` 순서와 중복 행을 보존하고, 삭제 메서드가 학생의 행만 제거한다.

- [x] **Step 6: 저장 모델을 커밋한다.**

```bash
git add src/main/resources/db/migration/V12__create_student_designated_courses.sql src/main/java/com/chukchuk/haksa/domain/student src/test/java/com/chukchuk/haksa/global/db/FlywayMigrationTest.java src/test/java/com/chukchuk/haksa/domain/student/repository/StudentDesignatedCourseRepositoryTests.java
git commit -m "337 feat: 학생 지정과목 저장 스키마 추가"
```

### Task 3: 버전 기반 지정과목 동기화 서비스 추가

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseService.java`
- Create: `src/test/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseServiceTests.java`
- Create: `src/test/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseServiceIntegrationTests.java`

**Interfaces:**
- Consumes: `sync(UUID activeUserId, DesignatedCourseSnapshot snapshot, Instant snapshotVersion)`.
- Produces: 누락 no-op, 빈 배열 삭제, 최신 배열 교체, 오래되거나 같은 버전 no-op.

- [x] **Step 1: 동기화 정책 단위 테스트를 먼저 작성한다.**

Mockito로 다음 분기를 독립 검증한다.

```java
syncDesignatedCourseService.sync(userId, DesignatedCourseSnapshot.notReceived(), version);
verifyNoInteractions(studentRepository, designatedCourseRepository);
```

```java
when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(student));
when(student.canApplyDesignatedCourseSnapshot(version)).thenReturn(true);

syncDesignatedCourseService.sync(
    userId, DesignatedCourseSnapshot.received(List.of(first, duplicate)), version);

verify(designatedCourseRepository).deleteAllByStudentId(studentId);
verify(designatedCourseRepository).saveAll(courseCaptor.capture());
assertThat(courseCaptor.getValue()).hasSize(2);
verify(student).updateDesignatedCourseSnapshotVersion(version);
```

빈 배열은 삭제 후 `saveAll`을 호출하지 않고 버전은 갱신한다. 저장 버전보다 오래되거나 같은 요청은 삭제·저장·버전 갱신을 모두 건너뛴다. 학생 부재는 `STUDENT_NOT_FOUND`를 던진다.

- [x] **Step 2: focused test가 서비스 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceTests --stacktrace --no-daemon
```

Expected: `SyncDesignatedCourseService`가 없어 컴파일이 실패한다.

- [x] **Step 3: 최소 동기화 서비스를 구현한다.**

```java
@Service
@RequiredArgsConstructor
public class SyncDesignatedCourseService {
  private final StudentRepository studentRepository;
  private final StudentDesignatedCourseRepository designatedCourseRepository;

  @Transactional
  public void sync(
      UUID userId, DesignatedCourseSnapshot snapshot, Instant snapshotVersion) {
    if (!snapshot.received()) {
      return;
    }
    if (snapshotVersion == null) {
      throw new IllegalArgumentException("지정과목 스냅샷 버전이 없습니다.");
    }

    Student student =
        studentRepository
            .findForUpdateByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
    if (!student.canApplyDesignatedCourseSnapshot(snapshotVersion)) {
      return;
    }

    designatedCourseRepository.deleteAllByStudentId(student.getId());
    if (!snapshot.courses().isEmpty()) {
      designatedCourseRepository.saveAll(
          snapshot.courses().stream()
              .map(course -> new StudentDesignatedCourse(student, course))
              .toList());
    }
    student.updateDesignatedCourseSnapshotVersion(snapshotVersion);
  }
}
```

- [x] **Step 4: 트랜잭션과 순서 역전 통합 테스트를 추가한다.**

실제 JPA repository를 사용해 다음을 검증한다.

- 최신 버전 `2026-08-30T02:00:00Z` 저장 뒤 과거 버전 `2026-08-30T01:00:00Z`가 도착해도 최신 행과 버전이 유지된다.
- 기존 행 삭제 후 `saveAll` 단계에서 unique constraint 오류를 발생시키면 트랜잭션 롤백으로 기존 행과 버전이 유지된다.
- 두 개의 동일 과목 코드 행은 서로 다른 `source_order`로 모두 저장된다.
- 지정과목 동기화 전후 `student_courses`, `courses`, `course_offerings`, `student_academic_records.total_earned_credits`의 row와 값이 변하지 않는다.

- [x] **Step 5: 단위·통합 테스트를 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceIntegrationTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: 동기화 서비스를 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseService.java src/test/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseServiceTests.java src/test/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseServiceIntegrationTests.java
git commit -m "337 feat: 지정과목 최신 스냅샷 동기화"
```

### Task 4: ScrapeJob 버전을 최초 연동과 새로고침에 연결

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/application/portal/ScrapeResultCallbackTxService.java`
- Modify: `src/main/java/com/chukchuk/haksa/application/portal/PortalSyncService.java`
- Modify: `src/test/java/com/chukchuk/haksa/application/portal/PortalSyncServiceTests.java`
- Create: `src/test/java/com/chukchuk/haksa/application/portal/ScrapeResultCallbackTxServiceTests.java`
- Modify: `src/test/java/com/chukchuk/haksa/application/portal/PortalCallbackPostProcessorTests.java`

**Interfaces:**
- Consumes: `ScrapeJob.getCreatedAt()`.
- Produces: `PortalSyncService.syncWithPortal(..., Instant snapshotVersion)`과 `refreshFromPortal(..., Instant snapshotVersion)`.

- [x] **Step 1: createdAt 전달과 두 동기화 분기의 실패 테스트를 작성한다.**

```java
verify(portalSyncService)
    .syncWithPortal(userId, portalData, job.getCreatedAt());
```

```java
verify(syncDesignatedCourseService)
    .sync(activeUserId, portalData.designatedCourses(), snapshotVersion);
```

LINK가 기존 계정 병합 후 REFRESH 분기로 전환되는 경우에도 병합 뒤의 `activeUserId`와 같은 `snapshotVersion`을 사용해야 한다.

- [x] **Step 2: focused test가 기존 메서드 signature 때문에 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackTxServiceTests --stacktrace --no-daemon
```

Expected: snapshot version 인자가 없어 컴파일 또는 Mockito 검증이 실패한다.

- [x] **Step 3: callback의 job 생성 시각을 PortalSyncService까지 전달한다.**

`ScrapeResultCallbackTxService.completeSuccess`에서 잠금 조회한 job의 `createdAt`을 사용한다.

```java
Instant snapshotVersion = job.getCreatedAt();
if (operationType == ScrapeJobOperationType.LINK) {
  portalSyncService.syncWithPortal(userId, portalData, snapshotVersion);
} else {
  portalSyncService.refreshFromPortal(userId, portalData, snapshotVersion);
}
```

`PortalSyncService`는 학생 프로필과 학업 이력 동기화가 성공한 뒤, 포털 연결 완료 표시 전에 다음 호출을 수행한다.

```java
syncDesignatedCourseService.sync(
    activeUserId, portalData.designatedCourses(), snapshotVersion);
```

누락 스냅샷은 서비스 내부에서 즉시 반환하므로 구버전 scraper payload 흐름은 기존과 같다.

- [x] **Step 4: callback·최초 연동·새로고침 테스트를 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackTxServiceTests --tests com.chukchuk.haksa.application.portal.PortalCallbackPostProcessorTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: 포털 동기화 연결을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/application/portal src/test/java/com/chukchuk/haksa/application/portal
git commit -m "337 feat: 지정과목 동기화 흐름 연결"
```

### Task 5: 학생 초기화·탈퇴·재연동·계정 통합 생명주기 반영

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/domain/student/service/StudentService.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/student/service/StudentDeletionService.java`
- Modify: `src/test/java/com/chukchuk/haksa/domain/student/service/StudentServiceUnitTests.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/student/service/StudentDeletionServiceUnitTests.java`
- Modify: `src/test/java/com/chukchuk/haksa/domain/user/repository/UserPortalConnectionRepositoryTests.java`
- Modify: `src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceIntegrationTest.java`

**Interfaces:**
- Consumes: 기존 `/students/reset`, 재연동 시 기존 학생 재사용, 회원 탈퇴, 학번 기반 계정 병합 흐름.
- Produces: reset·탈퇴 시 지정과목 제거와 버전 초기화, 계정 병합 시 학생 소유 데이터 유지.

- [x] **Step 1: 생명주기 실패 테스트를 추가한다.**

`StudentServiceUnitTests`의 reset 검증을 다음과 같이 확장한다.

```java
when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

studentService.resetBy(studentId);

verify(studentDesignatedCourseRepository).deleteAllByStudentId(studentId);
verify(student).clearDesignatedCourseSnapshotVersion();
```

`StudentDeletionServiceUnitTests`는 익명화 전에 지정과목 bulk delete와 버전 초기화가 실행되는지 확인한다. `UserPortalConnectionRepositoryTests`는 기존 학생 재사용 시 `resetBy` 위임으로 과거 지정과목이 제거됨을 유지한다.

`UserServiceIntegrationTest`는 지정과목이 있는 기존 학생을 새 사용자로 계정 병합한 뒤 같은 학생 UUID와 지정과목 행이 유지되고, 기존 사용자만 삭제되는지 확인한다.

- [x] **Step 2: focused test가 누락된 lifecycle 처리로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.student.service.StudentServiceUnitTests --tests com.chukchuk.haksa.domain.student.service.StudentDeletionServiceUnitTests --tests com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepositoryTests --tests com.chukchuk.haksa.domain.user.service.UserServiceIntegrationTest --stacktrace --no-daemon
```

Expected: 지정과목 repository 호출과 버전 초기화가 없어 실패한다.

- [x] **Step 3: reset과 탈퇴 경로에 지정과목 정리를 추가한다.**

`StudentService.resetBy`는 지정과목 bulk delete 후 학생의 버전을 `null`로 변경한다. `StudentDeletionService.anonymizeByStudent`도 학생을 실제 삭제하지 않고 익명화하므로 FK cascade에만 의존하지 않고 같은 처리를 명시적으로 수행한다.

```java
studentDesignatedCourseRepository.deleteAllByStudentId(studentId);
student.clearDesignatedCourseSnapshotVersion();
```

계정 통합은 지정과목이 `student_id`를 참조하고 기존 `Student` 자체를 새 사용자로 이동하므로 별도 복사·삭제 코드를 추가하지 않는다.

- [x] **Step 4: 생명주기 테스트를 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.student.service.StudentServiceUnitTests --tests com.chukchuk.haksa.domain.student.service.StudentDeletionServiceUnitTests --tests com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepositoryTests --tests com.chukchuk.haksa.domain.user.service.UserServiceIntegrationTest --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: 생명주기 변경을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/student/service src/test/java/com/chukchuk/haksa/domain/student/service src/test/java/com/chukchuk/haksa/domain/user
git commit -m "337 feat: 지정과목 학생 생명주기 반영"
```

### Task 6: 실제 payload 회귀와 전체 트랜잭션 검증

**Files:**
- Modify: `src/test/java/com/chukchuk/haksa/application/portal/PortalCallbackPostProcessorTests.java`
- Modify: `src/test/java/com/chukchuk/haksa/application/portal/SyncDesignatedCourseServiceIntegrationTests.java`
- Modify: `docs/tasks/337/plan.md`

**Interfaces:**
- Consumes: Task 1의 실제 JSON fixture와 Task 2~5의 전체 저장 흐름.
- Produces: 이슈 #337의 누락·null·빈 배열·교체·순서 역전·rollback 인수 조건에 대한 회귀 증거.

- [x] **Step 1: 실제 JSON fixture를 callback 후처리 경계에서 검증한다.**

`PortalCallbackPostProcessorTests`에서 fixture를 읽어 `ScrapeResultCallbackTxService.completeSuccess`로 전달된 `PortalData`를 capture하고 다음을 확인한다.

```java
assertThat(portalDataCaptor.getValue().designatedCourses().received()).isTrue();
assertThat(portalDataCaptor.getValue().designatedCourses().courses())
    .extracting(DesignatedCourseData::subjtCd)
    .containsExactly("C101", "C101");
```

잘못된 숫자와 학생번호 불일치 fixture 변형은 `SCRAPE_RESULT_SCHEMA_INVALID`를 발생시키고 portal sync를 호출하지 않아야 한다.

- [x] **Step 2: 인수 조건별 focused test를 실행한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --tests com.chukchuk.haksa.application.portal.PortalCallbackPostProcessorTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceIntegrationTests --tests com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepositoryTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`이며 다음 항목이 모두 별도 테스트명으로 확인된다.

- 필드 누락과 `null` 유지.
- 빈 배열 삭제와 버전 갱신.
- 값이 있는 배열 교체.
- 순서와 중복 보존.
- 오래된 요청 무시.
- 학생번호 불일치와 잘못된 숫자 거부.
- 저장 실패 전체 롤백.
- `StudentCourse`와 취득학점 불변.

- [x] **Step 3: formatter와 전체 품질 게이트를 실행한다.**

Run:

```bash
./gradlew spotlessApply --no-daemon
./gradlew check --stacktrace --no-daemon
git diff --check
```

Expected: `BUILD SUCCESSFUL`, `git diff --check` exit 0.

- [x] **Step 4: 실제 검증 결과와 발견 사항을 이 문서 하단에 기록한다.**

명령, 결과, 테스트 수, 스키마 버전, 남은 위험을 `## Verification Notes`에 기록한다. 실패한 검사를 생략하지 않고 원인과 재실행 결과를 함께 남긴다.

- [x] **Step 5: 회귀 검증 변경을 커밋한다.**

```bash
git add src/test docs/tasks/337/plan.md
git commit -m "337 test: 지정과목 동기화 회귀 검증 보강"
```

### Task 7: Wiki 계약 갱신과 PR 준비

**Files:**
- Update in Wiki repository: `Core-Domain-Flows.md`
- Update in Wiki repository: `Project-Architecture.md`
- Modify: `docs/tasks/337/plan.md`

**Interfaces:**
- Consumes: 확정된 S3 입력 계약, V12 스키마, 동기화·생명주기 정책.
- Produces: 운영자와 후속 편입생 졸업진단 구현자가 참조할 Wiki 및 PR 검증 기록.

- [x] **Step 1: `Core-Domain-Flows.md`에 입력·동기화 계약을 기록한다.**

포털 후처리 데이터 목록에 `designatedCourses`를 추가하고 다음 의미를 명시한다.

- 누락 또는 `null`: 구버전 scraper payload로 판단해 기존 목록과 버전을 유지한다.
- `[]`: 기존 목록을 비우고 스냅샷 버전을 갱신한다.
- 값이 있는 배열: 순서와 중복을 보존해 전체 교체한다.
- 지정과목은 이수 내역이 아니므로 `student_courses`나 취득학점에 직접 반영하지 않는다.
- `ScrapeJob.createdAt`보다 최신 버전이 이미 있으면 늦은 결과를 무시한다.

- [x] **Step 2: `Project-Architecture.md`에 저장 구조를 기록한다.**

학생 데이터 저장 설명에 `student_designated_courses`와 `students.designated_courses_snapshot_version`을 추가한다. 새 테이블은 `student_id` FK와 `ON DELETE CASCADE`, `(student_id, source_order)` unique constraint를 가지며 수강·과목 테이블과 연관되지 않음을 적는다.

- [x] **Step 3: Wiki 변경을 별도 저장소에 커밋한다.**

Wiki `master`에서 변경 범위와 링크를 확인한 뒤 다음 형식으로 커밋한다.

```bash
git add Core-Domain-Flows.md Project-Architecture.md
git commit -m "337 docs: 포털 지정과목 저장 계약 문서화"
git push origin master
```

- [x] **Step 4: 최종 브랜치 범위를 검증한다.**

Run:

```bash
git status --short
git diff --check origin/dev...HEAD
git diff --stat origin/dev...HEAD
git log --oneline origin/dev..HEAD
```

Expected: 이슈 #337 관련 코드·migration·테스트·작업 문서만 포함되고 `.DS_Store`는 추적되지 않는다.

- [ ] **Step 5: PR 본문에 필수 기록을 포함한다.**

`.github/PULL_REQUEST_TEMPLATE.md`를 사용해 `Closes #337`, 변경 범위, `./gradlew check --stacktrace --no-daemon` 결과, Wiki commit, V12 backward compatibility, 남은 위험을 기록한다. PR 작성자를 Assignee로 지정하고 `✨ Feature` 릴리즈 노트 라벨을 붙인다.

## Verification Notes

- 계획 작성 시점 기준 브랜치: `feat/337`, 기준 커밋: `538df4ba` (`origin/dev`).
- 계획 작성 시점의 최신 migration: `V11__backfill_transfer_student_flag.sql`; 이번 작업의 새 migration은 V12로 고정한다.
- 확인된 Wiki 대상: `Core-Domain-Flows.md`, `Project-Architecture.md`.
- Task 1 RED: `PortalDataMapperTests`가 `DesignatedCourseData`와 `PortalData.designatedCourses()` 부재로 컴파일 실패했다.
- Task 1 GREEN: `./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon` 통과.
- Task 1 회귀: `./gradlew test --stacktrace --no-daemon` 통과.
- Task 1 포맷: `./gradlew spotlessApply --no-daemon` 통과.
- Task 2 회귀: `./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest --tests com.chukchuk.haksa.domain.student.model.StudentModelTests --tests com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepositoryTests --stacktrace --no-daemon` 통과.
- Task 3 회귀: `./gradlew test --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceIntegrationTests --stacktrace --no-daemon` 통과.
- Task 4 회귀: `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackTxServiceTests --tests com.chukchuk.haksa.application.portal.PortalCallbackPostProcessorTests --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackServiceUnitTests --stacktrace --no-daemon` 통과.
- Task 4 포맷: `./gradlew spotlessApply --no-daemon` 통과.
- Task 5 회귀: `./gradlew test --tests com.chukchuk.haksa.domain.student.service.StudentServiceUnitTests --tests com.chukchuk.haksa.domain.student.service.StudentDeletionServiceUnitTests --tests com.chukchuk.haksa.domain.user.service.UserServiceIntegrationTest --stacktrace --no-daemon`에서 초기 RED 후 구현 GREEN 통과.
- Task 6 회귀: `./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --tests com.chukchuk.haksa.application.portal.PortalCallbackPostProcessorTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceTests --tests com.chukchuk.haksa.application.portal.SyncDesignatedCourseServiceIntegrationTests --tests com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepositoryTests --stacktrace --no-daemon` 통과.
- Task 6 통합 검증: 저장 실패 시 기존 지정과목·버전 롤백, 학업 요약 취득학점 불변, 실제 callback JSON의 지정과목 순서·중복 보존을 확인했다.
- 전체 품질 게이트: 기본 Java 24에서는 Gradle Checkstyle task 생성 오류(`Type T not present`)가 발생했으나, Java 17(`temurin-17.0.18`) 환경의 `./gradlew check --stacktrace --no-daemon`은 통과했다.
- Task 6 포맷·정적 검사: Java 17 환경에서 `./gradlew spotlessApply --no-daemon` 및 `./gradlew checkstyleMain checkstyleTest --stacktrace --no-daemon` 통과.
- Task 7 문서화: Wiki `Core-Domain-Flows.md`, `Project-Architecture.md`를 커밋 `b6b7e8c`로 갱신하고 `origin/master`에 push했다.
- 지정과목 스냅샷 반영 시 `AcademicCache.deleteAllByStudentId`를 호출해 졸업진단·학사 캐시를 무효화하며, 필드 누락·stale 결과에는 호출하지 않는다.
- 학생 학사 데이터 reset 경로에서도 지정과목·스냅샷 버전과 함께 학생별 학사 캐시를 초기화한다.
- 최종 품질 게이트: Java 17 환경의 `./gradlew check --stacktrace --no-daemon` 통과.
- `RawPortalData`와 `PortalData`에 구버전 3개 인자 생성자를 유지해 designatedCourses 필드 추가 전 호출부도 호환한다.
- 호환성 보강 후에도 Java 17 환경의 `./gradlew check --stacktrace --no-daemon` 통과.
- PR 생성과 Assignee·릴리즈 라벨 설정은 사용자의 별도 요청 후 수행한다.
- 남은 주요 위험은 실제 운영 payload의 숫자 타입 변형, PostgreSQL에서의 비관적 잠금 순서, bulk delete 뒤 insert 실패 시 rollback이다. Task 1, 3, 6에서 각각 회귀 테스트로 닫는다.
