package com.quezap.api.v1.services;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.quezap.api.v1.exceptions.InvalidFileException;
import com.quezap.lib.utils.FileVerifyingHelper;

@Service
public class PictureMetadataValidatorService {

  private static final Set<String> ALLOWED_PICTURE_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  public String validateAndGetMimeType(final MultipartFile picture) {
    // 1. Validation de base (taille/présence)
    if (picture.isEmpty()) {
      throw new InvalidFileException("Picture file must not be empty.");
    }

    if (!FileVerifyingHelper.fileNameIsValidAndMatchesExtension(picture)) {
      throw new InvalidFileException(
          "Picture file name is invalid or does not match its actual content.");
    }

    final String mimeType = FileVerifyingHelper.getFileMimeType(picture);
    if (mimeType == null) {
      throw new InvalidFileException("Could not determine picture MIME type.");
    }

    if (!ALLOWED_PICTURE_MIME_TYPES.contains(mimeType)) {
      throw new InvalidFileException(
          "Picture file must be one of the following types: "
              + String.join(", ", ALLOWED_PICTURE_MIME_TYPES));
    }

    return mimeType;
  }
}
