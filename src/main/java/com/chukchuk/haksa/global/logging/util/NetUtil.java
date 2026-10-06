package com.chukchuk.haksa.global.logging.util;

import jakarta.servlet.http.HttpServletRequest;

/** HTTP 요청의 네트워크 정보를 추출한다. */
public final class NetUtil {
  private NetUtil() {}

  /**
   * 프록시 헤더를 고려해 요청 클라이언트 IP를 반환한다.
   *
   * @param req 클라이언트 주소와 프록시 헤더를 포함한 HTTP 요청
   * @return {@code X-Forwarded-For}의 첫 주소, {@code X-Real-IP}, 원격 주소 순으로 선택한 IP
   */
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

  /**
   * 로그에 기록할 IP 주소에서 호스트를 식별할 수 있는 뒷부분을 마스킹한다.
   *
   * @param ip 마스킹할 IPv4·IPv6 주소
   * @return IPv4는 앞 두 옥텟만, IPv6는 첫 접두만 남긴 문자열
   */
  public static String shorten(String ip) {
    if (ip == null) {
      return "unknown";
    }
    if (ip.contains(":")) { // IPv6
      int idx = ip.indexOf(':');
      return idx > 0 ? ip.substring(0, idx) + ":*" : "ipv6:*";
    }
    String[] p = ip.split("\\.");
    if (p.length == 4) {
      return p[0] + "." + p[1] + ".*.*";
    }
    return ip;
  }
}
