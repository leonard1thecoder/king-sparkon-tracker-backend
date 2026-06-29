package com.king_sparkon_tracker.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.google-storage")
public class GoogleStorageProperties {

	private boolean enabled;
	private String projectId;
	private String bucketName;
	private String publicBaseUrl;
	private String rootFolder = "king-sparkon-tracker";
	private String credentialsJson;
	private String credentialsJsonBase64;
	private boolean makePublic;
	private boolean rejectExternalImageUrls = true;
	private long maxFileSizeBytes = 5L * 1024L * 1024L;
	private List<String> allowedContentTypes = new ArrayList<>(List.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"image/gif"));

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	public String getCredentialsJson() {
		return credentialsJson;
	}

	public void setCredentialsJson(String credentialsJson) {
		this.credentialsJson = credentialsJson;
	}

	public String getCredentialsJsonBase64() {
		return credentialsJsonBase64;
	}

	public void setCredentialsJsonBase64(String credentialsJsonBase64) {
		this.credentialsJsonBase64 = credentialsJsonBase64;
	}

	public boolean isMakePublic() {
		return makePublic;
	}

	public void setMakePublic(boolean makePublic) {
		this.makePublic = makePublic;
	}

	public boolean isRejectExternalImageUrls() {
		return rejectExternalImageUrls;
	}

	public void setRejectExternalImageUrls(boolean rejectExternalImageUrls) {
		this.rejectExternalImageUrls = rejectExternalImageUrls;
	}

	public long getMaxFileSizeBytes() {
		return maxFileSizeBytes;
	}

	public void setMaxFileSizeBytes(long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<String> getAllowedContentTypes() {
		return allowedContentTypes;
	}

	public void setAllowedContentTypes(List<String> allowedContentTypes) {
		this.allowedContentTypes = allowedContentTypes == null ? new ArrayList<>() : new ArrayList<>(allowedContentTypes);
	}
}
