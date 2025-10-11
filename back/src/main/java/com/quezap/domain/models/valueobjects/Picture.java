package com.quezap.domain.models.valueobjects;

import java.net.URI;
import java.util.Locale;

import com.quezap.lib.utils.Domain;

public record Picture(URI path, PictureType pictureType) {
  public Picture {
    final var pathStr = path.getPath();
    final var expectedExtension = "." + pictureType.name().toLowerCase(Locale.ROOT);

    Domain.checkDomain(
        () ->
            path.getScheme() == null
                && path.getAuthority() == null
                && path.getQuery() == null
                && path.getFragment() == null,
        "The path must be relative and not contain scheme, authority, query, or fragment.");

    Domain.checkDomain(
        () -> pathStr.toLowerCase(Locale.ROOT).endsWith(expectedExtension),
        "The file extension does not match the picture type. Expected extension: "
            + expectedExtension);
  }
}
