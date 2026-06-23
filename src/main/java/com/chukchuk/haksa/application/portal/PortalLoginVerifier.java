// 포털 로그인 검증을 외부 구현과 분리하는 인터페이스
package com.chukchuk.haksa.application.portal;

public interface PortalLoginVerifier {

    void verify(String portalType, String username, String password);
}
