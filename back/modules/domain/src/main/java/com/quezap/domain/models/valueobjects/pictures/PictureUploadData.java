package com.quezap.domain.models.valueobjects.pictures;

import java.io.InputStream;

import com.quezap.lib.utils.Domain;

public record PictureUploadData(InputStream inputStream, long contentLength, PictureType type) {
  public PictureUploadData {
    Domain.checkDomain(
        () -> inputStream != null, "Input stream cannot be null for picture upload data");
    Domain.checkDomain(
        () -> contentLength > 0,
        "Content length must be greater than zero for picture upload data");
  }
}
