package com.king_sparkon_tracker.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.king_sparkon_tracker.backend.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
			String tokenHash,
			LocalDateTime now);

	@Modifying
	@Query("""
			update PasswordResetToken token
			   set token.usedAt = :usedAt
			 where token.user.id = :userId
			   and token.usedAt is null
			""")
	int invalidateUnusedTokensForUser(Long userId, LocalDateTime usedAt);
}
