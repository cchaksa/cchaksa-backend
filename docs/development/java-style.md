# Java 스타일 가이드

척척학사 Java 소스는 Google Java Style을 기본으로 하며, Gradle 품질 검사로 동일한 기준을 적용합니다.

## 자동 포맷

`./gradlew spotlessApply --no-daemon`으로 main/test Java 소스를 google-java-format 기준에 맞춥니다. 포맷 적용 뒤에는 `./gradlew spotlessCheck --no-daemon`으로 변경이 남지 않았는지 확인합니다.

## Checkstyle

`./gradlew checkstyleMain checkstyleTest --no-daemon`으로 공식 Google Checks 기반의 명명, import, 줄 길이, Javadoc 규칙을 검사합니다. 자동 포맷으로 해결되지 않는 위반은 보고서의 파일·규칙을 확인해 동작을 보존하며 수정합니다.

## Javadoc

- public/protected 타입과 메서드는 호출자가 알아야 할 책임과 계약을 설명합니다.
- 매개변수, 반환값, 호출자가 처리해야 할 예외가 있으면 각각 `@param`, `@return`, `@throws`로 기록합니다.
- public/protected 메서드에서 `@param`, `@return` 또는 메서드 본문에서 직접 발생시키는 예외의 `@throws`를 빠뜨리면 Checkstyle이 실패합니다.
- record 타입은 각 구성요소를 `@param`으로 설명합니다.
- `@Override`, `@Test` 메서드는 상위 계약이나 테스트 이름으로 의도가 충분하면 중복 Javadoc을 작성하지 않습니다.
- private 구현에는 알고리즘이나 제약을 설명할 필요가 있을 때만 Javadoc을 작성합니다.
- 타입·메서드·매개변수 이름을 그대로 반복하지 않습니다. `요청 조건에 맞는 데이터를 조회한다`, `계층 간 전달할 데이터를 표현한다`, `@param value value 값`, `@param token 응답에 포함할 토큰`, `@return 생성된`처럼 호출자에게 새로운 정보를 주지 않는 문장은 Checkstyle에서 거부합니다. 단, 구체적인 의미를 설명한 정상 문장까지 금지하지 않도록 정확히 일치하는 반복형 문구만 검사합니다.

```java
/**
 * 학과 식별 정보를 반환한다.
 *
 * @param id 학과 식별자
 * @param name 학과 이름
 */
public record DepartmentResponse(Long id, String name) {}
```

## 파일 역할 주석

새 Java 파일은 `package` 선언 앞 첫 줄에 파일 역할을 설명하는 한 줄짜리 한국어 주석을 둡니다. 기존 파일을 단순 수정할 때는 역할 주석만 추가하기 위한 변경을 만들지 않습니다.

## 일반 주석

- 주석은 코드가 수행하는 동작을 반복하지 않고, 해당 구현을 선택한 이유나 지켜야 할 제약·부작용을 설명합니다.
- 코드만으로 의도가 명확한 경우에는 주석을 추가하지 않습니다.
- 구현을 변경할 때는 관련 주석도 함께 갱신하며, 더 이상 유효하지 않은 주석은 제거합니다.
- 후속 작업이 필요한 `TODO`에는 작업 내용과 연결된 이슈를 `TODO: 작업 설명 (#이슈 번호)` 형식으로 기록합니다.

## import와 명명

- wildcard import를 사용하지 않습니다.
- 타입은 UpperCamelCase, 운영·테스트 메서드와 변수는 lowerCamelCase를 사용합니다. 테스트 조건과 기대 결과도 밑줄 대신 camelCase로 이어 씁니다.
- 약어도 일반 단어처럼 표기합니다. 예: `OidcConfig`, `RawPortalDataDto`.
- Java 내부 식별자는 camelCase를 사용하고, 외부 JSON의 snake_case 계약은 `@JsonProperty`로 명시합니다.

## 로컬 검증

- 자동 수정: `./gradlew spotlessApply --no-daemon`.
- 포맷 진단: `./gradlew spotlessCheck --no-daemon`.
- 스타일 진단: `./gradlew checkstyleMain checkstyleTest --no-daemon`.
- 테스트: `./gradlew test --stacktrace --no-daemon`.
- 최종 통합 검사: `./gradlew check --stacktrace --no-daemon`.
