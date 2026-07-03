package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateWorkerRequest(
		@Schema(description = "Worker login username.", example = "worker") @NotBlank String username,
		@Schema(description = "Worker email address.", example = "worker@example.com") @NotBlank @Email String emailAddress,
		@Schema(description = "Worker password.", example = "secret") @NotBlank String password,
		@Schema(description = "Worker cellphone number used for account contact and WhatsApp flows.", example = "+27821234567") @NotBlank @Size(max = 32) String cellphoneNumber,
		@Schema(description = "Worker job title.", example = "Cashier") @NotBlank @Size(max = 120) String jobTitle,
		@Schema(description = "Whether this worker should have a static QR code for customer tips.", example = "true") Boolean tipQrCodeEnabled,
		@Schema(description = "Optional worker profile-picture URL.", example = "https://cdn.example.com/profiles/cashier.png") @Size(max = 2048) String profilePictureUrl) {

	public CreateWorkerRequest(String username, String emailAddress, String password) {
		this(username, emailAddress, password, "+27000000000", "Worker", false, null);
	}

	public CreateWorkerRequest(String username, String emailAddress, String password, String jobTitle, Boolean tipQrCodeEnabled) {
		this(username, emailAddress, password, "+27000000000", jobTitle, tipQrCodeEnabled, null);
	}
}
