package com.chukchuk.haksa.domain.user.model;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_social_id",
                        columnNames = {"provider", "social_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private OidcProvider provider;

    @Column(name = "social_id", nullable = false, length = 255)
    private String socialId;

    @Column(name = "email", nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public SocialAccount(OidcProvider provider, String socialId, String email, User user) {
        this.provider = provider;
        this.socialId = socialId;
        this.email = email;
        this.user = user;
    }

    public void updateUser(User user) {
        this.user = user;
    }
}
