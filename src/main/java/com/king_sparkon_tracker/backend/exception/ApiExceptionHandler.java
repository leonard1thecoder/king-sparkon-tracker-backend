package com.king_sparkon_tracker.backend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final int MAX_ERROR_MESSAGE_LENGTH = 240;

	@ExceptionHandler(DuplicateUsernameException.class)
	ResponseEntity<Map<String, Object>> handleDuplicateUsername(DuplicateUsernameException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_USERNAME, exception.getMessage(), request);
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, ErrorCode.EMAIL_NOT_VERIFIED, exception.getMessage(), request);
	}

	@ExceptionHandler(DuplicateEmailAddressException.class)
	ResponseEntity<Map<String, Object>> handleDuplicateEmailAddress(DuplicateEmailAddressException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_EMAIL_ADDRESS, exception.getMessage(), request);
	}

	@ExceptionHandler(DuplicateBarcodeException.class)
	ResponseEntity<Map<String, Object>> handleDuplicateBarcode(DuplicateBarcodeException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_BARCODE, exception.getMessage(), request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED, exception.getMessage(), request);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<Map<String, Object>> handleMalformedRequestBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
		String message = "Malformed request body";
		Throwable mostSpecificCause = exception.getMostSpecificCause();
		if (mostSpecificCause != null && mostSpecificCause.getMessage() != null && !mostSpecificCause.getMessage().isBlank()) {
			message = "Malformed request body: " + sanitizeErrorMessage(mostSpecificCause.getMessage());
		}
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
		ErrorCode code = exception.getMessage() != null && exception.getMessage().toLowerCase().contains("stock")
				? ErrorCode.INSUFFICIENT_STOCK
				: ErrorCode.VALIDATION_FAILED;
		return error(HttpStatus.BAD_REQUEST, code, exception.getMessage(), request);
	}

	@ExceptionHandler(RuntimeException.class)
	ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException exception, HttpServletRequest request) {
		log.error("api_unhandled_error path={} requestId={} reason={}", request.getRequestURI(), MDC.get("requestId"), exception.getMessage(), exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected server error", request);
	}

	private ResponseEntity<Map<String, Object>> error(HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
		log.warn("api_error status={} code={} path={} requestId={} message={}", status.value(), code, request.getRequestURI(), MDC.get("requestId"), message);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("code", code.name());
		body.put("message", message);
		body.put("path", request.getRequestURI());
		body.put("requestId", MDC.get("requestId"));
		return ResponseEntity.status(status).body(body);
	}

	private String sanitizeErrorMessage(String message) {
		String sanitized = message.replaceAll("\\s+", " ").trim();
		int sourceIndex = sanitized.indexOf(" at [Source");
		if (sourceIndex > 0) {
			sanitized = sanitized.substring(0, sourceIndex).trim();
		}
		if (sanitized.length() > MAX_ERROR_MESSAGE_LENGTH) {
			return sanitized.substring(0, MAX_ERROR_MESSAGE_LENGTH - 3) + "...";
		}
		return sanitized;
	}
}