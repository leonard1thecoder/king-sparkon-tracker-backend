package com.king_sparkon_tracker.backend.tickets.service;

import com.king_sparkon_tracker.backend.tickets.dto.TicketEngagementDtos.BusinessCatalogItem;
import com.king_sparkon_tracker.backend.tickets.dto.TicketEngagementDtos.CommentRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketEngagementDtos.CommentResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketEngagementDtos.FollowResponse;
import com.king_sparkon_tracker.backend.tickets.model.BusinessFollow;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventComment;
import com.king_sparkon_tracker.backend.tickets.repository.CompanyFollowRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventCommentRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketEngagementService {
    private final TicketEventRepository ticketEventRepository;
    private final TicketEventCommentRepository ticketEventCommentRepository;
    private final CompanyFollowRepository companyFollowRepository;

    public TicketEngagementService(TicketEventRepository ticketEventRepository, TicketEventCommentRepository ticketEventCommentRepository, CompanyFollowRepository companyFollowRepository) {
        this.ticketEventRepository = ticketEventRepository;
        this.ticketEventCommentRepository = ticketEventCommentRepository;
        this.companyFollowRepository = companyFollowRepository;
    }

    public CommentResponse addComment(CommentRequest request, String userId) {
        if (!ticketEventRepository.existsById(request.eventId())) throw new IllegalArgumentException("Ticket event not found.");
        TicketEventComment comment = new TicketEventComment();
        comment.setId("ticket-comment-" + UUID.randomUUID());
        comment.setEventId(request.eventId());
        comment.setUserId(userId);
        comment.setDisplayName(request.displayName().trim());
        comment.setComment(request.comment().trim());
        return toCommentResponse(ticketEventCommentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String eventId) {
        return ticketEventCommentRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream().map(this::toCommentResponse).toList();
    }

    public FollowResponse follow(String businessId, String userId) {
        BusinessFollow follow = companyFollowRepository.findByUserIdAndBusinessId(userId, businessId).orElseGet(() -> {
            BusinessFollow created = new BusinessFollow();
            created.setId("business-follow-" + UUID.randomUUID());
            created.setUserId(userId);
            created.setBusinessId(businessId);
            return created;
        });
        follow.setActive(true);
        follow.setUpdatedAt(Instant.now());
        BusinessFollow saved = companyFollowRepository.save(follow);
        return new FollowResponse(saved.getBusinessId(), saved.getUserId(), saved.isActive());
    }

    public FollowResponse unfollow(String businessId, String userId) {
        BusinessFollow follow = companyFollowRepository.findByUserIdAndBusinessId(userId, businessId).orElseThrow(() -> new IllegalArgumentException("Follow record not found."));
        follow.setActive(false);
        follow.setUpdatedAt(Instant.now());
        BusinessFollow saved = companyFollowRepository.save(follow);
        return new FollowResponse(saved.getBusinessId(), saved.getUserId(), saved.isActive());
    }

    @Transactional(readOnly = true)
    public List<BusinessCatalogItem> catalog(String userId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TicketEvent event : ticketEventRepository.findAll()) {
            counts.put(event.getOwnerId(), counts.getOrDefault(event.getOwnerId(), 0L) + 1L);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new BusinessCatalogItem(entry.getKey(), "Business " + entry.getKey(), null, companyFollowRepository.existsByUserIdAndBusinessIdAndActiveTrue(userId, entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(BusinessCatalogItem::following).thenComparing(BusinessCatalogItem::businessName))
                .toList();
    }

    private CommentResponse toCommentResponse(TicketEventComment comment) {
        return new CommentResponse(comment.getId(), comment.getEventId(), comment.getUserId(), comment.getDisplayName(), comment.getComment(), comment.getCreatedAt());
    }
}
