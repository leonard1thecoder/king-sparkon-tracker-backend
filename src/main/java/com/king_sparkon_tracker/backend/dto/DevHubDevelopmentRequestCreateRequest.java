package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevHubDevelopmentRequestCreateRequest(
		@NotBlank(message = "clientName is required")
		@Size(max = 160, message = "clientName must not exceed 160 characters")
		@Schema(example = "Sizolwakhe Leonard")
		String clientName,

		@NotBlank(message = "emailAddress is required")
		@Email(message = "emailAddress must be valid")
		@Size(max = 180, message = "emailAddress must not exceed 180 characters")
		@Schema(example = "client@example.com")
		String emailAddress,

		@Size(max = 80, message = "phoneNumber must not exceed 80 characters")
		@Schema(example = "+27 72 000 0000")
		String phoneNumber,

		@Size(max = 180, message = "companyName must not exceed 180 characters")
		@Schema(example = "King Sparkon AI")
		String companyName,

		@NotBlank(message = "projectType is required")
		@Size(max = 120, message = "projectType must not exceed 120 characters")
		@Schema(example = "E-commerce web app")
		String projectType,

		@NotBlank(message = "title is required")
		@Size(max = 180, message = "title must not exceed 180 characters")
		@Schema(example = "Build a barcode marketplace with payments")
		String title,

		@NotBlank(message = "description is required")
		@Size(max = 5000, message = "description must not exceed 5000 characters")
		@Schema(example = "I need a Next.js and Spring Boot marketplace with barcode scanning, Stripe payments, and admin dashboard.")
		String description,

		@Size(max = 120, message = "budgetRange must not exceed 120 characters")
		@Schema(example = "R15,000 - R30,000")
		String budgetRange,

		@Size(max = 120, message = "timeline must not exceed 120 characters")
		@Schema(example = "4 weeks")
		String timeline
) {
}
