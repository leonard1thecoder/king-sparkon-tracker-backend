package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.BarcodeCatalog;

public interface BarcodeCatalogRepository extends JpaRepository<BarcodeCatalog, Long> {

	Optional<BarcodeCatalog> findByBarcode(String barcode);

	boolean existsByBarcode(String barcode);
}
