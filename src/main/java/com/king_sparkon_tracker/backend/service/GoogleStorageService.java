package com.king_sparkon_tracker.backend.service;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.king_sparkon_tracker.backend.config.GoogleStorageProperties;

@Service
public class GoogleStorageService {

	private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:(image/[A-Za-z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);
	private static final String GOOGLE_STORAGE_BASE_URL = "https://storage.googleapis.com/";

	private final GoogleStorageProperties properties;
	private volatile Storage storage;

	public GoogleStorageService(GoogleStorageProperties properties) {
		this.properties = properties;
	}

	public StoredImage storeImage(MultipartFile file, String folder, String ownerKey) {
		return storeMultipartImage(file, folder, ownerKey, true);
	}

	public StoredImage storePrivateImage(MultipartFile file, String folder, String ownerKey) {
		return storeMultipartImage(file, folder, ownerKey, false);
	}

	public StoredImage storeGeneratedImage(
			byte[] bytes,
			String contentType,
			String folder,
			String ownerKey,
			String extension) {
		requireEnabled();
		String normalizedType = normalizeContentType(contentType);
		validateImage(normalizedType, bytes == null ? 0 : bytes.length);
		return upload(bytes, normalizedType, objectName(folder, ownerKey, extension), true);
	}

	private StoredImage storeMultipartImage(MultipartFile file, String folder, String ownerKey, boolean publicAccess) {
		requireEnabled();
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Image file is required");
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException exception) {
			throw new IllegalArgumentException("Could not read uploaded image", exception);
		}

		String contentType = normalizeContentType(file.getContentType());
		validateImage(contentType, bytes.length);
		return upload(bytes, contentType, objectName(folder, ownerKey, extensionFor(contentType, file.getOriginalFilename())), publicAccess);
	}

	public String storeImageValue(String value, String folder, String ownerKey) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		String candidate = value.trim();
		if (isStoredUrl(candidate)) {
			return candidate;
		}

		if (candidate.startsWith("data:image/")) {
			return storeDataUri(candidate, folder, ownerKey).url();
		}

		if (isHttpUrl(candidate)) {
			if (properties.isEnabled() && properties.isRejectExternalImageUrls()) {
				throw new IllegalArgumentException("External image URLs are not allowed when Google Storage is enabled. Upload the image to Google Storage first.");
			}
			return candidate;
		}

