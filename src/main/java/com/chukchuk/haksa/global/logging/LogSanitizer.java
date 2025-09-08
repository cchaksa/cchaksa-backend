package com.chukchuk.haksa.global.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 로그에 노출되면 안 되는 민감정보를 ***로 마스킹
 */
public final class LogSanitizer {

    /* 공통 패턴 */
    private static final Pattern EMAIL   = Pattern.compile("([\\w._%+-])([\\w._%+-]*)(@[^\\s,;]+)");
    private static final Pattern TOKEN   = Pattern.compile("(Bearer|token|access_token|id_token|refresh_token)=([A-Za-z0-9._~-]+)", Pattern.CASE_INSENSITIVE);

    /* 도메인 패턴 */
    // query/header/body에서 key:value 형태로 흔히 보이는 것만 정밀 타깃팅
    private static final Pattern STUDENT_CODE = Pattern.compile("(?i)(student(Code|Id|No))=([^&\\s,;]+)");
    private static final Pattern PORTAL_SESS  = Pattern.compile("(?i)(portal(Session|Token)|JSESSIONID)=([^&\\s,;]+)");

    /* 추가 확장용 */
    private static final List<ReplaceRule> EXTRA_RULES = new ArrayList<>();

    private LogSanitizer() {}

    /** 문자열 전체를 마스킹. null은 null 반환. */
    public static String clean(String s) {
        if (s == null) return null;
        String r = s;

        // 1) 포맷 독립 키-값 마스킹(도메인 우선)
        r = STUDENT_CODE.matcher(r).replaceAll("$1=***");
        r = PORTAL_SESS .matcher(r).replaceAll("$1=***");

        // 2) 일반 민감정보
        r = EMAIL .matcher(r).replaceAll(m -> m.group(1) + "***" + m.group(3)); // a***@domain
        r = TOKEN .matcher(r).replaceAll("$1=***");

        // 3) 추가 규칙 적용
        for (ReplaceRule rule : EXTRA_RULES) {
            r = rule.pattern.matcher(r).replaceAll(rule.replacement);
        }
        return r;
    }

    /** 개별 인자 전용: 객체를 문자열화 후 clean 적용 (SLF4J args에 사용) */
    public static Object arg(Object o) {
        if (o == null) return null;
        return clean(String.valueOf(o));
    }

    /** application.yml 등에서 외부 규칙을 주입하고 싶을 때 사용 */
    public static void registerExtraRule(String regex, String replacement) {
        EXTRA_RULES.add(new ReplaceRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), replacement));
    }

    private record ReplaceRule(Pattern pattern, String replacement) {}
}
