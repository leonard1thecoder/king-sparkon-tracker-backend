package com.king_sparkon_tracker.backend.finance;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(
		name = "financial_ledger_lines",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_financial_line_number",
				columnNames = { "journal_id", "line_number" }))
public class FinancialLedgerLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "journal_id", nullable = false)
	private FinancialJournal journal;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_code", nullable = false, length = 80)
	private FinancialAccountCode accountCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_side", nullable = false, length = 12)
	private LedgerSide entrySide;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(length = 700)
	private String memo;

	protected FinancialLedgerLine() {
	}

	public FinancialLedgerLine(
			int lineNumber,
			FinancialAccountCode accountCode,
			LedgerSide entrySide,
			BigDecimal amount,
			String currency,
			String memo) {
		this.lineNumber = lineNumber;
		this.accountCode = accountCode;
		this.entrySide = entrySide;
		this.amount = amount;
		this.currency = currency;
		this.memo = memo;
	}

	void attach(FinancialJournal journal) { this.journal = journal; }
	public Long getId() { return id; }
	public FinancialJournal getJournal() { return journal; }
	public int getLineNumber() { return lineNumber; }
	public FinancialAccountCode getAccountCode() { return accountCode; }
	public LedgerSide getEntrySide() { return entrySide; }
	public BigDecimal getAmount() { return amount; }
	public String getCurrency() { return currency; }
	public String getMemo() { return memo; }
}
