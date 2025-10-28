package com.quezap.lib.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.springframework.web.multipart.MultipartFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the FileVerifyingHelper class, focusing on file name sanitization and validation
 * logic.
 */
@ExtendWith(MockitoExtension.class)
class FileVerifyingHelperTest {

  // Helper class constructor test
  @Test
  @DisplayName("Should allow construction of the utility class (for coverage)")
  void constructorTest() {
    // GIVEN
    // WHEN
    new FileVerifyingHelper();
    // THEN
    // Just ensures no exception is thrown, typical for utility classes
  }

  @Nested
  @DisplayName("Tests for fileNameIsValidAndMatchesExtension")
  class FileNameIsValidAndMatchesExtensionTest {

    // Note: Full Tika mocking is complex. These tests focus on the main
    // logic flow and error handling around the MultipartFile inputs.

    @ParameterizedTest(name = "Should return false for null or blank original filenames: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldReturnFalseForNullOrBlankFilenames(String filename) {
      // GIVEN
      final MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn(filename);

      // WHEN
      final boolean isValid = FileVerifyingHelper.fileNameIsValidAndMatchesExtension(mockFile);

      // THEN
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false on exception during Tika processing (e.g., I/O error)")
    void shouldReturnFalseOnIoException() throws Exception {
      // GIVEN
      final String validFilename = "file.txt";
      final MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn(validFilename);
      when(mockFile.getInputStream())
          .thenThrow(new RuntimeException("Simulated I/O Error")); // Simulate I/O error

      // WHEN
      final boolean isValid = FileVerifyingHelper.fileNameIsValidAndMatchesExtension(mockFile);

      // THEN
      assertThat(isValid).isFalse();
    }

    // --- Success Path Example (Requires Tika integration or deep Mocking) ---

    // In a real project, this would be an integration test or require mocking Tika/CharsetDetector
    @Test
    @DisplayName("Should return true when Tika detects the same MIME type as ContentType")
    void shouldReturnTrueWhenMimeTypeMatches() throws Exception {
      // GIVEN
      final String filename = "test-file.pdf";
      final String contentType = "application/pdf";
      final String fileContent =
          """
          %PDF-1.0
          1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
          2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
          3 0 obj<</Type/Page/Parent 2 0 R/Resources<<>>/MediaBox[0 0 9 9]>>endobj
          xref
          0 4
          0000000000 65535 f
          0000000009 00000 n
          0000000052 00000 n
          0000000101 00000 n
          trailer<</Root 1 0 R/Size 4>>
          startxref
          174""";

      final MultipartFile mockFile = mock(MultipartFile.class);
      when(mockFile.getOriginalFilename()).thenReturn(filename);
      when(mockFile.getContentType()).thenReturn(contentType);
      when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));

      // WHEN
      final boolean isValid = FileVerifyingHelper.fileNameIsValidAndMatchesExtension(mockFile);

