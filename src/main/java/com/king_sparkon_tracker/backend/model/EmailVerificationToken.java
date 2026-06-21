package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "email_verification_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_verification_tokens_token_hash", columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_email_verification_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_email_verification_tokens_token_hash_used_expires", columnList = "token_hash, used_at, expires_at")
        }
)
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private TrackerUser user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(TrackerUser user, String tokenHash, LocalDateTime expiresAt, String requestedIp, String userAgent) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestedIp = requestedIp;
        this.userAgent = userAgent;
    }

    @PrePersist
    void beforeCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public TrackerUser getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public String getRequestedIp() { return requestedIp; }
    public String getUserAgent() { return userAgent; }
    public void markUsed() { this.usedAt = LocalDateTime.now(); }
}
