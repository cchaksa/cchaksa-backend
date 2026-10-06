package com.chukchuk.haksa.global.logging.sanitize;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/** 로그에 기록되는 민감정보와 제어문자를 제거한다. */
public final class LogSanitizer {

  /* 공통 패턴 */
  private static final Pattern EMAIL = Pattern.compile("([\\w._%+-])([\\w._%+-]*)(@[^\\s,;]+)");
  private static final Pattern TOKEN_QS =
      Pattern.compile(
          "(?:^|[?&])(token|access_token|id_token|refresh_token)=([^&\\s,;]+)",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTH_HEADER =
      Pattern.compile("(?i)(authorization\\s*:\\s*bearer)\\s+([A-Za-z0-9._~+/=\\-]+)");

  /* 도메인/자격증명 패턴 (쿼리스트링) */
  private static final Pattern PASSWORD_QS =
      Pattern.compile("(?i)(?:^|[?&])(password|passwd|pwd|pass)=([^&\\s,;]*)");
  private static final Pattern USERNAME_QS =
      Pattern.compile("(?i)(?:^|[?&])(username|user|login|account)=([^&\\s,;]*)");

  /* 세션/학번 등 */
  private static final Pattern STUDENT_CODE =
      Pattern.compile("(?i)(student(Code|Id|No))=([^&\\s,;]+)");
  private static final Pattern PORTAL_SESS =
      Pattern.compile("(?i)(portal(Session|Token)|JSESSIONID)=([^&\\s,;]+)");

  /* JSON 본문 내 키-값 마스킹: "password": "value" 등 */
  private static final Pattern JSON_SECRET_KV =
      Pattern.compile(
          "(?i)\"(password|passwd|pwd|token|access_token|refresh_token|secret|"
              + "authorization)\"\\s*:\\s*\"[^\"]*\"");

  /* 확장 규칙 (thread-safe) */
  private static final List<ReplaceRule> EXTRA_RULES = new CopyOnWriteArrayList<>();

  private LogSanitizer() {}

  /**
   * 로그 문자열에서 민감정보와 제어문자를 제거한다.
   *
   * @param s 로그에 기록하려는 문자열이며 {@code null}일 수 있음
   * @return 토큰·비밀번호·학번·이메일과 제어 대상 값이 마스킹된 문자열 또는 {@code null}
   */
  public static String clean(String s) {
    if (s == null) {
      return null;
    }
    String r = s;

    // 1) 쿼리 파라미터/세션/학번 등 도메인 우선
    r = STUDENT_CODE.matcher(r).replaceAll("$1=***");
    r = PORTAL_SESS.matcher(r).replaceAll("$1=***");

    // 2) 자격증명 (쿼리스트링, 토큰, 헤더)
    r = PASSWORD_QS.matcher(r).replaceAll(m -> m.group(0).replace(m.group(2), "***"));
    r =
        USERNAME_QS
            .matcher(r)
            .replaceAll(m -> m.group(0).replace(m.group(2), maskMiddle(m.group(2))));
    r = TOKEN_QS.matcher(r).replaceAll(m -> m.group(0).replace(m.group(2), "***"));
    r = AUTH_HEADER.matcher(r).replaceAll("$1 ***");

    // 3) 일반 민감정보
    r = EMAIL.matcher(r).replaceAll(m -> m.group(1) + "***" + m.group(3)); // a***@domain

    // 4) JSON 본문 내 키-값
    r = JSON_SECRET_KV.matcher(r).replaceAll(m -> "\"" + m.group(1) + "\": \"***\"");

    // 5) 추가 규칙
    for (ReplaceRule rule : EXTRA_RULES) {
      r = rule.pattern.matcher(r).replaceAll(rule.replacement);
    }
    return r;
  }

  /**
   * 로그 인자를 안전한 값으로 변환한다.
   *
   * @param o 문자열로 변환해 로그에 기록할 객체
   * @return 문자열 변환 후 민감정보를 마스킹한 값 또는 {@code null}
   */
  public static Object arg(Object o) {
    return (o == null) ? null : clean(String.valueOf(o));
  }

  /**
   * 추가 로그 마스킹 규칙을 등록한다.
   *
   * @param regex 대소문자를 구분하지 않고 추가로 치환할 정규식
   * @param replacement 정규식에 일치한 내용을 바꿀 문자열
   * @throws java.util.regex.PatternSyntaxException 정규식 문법이 유효하지 않은 경우
   */
  public static void registerExtraRule(String regex, String replacement) {
    EXTRA_RULES.add(new ReplaceRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), replacement));
  }

  private static String maskMiddle(String v) {
    if (v == null || v.isBlank()) {
      return v;
    }
    if (v.length() <= 2) {
      return "*".repeat(v.length());
    }
    int keep = Math.max(1, v.length() / 3);
    String head = v.substring(0, keep);
    String tail = v.substring(v.length() - keep);
    return head + "***" + tail;
  }

  private record ReplaceRule(Pattern pattern, String replacement) {}
}
