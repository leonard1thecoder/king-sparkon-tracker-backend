package com.king_sparkon_tracker.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
		@UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
})
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private TrackerUser user;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_hash", length = 64)
	private String replacedByTokenHash;

	@Column(name = "created_ip", length = 64)
	private String createdIp;

	@Column(name = "user_agent", length = 512)
	private String userAgent;

	protected RefreshToken() {
	}

	public RefreshToken(TrackerUser user, String tokenHash, Instant issuedAt, Instant expiresAt, String createdIp, String userAgent) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.createdIp = createdIp;
		this.userAgent = userAgent;
	}

	@PrePersist
	void beforeCreate() {
		if (issuedAt == null) {
			issuedAt = Instant.now();
		}
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
	}

	public void rotateTo(String replacementHash, Instant rotatedAt) {
		this.revokedAt = rotatedAt;
		this.replacedByTokenHash = replacementHash;
	}

	public void markRevoked(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public Long getId() {
		return id;
	}

	public TrackerUser getUser() {
		return user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public String getReplacedByTokenHash() {
		return replacedByTokenHash;
	}

	public String getCreatedIp() {
		return createdIp;
	}

	public String getUserAgent() {
		return userAgent;
	}
}
