package com.chukchuk.haksa.global.logging;

import java.util.regex.Pattern;

public final class LogSanitizer {
    private static final Pattern EMAIL = Pattern.compile("([\\w._%+-])([\\w._%+-]*)(@[^\\s,;]+)");
    private static final Pattern TOKEN = Pattern.compile("(Bearer|token|access_token)=([A-Za-z0-9._-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STUDENT = Pattern.compile("(student(Id|Code|No))=([\\w-]+)", Pattern.CASE_INSENSITIVE);

    private LogSanitizer() {}

    public static String clean(String s) {
        if (s == null) return null;
        String r = EMAIL.matcher(s).replaceAll(m -> m.group(1) + "***" + m.group(3));
        r = TOKEN.matcher(r).replaceAll("$1=***");
        r = STUDENT.matcher(r).replaceAll("$1=***");
        return r;
    }
}
