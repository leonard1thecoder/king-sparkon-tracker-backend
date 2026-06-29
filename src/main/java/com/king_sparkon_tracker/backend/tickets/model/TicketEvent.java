package com.king_sparkon_tracker.backend.tickets.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket_events")
public class TicketEvent {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 220)
    private String location;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private LocalTime eventTime;

    @Column(length = 1024)
    private String bannerUrl;

    @Column(length = 2048)
    private String posterPhotoUrl;

    @ElementCollection
    @CollectionTable(name = "ticket_event_workers", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "worker_id", length = 64)
    private List<String> workerIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ticket_event_affiliates", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "affiliate_id", length = 64)
    private List<String> affiliateIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TicketEventStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventTicketType> ticketTypes = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addTicketType(EventTicketType ticketType) {
        ticketTypes.add(ticketType);
        ticketType.setEvent(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getPosterPhotoUrl() {
        return posterPhotoUrl;
    }

    public void setPosterPhotoUrl(String posterPhotoUrl) {
        this.posterPhotoUrl = posterPhotoUrl;
    }

    public List<String> getWorkerIds() {
        return workerIds;
    }

    public void setWorkerIds(List<String> workerIds) {
        this.workerIds = workerIds == null ? new ArrayList<>() : new ArrayList<>(workerIds);
    }

    public List<String> getAffiliateIds() {
        return affiliateIds;
    }

    public void setAffiliateIds(List<String> affiliateIds) {
        this.affiliateIds = affiliateIds == null ? new ArrayList<>() : new ArrayList<>(affiliateIds);
    }

    public TicketEventStatus getStatus() {
        return status;
    }

    public void setStatus(TicketEventStatus status) {
        this.status = status;
    }

    public List<EventTicketType> getTicketTypes() {
        return ticketTypes;
    }

    public void setTicketTypes(List<EventTicketType> ticketTypes) {
        this.ticketTypes = ticketTypes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
