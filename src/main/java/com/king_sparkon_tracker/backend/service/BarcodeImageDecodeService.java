package com.king_sparkon_tracker.backend.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.king_sparkon_tracker.backend.dto.BarcodeImageDecodeResponse;

@Service
public class BarcodeImageDecodeService {

	private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
	private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
			"image/png",
			"image/jpeg",
			"image/jpg"
	);

	public BarcodeImageDecodeResponse decode(MultipartFile file) {
		validateImage(file);

		try {
			BufferedImage image = ImageIO.read(file.getInputStream());
			if (image == null) {
				throw new IllegalArgumentException("Uploaded file is not a readable image");
			}

			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
			Result result = new MultiFormatReader().decode(
					bitmap,
					Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE)
			);

			return new BarcodeImageDecodeResponse(
					result.getText(),
					"ZXING",
					result.getBarcodeFormat().name(),
					"Barcode image decoded successfully"
			);
		} catch (NotFoundException exception) {
			throw new IllegalArgumentException("No barcode could be decoded from the image. Retake the picture with better lighting and keep the barcode straight.");
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to read uploaded barcode image", exception);
		}
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Barcode image is required");
		}

		if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
			throw new IllegalArgumentException("Barcode image must not exceed 5MB");
		}

		String contentType = file.getContentType();
		if (contentType == null || ALLOWED_CONTENT_TYPES.stream().noneMatch(contentType::equalsIgnoreCase)) {
			throw new IllegalArgumentException("Only PNG, JPG, and JPEG barcode images are supported");
		}
	}
}
