package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.service.GoogleStorageService;
import com.king_sparkon_tracker.backend.service.GoogleStorageService.StoredImage;

@RestController
@RequestMapping("/api/v1/pictures")
public class PictureController {

	private final GoogleStorageService googleStorageService;

	public PictureController(GoogleStorageService googleStorageService) {
		this.googleStorageService = googleStorageService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public StoredPictureResponse create(
			@RequestParam("picture") MultipartFile picture,
			@RequestParam(defaultValue = "general") String folder,
			Principal principal) {
		String ownerKey = principal == null ? "shared" : principal.getName();
		return StoredPictureResponse.from(googleStorageService.storeImage(picture, folder, ownerKey));
	}

	public record StoredPictureResponse(String objectName, String url, String contentType, long sizeBytes) {
		static StoredPictureResponse from(StoredImage image) {
			return new StoredPictureResponse(image.objectName(), image.url(), image.contentType(), image.sizeBytes());
		}
	}
}