		throw new IllegalArgumentException("Image value must be a Google Storage URL, an HTTPS URL, or a data:image base64 value");
	}

	public StoredImage storeDataUri(String dataUri, String folder, String ownerKey) {
		requireEnabled();
		Matcher matcher = DATA_URI_PATTERN.matcher(dataUri.trim());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Image data URI must use the format data:image/<type>;base64,<payload>");
		}

		String contentType = normalizeContentType(matcher.group(1));
		byte[] bytes = Base64.getDecoder().decode(matcher.group(2).replaceAll("\\s", ""));
		validateImage(contentType, bytes.length);
		return upload(bytes, contentType, objectName(folder, ownerKey, extensionFor(contentType, null)), true);
	}

	public String signedReadUrl(String objectName, long durationMinutes) {
		requireEnabled();
		if (!StringUtils.hasText(objectName)) {
			return null;
		}

		BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(requiredBucketName(), objectName.trim())).build();
		return storage().signUrl(
				blobInfo,
				Math.max(durationMinutes, 1L),
				TimeUnit.MINUTES,
				Storage.SignUrlOption.withV4Signature())
				.toString();
	}

	private StoredImage upload(byte[] bytes, String contentType, String objectName, boolean publicAccess) {
		BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(requiredBucketName(), objectName))
				.setContentType(contentType)
				.setCacheControl(publicAccess ? "public, max-age=31536000, immutable" : "private, no-store, max-age=0")
				.build();

		Blob blob = storage().create(blobInfo, bytes);
		if (publicAccess && properties.isMakePublic()) {
			blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
		}

		return new StoredImage(objectName, publicUrl(objectName), contentType, bytes.length);
	}

	private Storage storage() {
		Storage local = storage;
		if (local != null) {
			return local;
		}

		synchronized (this) {
			if (storage == null) {
				StorageOptions.Builder builder = StorageOptions.newBuilder();
				if (StringUtils.hasText(properties.getProjectId())) {
					builder.setProjectId(properties.getProjectId().trim());
				}
				storage = builder.build().getService();
			}
			return storage;
		}
	}

	private void requireEnabled() {
		if (!properties.isEnabled()) {
			throw new IllegalStateException("Google Storage is disabled. Set GOOGLE_STORAGE_ENABLED=true before uploading images.");
		}
		requiredBucketName();
	}

	private String requiredBucketName() {
		if (!StringUtils.hasText(properties.getBucketName())) {
			throw new IllegalStateException("GOOGLE_STORAGE_BUCKET_NAME is required for Google Storage uploads");
		}
		return properties.getBucketName().trim();
	}

	private void validateImage(String contentType, long sizeBytes) {
		if (sizeBytes <= 0) {
			throw new IllegalArgumentException("Image cannot be empty");
		}
		if (sizeBytes > properties.getMaxFileSizeBytes()) {
			throw new IllegalArgumentException("Image is larger than the configured Google Storage limit");
		}
		boolean allowed = properties.getAllowedContentTypes().stream()
				.filter(StringUtils::hasText)
				.map(value -> value.toLowerCase(Locale.ROOT).trim())
				.anyMatch(value -> value.equals(contentType));
		if (!allowed) {
			throw new IllegalArgumentException("Unsupported image content type: " + contentType);
		}
	}

	private String normalizeContentType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			throw new IllegalArgumentException("Image content type is required");
		}
		return contentType.trim().toLowerCase(Locale.ROOT);
	}

	private String extensionFor(String contentType, String originalFilename) {
		String original = extensionFromFilename(originalFilename);
		if (StringUtils.hasText(original)) {
			return original;
		}
		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			default -> ".img";
		};
	}

	private String extensionFromFilename(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return null;
		}
		String filename = originalFilename.trim().toLowerCase(Locale.ROOT);
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == filename.length() - 1) {
			return null;
		}
		String extension = filename.substring(dotIndex).replaceAll("[^.a-z0-9]", "");
		return switch (extension) {
			case ".jpg", ".jpeg", ".png", ".webp", ".gif" -> extension;
			default -> null;
		};
	}

	private String objectName(String folder, String ownerKey, String extension) {
		return sanitizePath(properties.getRootFolder(), "king-sparkon-tracker")
				+ "/" + sanitizePath(folder, "general")
				+ "/" + sanitizePath(ownerKey, "shared")
				+ "/" + UUID.randomUUID() + extension;
	}

	private String sanitizePath(String value, String fallback) {
		if (!StringUtils.hasText(value)) {
			return fallback;
		}
		String sanitized = value.trim()
				.replace('\\', '/')
				.replaceAll("[^A-Za-z0-9/_-]", "-")
				.replaceAll("/++", "/")
				.replaceAll("^/+", "")
				.replaceAll("/+$", "");
		return sanitized.isBlank() ? fallback : sanitized.toLowerCase(Locale.ROOT);
	}

	private String publicUrl(String objectName) {
		if (StringUtils.hasText(properties.getPublicBaseUrl())) {
			return trimTrailingSlash(properties.getPublicBaseUrl().trim()) + "/" + objectName;
		}
		return GOOGLE_STORAGE_BASE_URL + requiredBucketName() + "/" + objectName;
	}

	private boolean isStoredUrl(String value) {
		if (StringUtils.hasText(properties.getPublicBaseUrl())
				&& value.startsWith(trimTrailingSlash(properties.getPublicBaseUrl().trim()) + "/")) {
			return true;
		}
		return StringUtils.hasText(properties.getBucketName())
				&& value.startsWith(GOOGLE_STORAGE_BASE_URL + properties.getBucketName().trim() + "/");
	}

	private boolean isHttpUrl(String value) {
		try {
			URI uri = URI.create(value);
			return "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private String trimTrailingSlash(String value) {
		return value.replaceAll("/+$", "");
	}

	public record StoredImage(String objectName, String url, String contentType, long sizeBytes) {
	}
}
