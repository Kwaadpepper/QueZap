package com.quezap.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;

import org.springframework.web.multipart.MultipartFile;

import com.quezap.application.api.v1.exceptions.InvalidFileException;
import com.quezap.lib.utils.FileVerifyingHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PictureMetadataValidatorServiceTest {
  private final PictureMetadataValidatorService validatorService;
  private MockedStatic<FileVerifyingHelper> mockedFileVerifyingHelper;

  public PictureMetadataValidatorServiceTest() {
    this.validatorService = new PictureMetadataValidatorService();
  }

  @BeforeEach
  void setUp() {
    mockedFileVerifyingHelper = Mockito.mockStatic(FileVerifyingHelper.class);
  }

  @AfterEach
  void tearDown() {
    mockedFileVerifyingHelper.close();
  }

  @Nested
  @DisplayName("Validation success cases")
  class SuccessCases {
    private static final String VALID_MIME_TYPE = "image/jpeg";

    @Test
    @DisplayName("Should return the MIME type for a valid JPEG file")
    void shouldReturnMimeTypeForValidFile() {
      // GIVEN
      final MultipartFile mockFile = Mockito.mock(MultipartFile.class);
      Mockito.when(mockFile.isEmpty()).thenReturn(false);

      mockedFileVerifyingHelper
          .when(
              () ->
                  FileVerifyingHelper.fileNameIsValidAndMatchesExtension(any(MultipartFile.class)))
          .thenReturn(true);
      mockedFileVerifyingHelper
          .when(() -> FileVerifyingHelper.getFileMimeType(any(MultipartFile.class)))
          .thenReturn(VALID_MIME_TYPE);

      // WHEN
      final String resultMimeType = validatorService.validateAndGetMimeType(mockFile);

      // THEN
      assertThat(resultMimeType).isEqualTo(VALID_MIME_TYPE);
    }
  }

  @Nested
  @DisplayName("Validation failure cases (InvalidFileException 400)")
  class FailureCases {

    @Test
    @DisplayName("Should throw InvalidFileException when file is empty")
    void shouldThrowWhenFileIsEmpty() {
      // GIVEN
      final MultipartFile mockFile = Mockito.mock(MultipartFile.class);
      Mockito.when(mockFile.isEmpty()).thenReturn(true);

      // WHEN / THEN
      assertThatExceptionOfType(InvalidFileException.class)
          .isThrownBy(() -> validatorService.validateAndGetMimeType(mockFile))
          .withMessageContaining("must not be empty");

      mockedFileVerifyingHelper.verify(
          () -> FileVerifyingHelper.fileNameIsValidAndMatchesExtension(Mockito.any()),
          Mockito.never());
    }

    @Test
    @DisplayName("Should throw InvalidFileException when file name is invalid")
    void shouldThrowWhenFileNameIsInvalid() {
      // GIVEN
      final MultipartFile mockFile = Mockito.mock(MultipartFile.class);
      Mockito.when(mockFile.isEmpty()).thenReturn(false);

      mockedFileVerifyingHelper
          .when(
              () ->
                  FileVerifyingHelper.fileNameIsValidAndMatchesExtension(
                      Mockito.any(MultipartFile.class)))
          .thenReturn(false);

      // WHEN / THEN
      assertThatExceptionOfType(InvalidFileException.class)
          .isThrownBy(() -> validatorService.validateAndGetMimeType(mockFile))
          .withMessageContaining("name is invalid");
    }

    @Test
    @DisplayName("Should throw InvalidFileException when MIME type cannot be determined (null)")
    void shouldThrowWhenMimeTypeIsNull() {
      // GIVEN
      final MultipartFile mockFile = Mockito.mock(MultipartFile.class);
      Mockito.when(mockFile.isEmpty()).thenReturn(false);

      mockedFileVerifyingHelper
          .when(
              () ->
                  FileVerifyingHelper.fileNameIsValidAndMatchesExtension(
                      Mockito.any(MultipartFile.class)))
          .thenReturn(true);
      mockedFileVerifyingHelper
          .when(() -> FileVerifyingHelper.getFileMimeType(Mockito.any(MultipartFile.class)))
          .thenReturn(null);

      // WHEN / THEN
      assertThatExceptionOfType(InvalidFileException.class)
          .isThrownBy(() -> validatorService.validateAndGetMimeType(mockFile))
          .withMessageContaining("Could not determine picture MIME type");
    }

    @Test
    @DisplayName(
        "Should throw InvalidFileException for unauthorized MIME type (e.g., application/zip)")
    void shouldThrowForUnauthorizedMimeType() {
      // GIVEN
      final MultipartFile mockFile = Mockito.mock(MultipartFile.class);
      Mockito.when(mockFile.isEmpty()).thenReturn(false);

      mockedFileVerifyingHelper
          .when(
              () ->
                  FileVerifyingHelper.fileNameIsValidAndMatchesExtension(
                      Mockito.any(MultipartFile.class)))
          .thenReturn(true);
      mockedFileVerifyingHelper
          .when(() -> FileVerifyingHelper.getFileMimeType(Mockito.any(MultipartFile.class)))
          .thenReturn("application/zip");

      // WHEN / THEN
      assertThatExceptionOfType(InvalidFileException.class)
          .isThrownBy(() -> validatorService.validateAndGetMimeType(mockFile))
          .withMessageContaining("must be one of the following types");
    }
  }
}
