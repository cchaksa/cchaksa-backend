package com.chukchuk.haksa.domain.portal;

public interface PortalCredentialStore {

    void save(String userId, String username, String password);

    String getUsername(String userId);

    String getPassword(String userId);

    void clear(String userId);
}