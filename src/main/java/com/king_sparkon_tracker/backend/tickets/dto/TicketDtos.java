package com.king_sparkon_tracker.backend.tickets.dto;

import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLevel;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawalStatus;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TicketDtos {
    private TicketDtos() {}

    public record EventTicketTypeRequest(@NotNull TicketType type, @NotNull @DecimalMin("0.00") BigDecimal price, @Min(1) int capacity) {}
    public record CreateEventRequest(@NotBlank String ownerId, @NotBlank String name, @NotBlank String description, @NotBlank String location, @NotNull @FutureOrPresent LocalDate eventDate, @NotNull LocalTime eventTime, String bannerUrl, @NotNull TicketEventStatus status, @Valid @NotEmpty List<EventTicketTypeRequest> ticketTypes) {}
    public record UpdateEventRequest(String name, String description, String location, @FutureOrPresent LocalDate eventDate, LocalTime eventTime, String bannerUrl, TicketEventStatus status, @Valid List<EventTicketTypeRequest> ticketTypes) {}
    public record PurchaseTicketsRequest(@NotBlank String eventId, @NotBlank String userId, @NotBlank String buyerName, @NotBlank @Email String buyerEmail, @NotNull TicketType ticketType, @Min(1) int quantity) {}
    public record VerifyTicketRequest(@NotBlank String value, @NotBlank String workerId) {}
    public record TicketWithdrawalRequest(@NotBlank String ownerId, @NotNull @DecimalMin("0.01") BigDecimal grossAmount, String notes) {}
    public record EventTicketTypeResponse(String id, String eventId, TicketType type, BigDecimal price, int capacity, int sold, int available) {}
    public record TicketEventResponse(String id, String ownerId, String name, String description, String location, LocalDate eventDate, LocalTime eventTime, String bannerUrl, TicketEventStatus status, List<EventTicketTypeResponse> ticketTypes, Instant createdAt, Instant updatedAt) {}
    public record UserTicketResponse(String id, String eventId, String userId, String buyerName, String buyerEmail, TicketType ticketType, BigDecimal pricePaid, String qrCodeValue, String ticketReference, UserTicketStatus status, Instant purchasedAt, Instant usedAt, String scannedByWorkerId) {}
    public record TicketPaymentResponse(String id, String eventId, String userId, TicketType ticketType, int quantity, BigDecimal subtotalAmount, BigDecimal checkoutServiceFeeAmount, BigDecimal totalAmount, TicketPaymentStatus status, String paymentProvider, String paymentReference, Instant createdAt) {}
    public record TicketPurchaseResponse(TicketPaymentResponse payment, List<?> tickets) {}
    public record TicketVerificationResponse(boolean valid, String message, Object ticket, TicketEventResponse event) {}
    public record OwnerTicketDashboardResponse(long totalEvents, long upcomingEvents, int totalCapacity, int totalSold, int totalAvailable, long regularSold, long vipSold, long vvipSold, BigDecimal revenue, BigDecimal availableWithdrawalBalance, BigDecimal ticketWithdrawalFeePercent) {}
    public record TicketWithdrawalResponse(String id, String ownerId, BigDecimal grossAmount, BigDecimal serviceFeePercent, BigDecimal serviceFeeAmount, BigDecimal netAmount, TicketWithdrawalStatus status, Instant requestedAt, Instant processedAt, String notes) {}
    public record TicketAuditLogResponse(String id, String ownerId, String eventId, String ticketId, String actorId, String action, TicketAuditLevel level, String message, String structuredMetadata, Instant createdAt) {}
}
