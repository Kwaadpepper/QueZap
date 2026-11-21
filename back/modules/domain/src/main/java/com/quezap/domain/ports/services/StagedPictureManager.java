package com.quezap.domain.ports.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/** Domain port managing the lifecycle of temporarily staged pictures and picture URL validation. */
public interface StagedPictureManager {

  int MAX_ACTIVE_STAGED_PICTURES_PER_USER = 50;
  String PUBLIC_BUCKET_PREFIX = "/quezap-public-images/";
  String OBJECT_KEY_REGEX = "pictures/questions/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)";
  Pattern VALID_PICTURE_PATTERN =
      Pattern.compile("^(/quezap-public-images/)?" + OBJECT_KEY_REGEX + "$");
  Pattern VALID_OBJECT_KEY_PATTERN = Pattern.compile("^" + OBJECT_KEY_REGEX + "$");

  /**
   * Registers an uploaded picture in staging storage with a TTL.
   *
   * @param objectKey S3 object key (e.g. {@code pictures/questions/<uuid>.png})
   * @param pictureUrl public relative picture URL
   * @param uploaderId identifier of the authenticated user who uploaded the picture
   * @param ttl optional time-to-live (defaults to implementation TTL if null)
   * @return metadata of the staged picture including expiration timestamp
   */
  StagedPictureInfo registerStagedPicture(
      String objectKey, String pictureUrl, UUID uploaderId, @Nullable Duration ttl);

  /**
   * Claims staged pictures on behalf of {@code uploaderId} and removes them from staging.
   *
   * <p>Rejects any URL staged by another user or any URL not currently staged by {@code
   * uploaderId}.
   *
   * @param pictureUrls picture URLs newly attached to a quiz
   * @param uploaderId identifier of the authenticated author performing the save
   */
  void claimPictures(Collection<String> pictureUrls, UUID uploaderId);

  /**
   * Purges expired staged pictures from both object storage and persistence.
   *
   * @return number of pictures deleted
   */
  int purgeExpiredPictures();

  /**
   * Counts active (non-expired) staged pictures uploaded by {@code uploaderId}.
   *
   * @param uploaderId identifier of the authenticated user
   * @return active staged picture count
   */
  default long countActiveStagedPictures(UUID uploaderId) {
    return 0L;
  }

  /**
   * Resolves and validates a user-supplied picture reference into a canonical public picture URL.
   *
   * @param rawPicture raw user input (nullable or blank)
   * @return canonical public picture URL, or {@code null} if {@code rawPicture} is null/blank
   */
  default @Nullable String resolveValidPublicPictureUrl(@Nullable String rawPicture) {
    return validateAndNormalizePublicUrl(rawPicture);
  }

  /**
   * Converts a stored object key or internal picture URL to its canonical public URL.
   *
   * @param pictureFilename stored filename, object key, or public URL
   * @return public URL or {@code null} if null/blank/invalid
   */
  default @Nullable String toPublicUrl(@Nullable String pictureFilename) {
    return normalizePublicUrl(pictureFilename);
  }

  /**
   * Extracts the S3 object key from a public picture URL or key.
   *
   * @param pictureUrl picture URL or key
   * @return S3 object key or {@code null} if null/blank/invalid
   */
  default @Nullable String toObjectKey(@Nullable String pictureUrl) {
    return extractObjectKey(pictureUrl);
  }

  /**
   * Validates user-supplied picture input and returns its canonical public URL.
   *
   * @param rawPicture user-supplied picture filename or URL
   * @return canonical public URL, or {@code null} when {@code rawPicture} is null or blank
   * @throws InvalidPictureReferenceException if {@code rawPicture} is external, contains path
   *     traversal, base64 data URLs, or violates the strict internal picture key pattern
   */
  static @Nullable String validateAndNormalizePublicUrl(@Nullable String rawPicture) {
    if (rawPicture == null || rawPicture.isBlank()) {
      return null;
    }
    final var trimmed = rawPicture.trim();
    if (trimmed.startsWith("data:")) {
      throw new InvalidPictureReferenceException(
          "Base64 data URLs are not supported. Upload image to /apiv1/pictures/upload first.");
    }
    if (trimmed.contains("..") || !VALID_PICTURE_PATTERN.matcher(trimmed).matches()) {
      throw new InvalidPictureReferenceException(
          "Invalid picture reference. Must match "
              + "(/quezap-public-images/)?pictures/questions/<uuid>.(jpg|jpeg|png|webp)");
    }
    return trimmed.startsWith(PUBLIC_BUCKET_PREFIX) ? trimmed : PUBLIC_BUCKET_PREFIX + trimmed;
  }

  /**
   * Normalizes a persisted picture path/key into a safe internal public URL, returning {@code null}
   * for empty or unsafe values.
   */
  static @Nullable String normalizePublicUrl(@Nullable String pictureFilename) {
    if (pictureFilename == null || pictureFilename.isBlank()) {
      return null;
    }
    final var trimmed = pictureFilename.trim();
    if (trimmed.contains("..")) {
      return null;
    }
    if (trimmed.startsWith(PUBLIC_BUCKET_PREFIX)) {
      final var key = trimmed.substring(PUBLIC_BUCKET_PREFIX.length());
      return VALID_OBJECT_KEY_PATTERN.matcher(key).matches() ? trimmed : null;
    }
    if (trimmed.startsWith("quezap-public-images/")) {
      final var key = trimmed.substring("quezap-public-images/".length());
      return VALID_OBJECT_KEY_PATTERN.matcher(key).matches() ? "/" + trimmed : null;
    }
    if (VALID_OBJECT_KEY_PATTERN.matcher(trimmed).matches()) {
      return PUBLIC_BUCKET_PREFIX + trimmed;
    }
    return null;
  }

  /**
   * Extracts a validated S3 object key ({@code pictures/questions/<uuid>.<ext>}) from a picture URL
   * or key, returning {@code null} if invalid or unsafe.
   */
  static @Nullable String extractObjectKey(@Nullable String pictureUrl) {
    if (pictureUrl == null || pictureUrl.isBlank()) {
      return null;
    }
    final var trimmed = pictureUrl.trim();
    if (trimmed.contains("..")) {
      return null;
    }
    final String candidate;
    if (trimmed.startsWith(PUBLIC_BUCKET_PREFIX)) {
      candidate = trimmed.substring(PUBLIC_BUCKET_PREFIX.length());
    } else if (trimmed.startsWith("quezap-public-images/")) {
      candidate = trimmed.substring("quezap-public-images/".length());
    } else {
      candidate = trimmed;
    }
    return VALID_OBJECT_KEY_PATTERN.matcher(candidate).matches() ? candidate : null;
  }

  /** Metadata of a newly staged picture. */
  record StagedPictureInfo(
      String objectKey, String pictureUrl, UUID uploaderId, Instant expiresAt) {}

  /** Thrown when a picture reference is invalid, external, or unstaged. */
  class InvalidPictureReferenceException extends IllegalArgumentException {
    public InvalidPictureReferenceException(String message) {
      super(message);
    }
  }

  /** Thrown when a user attempts to claim a picture staged by another user. */
  class PictureOwnershipException extends RuntimeException {
    public PictureOwnershipException(String message) {
      super(message);
    }
  }

  /** Thrown when a user exceeds the active staged picture upload quota. */
  class StagedPictureQuotaExceededException extends RuntimeException {
    public StagedPictureQuotaExceededException(String message) {
      super(message);
    }
  }
}
