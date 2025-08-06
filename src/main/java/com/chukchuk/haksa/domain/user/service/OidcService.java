package com.chukchuk.haksa.domain.user.service;

import io.jsonwebtoken.Claims;

public interface OidcService {
    Claims verifyIdToken(String idToken, String nonce);
}
