package com.king_sparkon_tracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class GoogleStorageMultipartConfig {

	@Bean
	MultipartConfigElement multipartConfigElement(GoogleStorageProperties properties) {
		long maxBytes = Math.max(properties.getMaxFileSizeBytes(), 1L);
		long maxRequestBytes = maxBytes + 1024L * 1024L;
		return new MultipartConfigElement("", maxBytes, maxRequestBytes, 0);
	}
}
