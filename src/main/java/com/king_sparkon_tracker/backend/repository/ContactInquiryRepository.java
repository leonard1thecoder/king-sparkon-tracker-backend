package com.king_sparkon_tracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.ContactInquiry;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {
}
