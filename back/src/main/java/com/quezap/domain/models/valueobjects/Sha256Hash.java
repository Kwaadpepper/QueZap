package com.quezap.domain.models.valueobjects;

import java.util.Arrays;

import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public final class Sha256Hash {
  private static final int HASH_LENGTH = 32;
  private final byte[] value;

  public Sha256Hash(byte[] value) {
    Domain.checkDomain(
        () -> value != null && value.length == HASH_LENGTH,
        "SHA-256 hash must be exactly 32 bytes long.");

    this.value = Arrays.copyOf(value, value.length);
  }

  public byte[] value() {
    return Arrays.copyOf(value, value.length);
  }

  public String toHexString() {
    final var sb = new StringBuilder();
    for (byte b : value) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Sha256Hash that = (Sha256Hash) o;
    return Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }
}
