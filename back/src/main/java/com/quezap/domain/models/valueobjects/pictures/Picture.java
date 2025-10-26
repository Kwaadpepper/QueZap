package com.quezap.domain.models.valueobjects.pictures;

import java.util.UUID;

import com.quezap.lib.utils.Domain;

public record Picture(String objectKey, PictureType pictureType) {
  public Picture {
    final var uuidPart =
        UUID.fromString(
            objectKey.substring(objectKey.lastIndexOf('/') + 1, objectKey.lastIndexOf('.')));
    final var extensionPart = objectKey.substring(objectKey.lastIndexOf('.') + 1).toLowerCase();

    Domain.checkDomain(() -> uuidPart.version() == 7, "The objectKey must be a UUIDv7.");
    Domain.checkDomain(
        () -> pictureType.extensions().contains(extensionPart),
        "The objectKey extension does not match the PictureType.");
  }
}
