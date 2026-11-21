package com.quezap.api.v1.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

public final class ClientIpResolver {

  private ClientIpResolver() {
    // Utility class
  }

  public static @Nullable String resolve(@Nullable HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    final var realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }

    final var forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      final var entries = forwardedFor.split(",", -1);
      for (int i = entries.length - 1; i >= 0; i--) {
        final var candidate = entries[i].trim();
        if (!candidate.isEmpty()) {
          return candidate;
        }
      }
    }

    final var remoteAddr = request.getRemoteAddr();
    if (remoteAddr == null || remoteAddr.isBlank()) {
      return null;
    }
    return remoteAddr.trim();
  }
}
