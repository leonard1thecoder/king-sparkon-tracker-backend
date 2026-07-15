package com.king_sparkon_tracker.backend.outbox;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.king_sparkon_tracker.backend.service.GoogleStorageService;

@Component
public class QrGenerationOutboxHandler implements OutboxEventHandler {

	private final ObjectMapper objectMapper;
	private final GeneratedQrAssetRepository repository;
	private final GoogleStorageService storageService;

	public QrGenerationOutboxHandler(
			ObjectMapper objectMapper,
			GeneratedQrAssetRepository repository,
			GoogleStorageService storageService) {
		this.objectMapper = objectMapper;
		this.repository = repository;
		this.storageService = storageService;
	}

	@Override
	public OutboxEventType supports() { return OutboxEventType.QR_GENERATION; }

	@Override
	public void handle(OutboxEvent event) throws Exception {
		OutboxPayloads.QrGeneration payload = objectMapper.readValue(event.getPayload(), OutboxPayloads.QrGeneration.class);
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(payload.value().getBytes(StandardCharsets.UTF_8)));
		if (repository.findByAggregateTypeAndAggregateIdAndQrValueHash(
				payload.aggregateType(), payload.aggregateId(), hash).isPresent()) {
			return;
		}
		BitMatrix matrix = new QRCodeWriter().encode(payload.value(), BarcodeFormat.QR_CODE, 512, 512);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(matrix, "PNG", output);
		byte[] bytes = output.toByteArray();
		String objectName = null;
		String publicUrl = null;
		String base64 = null;
		try {
			GoogleStorageService.StoredImage stored = storageService.storeGeneratedImage(
					bytes, "image/png", "generated-qr", payload.aggregateType() + "-" + payload.aggregateId(), ".png");
			objectName = stored.objectName();
			publicUrl = stored.url();
		} catch (IllegalStateException storageDisabled) {
			base64 = Base64.getEncoder().encodeToString(bytes);
		}
		repository.save(new GeneratedQrAsset(
				payload.aggregateType(), payload.aggregateId(), hash, objectName, publicUrl, base64));
	}
}
