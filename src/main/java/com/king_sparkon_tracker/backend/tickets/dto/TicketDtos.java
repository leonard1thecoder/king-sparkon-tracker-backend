package com.king_sparkon_tracker.backend.tickets.dto;

import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLevel;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPromotionStatus;
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
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TicketDtos {
    private TicketDtos() {}

    public static final class EventTicketTypeRequest {
        @NotBlank
        private String type;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;

        @Min(1)
        private int capacity;

        public EventTicketTypeRequest() {
        }

        public EventTicketTypeRequest(TicketType type, BigDecimal price, int capacity) {
            this(type == null ? null : type.name(), price, capacity);
        }

        public EventTicketTypeRequest(String type, BigDecimal price, int capacity) {
            this.type = type;
            this.price = price;
            this.capacity = capacity;
        }

        public TicketType type() {
            return TicketType.fromInput(type);
        }

        public BigDecimal price() {
            return price;
        }

        public int capacity() {
            return capacity;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }

    public record CreateEventRequest(@NotBlank String ownerId, @NotBlank String name, @NotBlank String description, @NotBlank String location, @NotNull @FutureOrPresent LocalDate eventDate, @NotNull LocalTime eventTime, String bannerUrl, String posterPhotoUrl, @NotNull TicketEventStatus status, @Valid @NotEmpty List<EventTicketTypeRequest> ticketTypes, @Size(max = 100) List<String> workerIds, @Size(max = 100) List<String> affiliateIds) {
        public CreateEventRequest(String ownerId, String name, String description, String location, LocalDate eventDate, LocalTime eventTime, String bannerUrl, TicketEventStatus status, List<EventTicketTypeRequest> ticketTypes) {
            this(ownerId, name, description, location, eventDate, eventTime, bannerUrl, null, status, ticketTypes, null, null);
        }
    }

    public record UpdateEventRequest(String name, String description, String location, @FutureOrPresent LocalDate eventDate, LocalTime eventTime, String bannerUrl, String posterPhotoUrl, TicketEventStatus status, @Valid List<EventTicketTypeRequest> ticketTypes, @Size(max = 100) List<String> workerIds, @Size(max = 100) List<String> affiliateIds) {
        public UpdateEventRequest(String name, String description, String location, LocalDate eventDate, LocalTime eventTime, String bannerUrl, TicketEventStatus status, List<EventTicketTypeRequest> ticketTypes) {
            this(name, description, location, eventDate, eventTime, bannerUrl, null, status, ticketTypes, null, null);
        }
    }

    public static final class PurchaseTicketsRequest {
        @NotBlank
        private String eventId;

        @NotBlank
        private String userId;

        @NotBlank
        private String buyerName;

        @NotBlank
        @Email
        private String buyerEmail;

        @NotBlank
        private String ticketType;

        @Min(1)
        private int quantity;

        private String callbackUrl;

        public PurchaseTicketsRequest() {
        }

        public PurchaseTicketsRequest(String eventId, String userId, String buyerName, String buyerEmail, TicketType ticketType, int quantity) {
            this(eventId, userId, buyerName, buyerEmail, ticketType, quantity, null);
        }

        public PurchaseTicketsRequest(String eventId, String userId, String buyerName, String buyerEmail, TicketType ticketType, int quantity, String callbackUrl) {
            this(eventId, userId, buyerName, buyerEmail, ticketType == null ? null : ticketType.name(), quantity, callbackUrl);
        }

        public PurchaseTicketsRequest(String eventId, String userId, String buyerName, String buyerEmail, String ticketType, int quantity, String callbackUrl) {
            this.eventId = eventId;
            this.userId = userId;
            this.buyerName = buyerName;
            this.buyerEmail = buyerEmail;
            this.ticketType = ticketType;
            this.quantity = quantity;
            this.callbackUrl = callbackUrl;
        }

        public String eventId() {
            return eventId;
        }

        public String userId() {
            return userId;
        }

        public String buyerName() {
            return buyerName;
        }

        public String buyerEmail() {
            return buyerEmail;
        }

        public TicketType ticketType() {
            return TicketType.fromInput(ticketType);
        }

        public int quantity() {
            return quantity;
        }

        public String callbackUrl() {
            return callbackUrl;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setBuyerName(String buyerName) {
            this.buyerName = buyerName;
        }

        public void setBuyerEmail(String buyerEmail) {
            this.buyerEmail = buyerEmail;
        }

        public void setTicketType(String ticketType) {
            this.ticketType = ticketType;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }
    }

    public record VerifyTicketRequest(@NotBlank String value, @NotBlank String workerId) {}
    public record TicketWithdrawalRequest(@NotBlank String ownerId, @NotNull @DecimalMin("0.01") BigDecimal grossAmount, String notes) {}
    public record PromoteTicketEventRequest(@DecimalMin("0.01") BigDecimal amount, Instant startsAt, Instant endsAt) {}
    public record EventTicketTypeResponse(String id, String eventId, TicketType type, BigDecimal price, int capacity, int sold, int available) {}
    public record TicketEventResponse(String id, String ownerId, String name, String description, String location, LocalDate eventDate, LocalTime eventTime, String bannerUrl, String posterPhotoUrl, TicketEventStatus status, List<EventTicketTypeResponse> ticketTypes, List<String> workerIds, List<String> affiliateIds, Instant createdAt, Instant updatedAt) {}
    public record UserTicketResponse(String id, String eventId, String userId, String buyerName, String buyerEmail, TicketType ticketType, BigDecimal pricePaid, String qrCodeValue, String ticketReference, UserTicketStatus status, Instant purchasedAt, Instant usedAt, String scannedByWorkerId) {}
    public record TicketPaymentResponse(String id, String eventId, String userId, TicketType ticketType, int quantity, BigDecimal subtotalAmount, BigDecimal checkoutServiceFeeAmount, BigDecimal totalAmount, TicketPaymentStatus status, String paymentProvider, String paymentReference, String paymentUrl, String qrCodeUrl, Instant createdAt) {}
    public record TicketPurchaseResponse(TicketPaymentResponse payment, List<?> tickets) {}
    public record TicketVerificationResponse(boolean valid, String message, Object ticket, TicketEventResponse event) {}
    public record OwnerTicketDashboardResponse(long totalEvents, long upcomingEvents, int totalCapacity, int totalSold, int totalAvailable, long regularSold, long vipSold, long vvipSold, BigDecimal revenue, BigDecimal availableWithdrawalBalance, BigDecimal ticketWithdrawalFeePercent) {}
    public record TicketWithdrawalResponse(String id, String ownerId, BigDecimal grossAmount, BigDecimal serviceFeePercent, BigDecimal serviceFeeAmount, BigDecimal netAmount, TicketWithdrawalStatus status, Instant requestedAt, Instant processedAt, String notes) {}
    public record TicketEventPromotionResponse(String id, String eventId, String ownerId, BigDecimal amount, String currency, TicketPromotionStatus status, Instant startsAt, Instant endsAt, Long businessAccountEntryId, Instant createdAt, Instant updatedAt) {}
    public record TicketAuditLogResponse(String id, String ownerId, String eventId, String ticketId, String actorId, String action, TicketAuditLevel level, String message, String structuredMetadata, Instant createdAt) {}
}
