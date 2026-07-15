package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;
import java.util.UUID;

import com.king_sparkon_tracker.backend.model.Business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_reports")
public class GeneratedReport {
	@Id
	@Column(length = 64)
	private String id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;
	@Column(name = "report_type", nullable = false, length = 80)
	private String reportType;
	@Column(nullable = false, columnDefinition = "text")
	private String payload;
	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	protected GeneratedReport() {
	}
	public GeneratedReport(Business business, String reportType, String payload) {
		this.id = "RPT-" + UUID.randomUUID();
		this.business = business;
		this.reportType = reportType;
		this.payload = payload;
	}
	@PrePersist void beforeCreate() { generatedAt = Instant.now(); }
	public String getId() { return id; }
	public Business getBusiness() { return business; }
	public String getReportType() { return reportType; }
	public String getPayload() { return payload; }
	public Instant getGeneratedAt() { return generatedAt; }
}
