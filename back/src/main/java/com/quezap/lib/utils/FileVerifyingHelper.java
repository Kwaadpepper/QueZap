package com.quezap.lib.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.web.multipart.MultipartFile;

import org.apache.tika.Tika;
import org.apache.tika.io.FilenameUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for verifying and sanitizing file names and contents. */
public final class FileVerifyingHelper {
  private static final Logger logger = LoggerFactory.getLogger(FileVerifyingHelper.class);

  FileVerifyingHelper() {
    // Utility class
  }

  public static boolean fileNameIsValidAndMatchesExtension(MultipartFile file) {
    final var filename = file.getOriginalFilename();

    if (filename == null || filename.isBlank()) {
      return false;
    }

    final var sanitizedFilename = sanitizeFileName(filename);
    final var fileExtension = getFileExtension(sanitizedFilename);

    if (sanitizedFilename.isBlank() || fileExtension.isBlank()) {
      return false;
    }

    final var expectedMimeType = file.getContentType();
    final var detectedMimeType = getFileMimeType(file);

    return detectedMimeType != null
        && expectedMimeType != null
        && detectedMimeType.equalsIgnoreCase(expectedMimeType);
  }

  public static @Nullable String getFileMimeType(MultipartFile file) {
    final var filename = file.getOriginalFilename();

    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Filename must not be null or blank.");
    }

    final var sanitizedFilename = sanitizeFileName(filename);
    final var tika = new Tika();

    try (var input = file.getInputStream()) {
      return tika.detect(input, sanitizedFilename);
    } catch (Exception e) {
      logger.error("Error while detecting file MIME type", e);
      return null;
    }
  }

  public static String beautifyFileName(String filename) {
    // Reduce consecutive characters.
    filename = filename.replaceAll(" +", "-"); // "file name.zip" becomes 'file-name.zip'
    filename = filename.replaceAll("_+", "-"); // "file___name.zip" becomes 'file-name.zip'
    filename = filename.replaceAll("-+", "-"); // "file---name.zip" becomes 'file-name.zip'
    filename = filename.replaceAll("-\\.{2,}-", "-"); // collapse hyphen · multi-dot · hyphen

    filename = filename.replaceAll("-*\\.-*", "."); // "file--.--.-.--name.zip" becomes
    // "file.name.zip"
    filename = filename.replaceAll("\\.{2,}", "."); // "file...name..zip" becomes "file.name.zip"

    // Lowercase for windows/unix interoperability.
    filename = filename.toLowerCase(Locale.ROOT);

    // Trim leading and trailing '.' and '-'
    filename = filename.replaceAll("(^[.-]+)|([.-]+$)", ""); // ".file-name.-" becomes "file-name"

    return filename;
  }

  public static String sanitizeFileName(final String fileName) {
    final var originalEncoding = detectEncoding(fileName);
    final var originalBytes = fileName.getBytes(originalEncoding);

    // Convert to UTF-8
    var sanitizedFileName = new String(originalBytes, StandardCharsets.UTF_8);

    sanitizedFileName = FilenameUtils.normalize(sanitizedFileName);

    // Define the regex pattern to match reserved characters
    final var regex =
        // file system reserved.
        "[<>:\"/\\\\|?*]|"
            + // control characters.
            "[\\x00-\\x1F]|"
            + // non-printing characters DEL, NO-BREAK SPACE, SOFT HYPHEN
            "[\\x7F\\xA0\\xAD]|"
            + // URI reserved
            "[#\\[\\]@!$&'()+,;=]|"
            + // URL unsafe characters
            "[{}^~`´’'ʼ]";

    // Remove unauthorized chars
    sanitizedFileName = sanitizedFileName.replaceAll(regex, "-");

    // Keep only authorized ones
    sanitizedFileName = sanitizedFileName.replaceAll("[^a-zA-Z0-9\\.\\-_~]", "-");

    // * Avoids '.' '..' or '.hiddenFiles' .
    sanitizedFileName = sanitizedFileName.replaceFirst("^[\\.\\-]", "");
    sanitizedFileName = FileVerifyingHelper.beautifyFileName(sanitizedFileName);

    sanitizedFileName =
        Math.abs(UUID.randomUUID().getMostSignificantBits()) + "-" + sanitizedFileName;

    // Maximize filename length to 255 bytes.
    final var extension = getFileExtension(sanitizedFileName);
    final var nameWithoutExtension = getFileNameWithoutExtension(sanitizedFileName);
    final var encodingForTruncation = StandardCharsets.UTF_8;

    final var maxLength = 255 - (extension.isEmpty() ? 0 : extension.length() + 1);
    final var truncatedName =
        truncateString(nameWithoutExtension, maxLength, encodingForTruncation);

    return truncatedName + (extension.isEmpty() ? "" : "." + extension);
  }

  private static Charset detectEncoding(final String filename) {
    final var detector = new CharsetDetector();
    detector.setText(filename.getBytes());

    final var charsetMatch =
        Stream.of(detector.detectAll()).filter(match -> match.getConfidence() > 50).findFirst();

    if (charsetMatch.isPresent()) {
      return Charset.forName(charsetMatch.get().getName());
    } else {
      return StandardCharsets.UTF_8;
    }
  }

  private static String getFileExtension(final String filename) {
    final var lastDotIndex = filename.lastIndexOf('.');
    return lastDotIndex != -1 && lastDotIndex < filename.length() - 1
        ? filename.substring(lastDotIndex + 1)
        : "";
  }

  private static String getFileNameWithoutExtension(final String filename) {
    final var lastDotIndex = filename.lastIndexOf('.');
    return lastDotIndex != -1 ? filename.substring(0, lastDotIndex) : filename;
  }

  private static String truncateString(
      final String str, final int maxLength, final Charset encoding) {
    final var bytes = str.getBytes(encoding);

    if (bytes.length <= maxLength) {
      return str;
    }

    return new String(bytes, 0, maxLength, encoding);
  }
}
