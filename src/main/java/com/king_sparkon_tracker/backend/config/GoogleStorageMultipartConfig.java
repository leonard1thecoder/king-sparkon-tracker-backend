package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class GoogleStorageMultipartConfig {

	@Bean
	MultipartConfigElement multipartConfigElement(GoogleStorageProperties properties) {
		long maxBytes = Math.max(properties.getMaxFileSizeBytes(), 1L);
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(maxBytes));
		factory.setMaxRequestSize(DataSize.ofBytes(maxBytes + 1024L * 1024L));
		return factory.createMultipartConfig();
	}
}
