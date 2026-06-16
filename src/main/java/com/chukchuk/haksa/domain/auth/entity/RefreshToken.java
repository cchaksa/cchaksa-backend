package com.chukchuk.haksa.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Date;

@Entity
@Getter
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column
    private String token;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(nullable = false)
    private Date expiry;

    public RefreshToken(String sessionId, String userId, String token, Date expiry) {
        this(sessionId, userId, token, null, expiry);
    }

    public RefreshToken(String sessionId, String userId, String token, String tokenHash, Date expiry) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.token = token;
        this.tokenHash = tokenHash;
        this.expiry = expiry;
    }

    public boolean hasTokenHash() {
        return tokenHash != null && !tokenHash.isBlank();
    }

    public RefreshToken() {

    }
}