      // THEN
      // This test is highly dependent on Tika's behavior. Assuming a proper setup, it should pass.
      // For a true unit test, Tika's behavior would need to be stubbed/mocked, which is complex.
      assertThat(isValid).isTrue();
    }
  }

  @Nested
  @DisplayName("Tests for beautifyFileName")
  class BeautifyFileNameTest {

    private static Stream<Arguments> beautifyFileNameCases() {
      return Stream.of(
          Arguments.of(
              "file name.zip", "file-name.zip", "Should replace spaces with single hyphen"),
          Arguments.of(
              "file___name.zip",
              "file-name.zip",
              "Should replace multiple underscores with single hyphen"),
          Arguments.of(
              "file---name.zip",
              "file-name.zip",
              "Should replace multiple hyphens with single hyphen"),
          Arguments.of(
              "file--.--.-.--name.zip",
              "file.name.zip",
              "Should clean up surrounding dots/hyphens"),
          Arguments.of(
              "file...name..zip", "file.name.zip", "Should replace multiple dots with single dot"),
          Arguments.of(".file-name.-", "file-name", "Should trim leading and trailing dot/hyphen"),
          Arguments.of(
              "File Name With Mïxed CaSes.TXT",
              "file-name-with-mïxed-cases.txt",
              "Should lowercase and apply hyphen rules"),
          Arguments.of(
              "filename.with.many.dots.and.hyphens",
              "filename.with.many.dots.and.hyphens",
              "Should not alter already clean names"),
          Arguments.of(
              "..---file.name---..", "file.name", "Should handle extreme leading/trailing chars"),
          Arguments.of("File_Name-..-Test.PDF", "file-name-test.pdf", "Complex mixed case"));
    }

    @ParameterizedTest(name = "{2} - Input: \"{0}\" -> Expected: \"{1}\"")
    @MethodSource("beautifyFileNameCases")
    void shouldBeautifyFileNameCorrectly(String input, String expected, String description) {
      // GIVEN
      // input is provided by @MethodSource

      // WHEN
      final String result = FileVerifyingHelper.beautifyFileName(input);

      // THEN
      assertThat(result).as(description).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("Tests for sanitizeFileName")
  class SanitizeFileNameTest {

    // Note: Since this method uses UUID and internal/private methods (detectEncoding,
    // truncateString, beautifyFileName),
    // we mainly check the core sanitization rules, and verify the UUID/length logic.

    @ParameterizedTest(name = "Should remove reserved/unsafe characters from: \"{0}\"")
    @ValueSource(
        strings = {
          "file<>:\"/\\|?*#[]@!$&'()+;=^{}~`´’'ʼ.txt", // Reserved, URI reserved, URL unsafe, and
          // control chars (if present)
          "file\u0000\u001F\u007F\u00A0\u00AD.txt", // Control and non-printing chars
          "file-name.test.pdf" // Ajout de ce cas pour s'assurer que .pdf est supporté
        })
    void shouldRemoveUnsafeCharactersAndAppendUuid(String input) {
      // GIVEN
      final String expectedExtension = input.substring(input.lastIndexOf('.') + 1);
      final String expectedPattern = "\\d+-[a-z0-9\\.\\-_]+\\." + expectedExtension.toLowerCase();

      // WHEN
      final String result = FileVerifyingHelper.sanitizeFileName(input);

      // THEN
      assertThat(result)
          .doesNotContain(
              "<", ">", ":", "\"", "/", "\\", "|", "?", "*", "#", "[", "]", "@", "!", "$", "&", "'",
              "(", ")", "+", ";", "=", "{", "}", "^", "~", "`", "´", "’", "ʼ")

          // Check for UUID prefix and correct extension suffix.
          .matches(expectedPattern)
          // Check that leading '.' or '-' were removed (and beautified)
          .doesNotMatch("^\\d+-[\\.\\-].*");

      // 3. Check for max length (255 bytes limit)
      assertThat(result.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(255);
    }

    @Test
    @DisplayName("Should handle very long filenames and truncate to max byte length (255)")
    void shouldTruncateLongFilenameCorrectly() {
      // GIVEN
      final String extension = ".pdf";
      // Create a very long name part (e.g., 300 characters of 'a')
      final String longNamePart = "a".repeat(300);
      final String longFileName = longNamePart + extension;

      // WHEN
      final String result = FileVerifyingHelper.sanitizeFileName(longFileName);

      // THEN
      // 1. Check that the name has been truncated (255 bytes total limit)
      assertThat(result.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(255);
      // 2. Check that the extension is preserved
      assertThat(result).endsWith(extension);
      // 3. Check for UUID prefix
      assertThat(result).matches("\\d+-[a-z\\-]+\\.pdf");
    }

    @Test
    @DisplayName("Should handle filenames without extension")
    void shouldHandleFilenamesWithoutExtension() {
      // GIVEN
      final String fileName = "no-extension-file";

      // WHEN
      final String result = FileVerifyingHelper.sanitizeFileName(fileName);

      // THEN
      // 1. Check that no dot is present at the end
      assertThat(result)
          .doesNotContainPattern("\\.$")
          // 2. Check for UUID prefix
          .matches("\\d+-[a-z\\-]+");
    }

    @Test
    @DisplayName("Should remove leading dot or hyphen even after initial sanitization")
    void shouldRemoveLeadingDotOrHyphen() {
      // GIVEN
      final String leadingDot = ".hidden-file.txt";
      final String leadingHyphen = "-start-file.txt";

      // WHEN
      final String resultDot = FileVerifyingHelper.sanitizeFileName(leadingDot);
      final String resultHyphen = FileVerifyingHelper.sanitizeFileName(leadingHyphen);

      // THEN
      // The result should start with the UUID prefix (e.g., 1234567890-) and not the original
      // leading char.
      assertThat(resultDot).matches("\\d+-hidden-file\\.txt");
      assertThat(resultHyphen).matches("\\d+-start-file\\.txt");
    }
  }
}
