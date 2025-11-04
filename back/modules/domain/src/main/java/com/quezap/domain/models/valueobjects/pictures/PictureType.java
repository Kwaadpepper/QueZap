package com.quezap.domain.models.valueobjects.pictures;

import java.util.List;

public enum PictureType {
  JPG,
  PNG,
  WEBP;

  public String mimeType() {
    return switch (this) {
      case JPG -> "image/jpeg";
      case PNG -> "image/png";
      case WEBP -> "image/webp";
    };
  }

  public List<String> extensions() {
    return switch (this) {
      case JPG -> List.of("jpg", "jpeg");
      case PNG -> List.of("png");
      case WEBP -> List.of("webp");
    };
  }

  public static PictureType fromMimeType(String mimeType) {
    return List.of(values()).stream()
        .filter(type -> type.mimeType().equalsIgnoreCase(mimeType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported MIME type: " + mimeType));
  }
}
