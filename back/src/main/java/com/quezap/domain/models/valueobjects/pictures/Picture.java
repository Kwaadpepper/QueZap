package com.quezap.domain.models.valueobjects.pictures;

import java.util.Locale;
import java.util.UUID;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.Domain;

public record Picture(String objectKey, PictureType pictureType) {
  public Picture {
    final var uuidPart = getUuidPart(objectKey);
    final var extensionPart = getExtension(objectKey);

    Domain.checkDomain(() -> uuidPart.version() == 7, "The objectKey must be a UUIDv7.");
    Domain.checkDomain(
        () -> pictureType.extensions().contains(extensionPart),
        "The objectKey extension does not match the PictureType.");
  }

  private UUID getUuidPart(String objectKey) {
    try {
      final var uuidString =
          objectKey.substring(objectKey.lastIndexOf('/') + 1, objectKey.lastIndexOf('.'));

      return UUID.fromString(uuidString);
    } catch (Exception _) {
      throw new IllegalDomainStateException(
          "The objectKey is invalid, there is no valid uuid part.");
    }
  }

  private String getExtension(String objectKey) {
    try {
      final var extension =
          objectKey.substring(objectKey.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

      Domain.checkDomain(() -> !extension.isBlank(), "The objectKey must have a valid extension.");

      return extension;
    } catch (Exception _) {
      throw new IllegalDomainStateException(
          "The objectKey is invalid, there is no valid extension part.");
    }
  }
}
