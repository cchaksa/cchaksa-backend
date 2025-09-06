package com.chukchuk.haksa.global.logging.util;


import jakarta.servlet.http.HttpServletRequest;

public final class NetUtil {
    private NetUtil(){}

    public static String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // 첫번째가 실제 클라이언트
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        return (ip != null && !ip.isBlank()) ? ip : req.getRemoteAddr();
    }

    /** 1.2.3.4 → 1.2.*.*  /  2001:db8:: → 접두만 */
    public static String shorten(String ip) {
        if (ip == null) return "unknown";
        if (ip.contains(":")) { // IPv6
            int idx = ip.indexOf(':');
            return idx > 0 ? ip.substring(0, idx) + ":*" : "ipv6:*";
        }
        String[] p = ip.split("\\.");
        if (p.length == 4) return p[0] + "." + p[1] + ".*.*";
        return ip;
    }
}