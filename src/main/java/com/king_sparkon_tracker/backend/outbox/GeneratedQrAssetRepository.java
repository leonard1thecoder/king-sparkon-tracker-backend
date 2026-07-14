package com.king_sparkon_tracker.backend.outbox;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedQrAssetRepository extends JpaRepository<GeneratedQrAsset, String> {
	Optional<GeneratedQrAsset> findByAggregateTypeAndAggregateIdAndQrValueHash(
			String aggregateType,
			String aggregateId,
			String qrValueHash);
}
