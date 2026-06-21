package com.king_sparkon_tracker.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.king_sparkon_tracker.backend.dto.ApiError;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/**
	 * Maps duplicate usernames to a conflict response the client can correct.
	 */
	@ExceptionHandler(DuplicateUsernameException.class)
	ResponseEntity<ApiError> handleDuplicateUsername(DuplicateUsernameException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage());
	}


	/**
	 * Maps duplicate email addresses to a conflict response the client can correct.
	 */
	@ExceptionHandler(DuplicateEmailAddressException.class)
	ResponseEntity<ApiError> handleDuplicateEmailAddress(DuplicateEmailAddressException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage());
	}

	/**
	 * Maps duplicate product barcodes to a conflict response for catalogue screens.
	 */
	@ExceptionHandler(DuplicateBarcodeException.class)
	ResponseEntity<ApiError> handleDuplicateBarcode(DuplicateBarcodeException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage());
	}

	/**
	 * Maps authentication failures without exposing which credential was incorrect to API clients.
	 */
	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	/**
	 * Maps missing records to HTTP 404 responses.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	/**
	 * Maps validation and business rule failures to HTTP 400 responses.
	 */
	@ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
	ResponseEntity<ApiError> handleBadRequest(Exception exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	/**
	 * Produces the shared error envelope and logs the sanitized failure reason.
	 */
	private ResponseEntity<ApiError> error(HttpStatus status, String message) {
		log.warn("api_error status={} reason={} message={}", status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), status.getReasonPhrase(), message));
	}
}
