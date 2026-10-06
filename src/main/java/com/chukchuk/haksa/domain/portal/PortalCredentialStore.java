package com.chukchuk.haksa.domain.portal;

/** 포털 연동 중에만 사용하는 계정 자격 증명의 저장·조회·삭제 계약을 정의한다. */
public interface PortalCredentialStore {

  /**
   * 포털 연동이 끝날 때까지 사용할 사용자 자격 증명을 임시 저장한다.
   *
   * @param userId 사용자 식별자
   * @param username 임시 저장할 포털 로그인 아이디
   * @param password 포털 비밀번호
   */
  void save(String userId, String username, String password);

  /**
   * 사용자의 임시 포털 계정 아이디를 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 저장된 포털 계정 아이디, 없으면 {@code null}
   */
  String getUsername(String userId);

  /**
   * 사용자의 임시 포털 계정 비밀번호를 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 저장된 포털 계정 비밀번호, 없으면 {@code null}
   */
  String getPassword(String userId);

  /**
   * 사용자의 임시 포털 자격 증명을 저장소에서 제거한다.
   *
   * @param userId 사용자 식별자
   */
  void clear(String userId);
}
