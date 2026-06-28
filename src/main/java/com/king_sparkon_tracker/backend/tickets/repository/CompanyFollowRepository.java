package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.BusinessFollow;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyFollowRepository extends JpaRepository<BusinessFollow, String> {
    Optional<BusinessFollow> findByUserIdAndBusinessId(String userId, String businessId);
    boolean existsByUserIdAndBusinessIdAndActiveTrue(String userId, String businessId);
}
