# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | (폐기) 선교 AreaProgressDto의 liberalAreaCode 대표값 산정 방식 | PO | Closed — Obsolete (2026-04-30 기능 명세 정정으로 위치가 CourseDto로 변경되어 영역 단위 집계 자체가 불요) |
| 2 | JSON 직렬화 정책. (a) `@JsonInclude(NON_NULL)`로 omit; (b) 항상 키 노출 + null. | PO | Closed — 결정 2 (a안 채택, 2026-04-30) |
| 3 | `toCourseResponseDto` 매퍼는 단일전공(`getStudentAreaProgress`)·복수전공(`getDualMajorAreaProgress`)에서 공유 사용된다. CourseDto에 `liberalAreaCode`를 추가하면 두 흐름 모두에 자동 반영된다. PO 결정 "복수전공 미적용"을 어떻게 처리할 것인가? (i) 매퍼 공유 그대로 두고 양쪽에 반영(사실상 DualMajor도 적용); (ii) 단일전공/복수전공별 매퍼 분리; (iii) DualMajor에서만 응답 후 liberalAreaCode를 강제 null 처리. | PO | Closed — 결정 4 (i안 채택, 2026-04-30 추가 결정 — 복수전공도 적용) |
| 4 | 글로벌 Jackson 설정에 `NON_NULL`이 이미 적용돼 있는가? 적용돼 있다면 DTO 어노테이션 불요. | Codex | Closed — `application.yml`의 `spring.jackson`에는 `time-zone`/`date-format`만 설정되어 있고 `default-property-inclusion`은 미설정. 기존 응답 DTO(`ErrorResponse`, `SuccessResponse`, `ErrorDetail`) 컨벤션상 클래스 레벨에 `@JsonInclude(NON_NULL)`을 명시적으로 적용함. → **CourseDto에 클래스 레벨 어노테이션 추가**로 결정 (Decision 5, 2026-04-30) |
| 5 | LiberalArtsAreaCode 엔티티 PK는 `code: Integer`. 선교 영역에서 실제 사용 중인 코드 값(범위/도메인)을 PO가 인지하고 있는가? prod 데이터 샘플 검증 가능? | PO | Open — Follow-ups |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | (폐기) liberalAreaCode 대표값 산정 알고리즘 결정 | 기능 명세 정정으로 위치가 CourseDto의 per-course 필드로 변경됨에 따라 결정 자체가 불요 | 2026-04-30 |
| 2 | JSON 직렬화는 `@JsonInclude(JsonInclude.Include.NON_NULL)` 정책으로 null이면 응답에서 키 자체를 omit | 사용자가 "다른 areaType일 때는 json에서 무시"를 우선 의도로 명시. 클라이언트 입장에서 키 부재 = 미적용으로 명확히 해석 가능. CourseDto per-course로 변경된 후에도 동일 정책 유지 | 2026-04-30 |
| 3 | (변경) 본 스펙은 `CourseDto` 레벨 필드를 추가한다. 매퍼 `toCourseResponseDto`가 단일전공·복수전공 양쪽에서 공유되므로, 코드 변경 자체는 두 흐름에 자동 반영되는 점을 인지함. | 위치 변경에 따른 자연스러운 귀결. | 2026-04-30 |
| 4 | 단일전공뿐 아니라 복수전공 흐름(`getDualMajorAreaProgress`)에도 적용한다 — Q3의 (i)안. 매퍼를 분리하지 않고 공유 그대로 둠. | PO 추가 결정. 매퍼 분리는 코드 중복을 만들고, 복수전공 졸업요건에 선교 영역이 등장한다면 단일전공과 동일하게 노출되는 것이 일관적임. | 2026-04-30 |
| 5 | `CourseDto`에 클래스 레벨 `@JsonInclude(JsonInclude.Include.NON_NULL)` 적용. | 글로벌 Jackson `default-property-inclusion` 미설정. 기존 응답 DTO(`ErrorResponse`/`SuccessResponse`/`ErrorDetail`)가 동일 패턴을 사용하므로 컨벤션 일치. | 2026-04-30 |
| 6 | 선교 영역 area_code historical NULL 정합성 보강 작업을 본 #226에 같은 spec/branch로 추가 (E-2). | PO 결정. liberalAreaCode 노출의 데이터 정합성 보강이라는 같은 맥락. | 2026-05-20 |
| 7 | Backfill 범위는 `faculty_division_name = 선교` 한정 (D-1). 다른 교양 영역(중핵/기교/소교/전교 등)은 본 작업 범위 외. | PO 명시. 본 spec의 응답 노출 의도가 선교 중심. 다른 영역에서 같은 문제 발견 시 가드 한 줄 풀어 별도 이슈로 확장 가능. | 2026-05-20 |
| 8 | CourseOffering immutable 원칙을 backfill 전용 도메인 메서드 한 점에 한해 양보. 메서드 이름(`backfillMissionLiberalAreaCode`)·내부 가드(선교 검증/idempotent/null 검증)·Javadoc 4겹 방어선으로 다른 용도 차단. | PO 가드레일 "이 이슈에서만 깨지도록 방어적 처리". raw JDBC 우회는 JPA 1차 캐시 불일치 위험이 있어 비채택. | 2026-05-20 |
| 9 | Backfill 트리거는 사용자가 발생시키는 첫 연결/재연결 sync 흐름 한정. 운영 일회성 SQL backfill은 비채택. | PO 명시 — 다른 record의 값을 참조해서 채우는 방식은 잘못된 매핑 위험. 정확한 정정은 portal 재스크래핑 페이로드에서만 가능. | 2026-05-20 |

## Risks / Unknowns
- Item: course_offerings.area_code의 historical NULL 데이터 잔존 (2025-07-20 17:53 `10b6ef7` 이전 row 가 NULL)
  - Impact: 선교 과목 일부 또는 전부가 NULL로 노출 → 해당 row의 `liberalAreaCode` 키가 omit됨
  - Mitigation: 2차 작업으로 재스크래핑 시 NULL → non-null 단방향 backfill 분기 추가 (Decision 6~9 참조). 사용자가 자발적으로 재연결하지 않으면 backfill되지 않는 한계는 PO 인지 사항.
- Item: 복수전공 흐름의 응답 변경 영향도
  - Impact: PO 결정으로 dual-major에도 적용됨(Decision 4). dual-major 졸업 요건에 선교 영역이 등장한다면 단일전공과 동일하게 노출된다.
  - Mitigation: prod에서 dual-major × 선교 조합이 실제 발생하는지 데이터로 사전 검증 권장(Follow-up 항목).
- Item: 글로벌 Jackson 설정과 DTO 어노테이션 충돌 가능성
  - Impact: 글로벌 NON_NULL이 이미 적용된 경우 DTO 레벨 어노테이션은 중복이지만 무해. 미적용이면 DTO 어노테이션이 단일 효과 적용 지점이 됨.
  - Mitigation: Phase 5에서 `application.yml` / `ObjectMapper` 빈 설정 점검 후 결정.

## Follow-ups
- [ ] prod `course_offerings`에서 `faculty_division_name = '선교' AND area_code` 분포 통계 (Owner: PO/DB 담당)
- [ ] dual-major × '선교' 영역 조합이 실제 응답에 등장하는지 prod 데이터로 검증 (Owner: PO) — 적용 영향도 평가용
- [ ] historical NULL row backfill 별도 이슈 생성 (Owner: TBD) — 본 스펙 범위 외
