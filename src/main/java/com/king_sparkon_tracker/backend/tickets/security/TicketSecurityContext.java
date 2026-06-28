package com.king_sparkon_tracker.backend.tickets.security;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TicketSecurityContext {

    public String currentUserId(Authentication authentication) {
        String value = firstClaim(authentication, List.of("userId", "id", "sub", "email"));
        if (value == null || value.isBlank()) {
            throw new AccessDeniedException("Authenticated user id is required.");
        }
        return value;
    }

    public String currentOwnerId(Authentication authentication) {
        String value = firstClaim(authentication, List.of("ownerId", "businessId", "companyId", "userId", "sub"));
        if (value == null || value.isBlank()) {
            throw new AccessDeniedException("Authenticated owner or business id is required.");
        }
        return value;
    }

    public String currentWorkerId(Authentication authentication) {
        String value = firstClaim(authentication, List.of("workerId", "userId", "id", "sub", "email"));
        if (value == null || value.isBlank()) {
            throw new AccessDeniedException("Authenticated worker id is required.");
        }
        return value;
    }

    private String firstClaim(Authentication authentication, List<String> claimNames) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            for (String claimName : claimNames) {
                Object claim = jwt.getClaims().get(claimName);
                if (claim != null && !claim.toString().isBlank()) return claim.toString();
            }
        }
        if (principal instanceof Map<?, ?> map) {
            for (String claimName : claimNames) {
                Object claim = map.get(claimName);
                if (claim != null && !claim.toString().isBlank()) return claim.toString();
            }
        }
        return authentication.getName();
    }
}
