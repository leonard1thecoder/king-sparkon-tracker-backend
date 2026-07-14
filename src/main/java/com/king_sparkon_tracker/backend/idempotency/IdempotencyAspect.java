package com.king_sparkon_tracker.backend.idempotency;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.HexFormat;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.security.AuthenticatedActor;
import com.king_sparkon_tracker.backend.service.AuthenticatedActorService;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class IdempotencyAspect {

	private static final String HEADER = "Idempotency-Key";
	private static final int MAX_KEY_LENGTH = 128;

	private final IdempotencyService idempotencyService;
	private final AuthenticatedActorService actorService;
	private final ObjectMapper objectMapper;

	public IdempotencyAspect(
			IdempotencyService idempotencyService,
			AuthenticatedActorService actorService,
			ObjectMapper objectMapper) {
		this.idempotencyService = idempotencyService;
		this.actorService = actorService;
		this.objectMapper = objectMapper;
	}

	@Around("@annotation(annotation)")
	public Object around(ProceedingJoinPoint joinPoint, IdempotentRequest annotation) throws Throwable {
		HttpServletRequest request = currentRequest();
		String key = requiredKey(request.getHeader(HEADER));
		String username = currentUsername();
		AuthenticatedActor actor = actorService.current(username);
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		String requestHash = requestHash(request, method, joinPoint.getArgs());
		IdempotencyService.Claim claim = idempotencyService.claim(
				key,
				annotation.scope(),
				username,
				actor.businessId(),
				requestHash,
				annotation.ttlSeconds());

		if (claim.replay()) {
			return replay(method, claim.record());
		}

		try {
			Object response = joinPoint.proceed();
			StoredResponse stored = storedResponse(method, response);
			idempotencyService.complete(
					claim.record().getId(),
					stored.body(),
					stored.type(),
					stored.httpStatus());
			return response;
		} catch (Throwable throwable) {
			idempotencyService.fail(claim.record().getId(), throwable);
			throw throwable;
		}
	}

	private Object replay(Method method, IdempotencyRecord record) throws Exception {
		if (method.getReturnType() == Void.TYPE) {
			return null;
		}
		if (ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
			JavaType generic = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
			JavaType bodyType = generic.containedTypeCount() == 0
					? objectMapper.getTypeFactory().constructType(Object.class)
					: generic.containedType(0);
			Object body = record.getResponseBody() == null ? null : objectMapper.readValue(record.getResponseBody(), bodyType);
			return ResponseEntity.status(record.getHttpStatus() == null ? 200 : record.getHttpStatus()).body(body);
		}
		JavaType returnType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
		return record.getResponseBody() == null ? null : objectMapper.readValue(record.getResponseBody(), returnType);
	}

	private StoredResponse storedResponse(Method method, Object response) throws Exception {
		if (response instanceof ResponseEntity<?> entity) {
			Object body = entity.getBody();
			return new StoredResponse(
					body == null ? null : objectMapper.writeValueAsString(body),
					body == null ? "void" : body.getClass().getName(),
					entity.getStatusCode().value());
		}
		ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
		int status = responseStatus == null ? HttpStatus.OK.value() : responseStatus.code().value();
		return new StoredResponse(
				response == null ? null : objectMapper.writeValueAsString(response),
				method.getGenericReturnType().getTypeName(),
				status);
	}

	private String requestHash(HttpServletRequest request, Method method, Object[] arguments) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(request.getMethod().getBytes(StandardCharsets.UTF_8));
		digest.update(request.getRequestURI().getBytes(StandardCharsets.UTF_8));
		digest.update(method.toGenericString().getBytes(StandardCharsets.UTF_8));
		for (Object argument : arguments) {
			if (argument == null || argument instanceof Principal || argument instanceof Authentication) {
				continue;
			}
			digest.update(objectMapper.writeValueAsBytes(argument));
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest();
		}
		throw new IllegalStateException("Idempotent request requires an HTTP request context");
	}

	private String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
			throw new IllegalArgumentException("Authenticated principal is required for idempotent requests");
		}
		return authentication.getName();
	}

	private String requiredKey(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(HEADER + " header is required");
		}
		String key = value.trim();
		if (key.length() > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException(HEADER + " cannot exceed " + MAX_KEY_LENGTH + " characters");
		}
		return key;
	}

	private record StoredResponse(String body, String type, int httpStatus) {
	}
}
