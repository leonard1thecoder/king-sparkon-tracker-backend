package com.king_sparkon_tracker.backend.outbox;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.finance.FinancialLedgerService;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;

@Component
public class ReportGenerationOutboxHandler implements OutboxEventHandler {
	private final ObjectMapper objectMapper;
	private final FinancialLedgerService financialLedgerService;
	private final BusinessRepository businessRepository;
	private final GeneratedReportRepository reportRepository;

	public ReportGenerationOutboxHandler(
			ObjectMapper objectMapper,
			FinancialLedgerService financialLedgerService,
			BusinessRepository businessRepository,
			GeneratedReportRepository reportRepository) {
		this.objectMapper = objectMapper;
		this.financialLedgerService = financialLedgerService;
		this.businessRepository = businessRepository;
		this.reportRepository = reportRepository;
	}
	@Override public OutboxEventType supports() { return OutboxEventType.REPORT_GENERATION; }
	@Override public void handle(OutboxEvent event) throws Exception {
		OutboxPayloads.ReportGeneration payload = objectMapper.readValue(event.getPayload(), OutboxPayloads.ReportGeneration.class);
		Business business = businessRepository.findById(payload.businessId())
				.orElseThrow(() -> new IllegalArgumentException("Business not found for report"));
		Object report = switch (payload.reportType()) {
			case "FINANCIAL_RECONCILIATION" -> financialLedgerService.reconcile(payload.businessId(), true);
			default -> throw new IllegalArgumentException("Unsupported background report type: " + payload.reportType());
		};
		reportRepository.save(new GeneratedReport(business, payload.reportType(), objectMapper.writeValueAsString(report)));
	}
}
