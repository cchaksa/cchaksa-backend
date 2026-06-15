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

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private Date expiry;

    public RefreshToken(String sessionId, String userId, String token, Date expiry) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.token = token;
        this.expiry = expiry;
    }

    public RefreshToken() {

    }
}
