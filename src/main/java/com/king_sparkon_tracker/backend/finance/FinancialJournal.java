package com.king_sparkon_tracker.backend.finance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Immutable;

import com.king_sparkon_tracker.backend.model.Business;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(
		name = "financial_journals",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_financial_journal_source",
				columnNames = { "business_id", "source_type", "source_reference" }))
public class FinancialJournal {

	@Id
	@Column(length = 64)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Column(name = "source_type", nullable = false, length = 64)
	private String sourceType;

	@Column(name = "source_reference", nullable = false, length = 180)
	private String sourceReference;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "immutable_hash", nullable = false, length = 64)
	private String immutableHash;

	@Column(name = "posted_at", nullable = false)
	private Instant postedAt;

	@Column(name = "created_by", nullable = false, length = 255)
	private String createdBy;

	@OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = false)
	@OrderBy("lineNumber asc")
	private List<FinancialLedgerLine> lines = new ArrayList<>();

	protected FinancialJournal() {
	}

	public FinancialJournal(
			String id,
			Business business,
			String sourceType,
			String sourceReference,
			String description,
			String currency,
			String immutableHash,
			String createdBy) {
		this.id = id;
		this.business = business;
		this.sourceType = sourceType;
		this.sourceReference = sourceReference;
		this.description = description;
		this.currency = currency;
		this.immutableHash = immutableHash;
		this.postedAt = Instant.now();
		this.createdBy = createdBy;
	}

	public void addLine(FinancialLedgerLine line) {
		lines.add(line);
		line.attach(this);
	}

	public String getId() { return id; }
	public Business getBusiness() { return business; }
	public String getSourceType() { return sourceType; }
	public String getSourceReference() { return sourceReference; }
	public String getDescription() { return description; }
	public String getCurrency() { return currency; }
	public String getImmutableHash() { return immutableHash; }
	public Instant getPostedAt() { return postedAt; }
	public String getCreatedBy() { return createdBy; }
	public List<FinancialLedgerLine> getLines() { return Collections.unmodifiableList(lines); }
}
