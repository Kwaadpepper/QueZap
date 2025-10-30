package com.quezap.application.api.v1.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PictureStreamHelper {
  private static final Logger logger = LoggerFactory.getLogger(PictureStreamHelper.class);

  private PictureStreamHelper() {
    // Utility class
  }

  public static @Nullable InputStream openStream(@Nullable MultipartFile picture)
      throws IOException {
    return picture != null ? picture.getInputStream() : null;
  }

  public static List<@Nullable InputStream> openStreams(List<@Nullable MultipartFile> pictures)
      throws IOException {
    final var streams = new ArrayList<@Nullable InputStream>();

    for (final var picture : pictures) {
      streams.add(openStream(picture));
    }

    return streams;
  }

  public static void closeStream(@Nullable InputStream stream) {
    try {
      if (stream != null) {
        stream.close();
      }
    } catch (IOException e) {
      logger.error("Failed to close InputStream - potential resource leak", e);
    }
  }
}
