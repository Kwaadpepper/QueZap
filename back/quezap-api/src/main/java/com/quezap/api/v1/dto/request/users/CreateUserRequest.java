package com.quezap.api.v1.dto.request.users;

import java.security.SecureRandom;

import org.jspecify.annotations.Nullable;

public record CreateUserRequest(
    @Nullable String name,
    @Nullable String identifier,
    @Nullable String email,
    @Nullable String password) {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  private static final String DIGITS = "0123456789";
  private static final String SPECIAL = "!@#$%^&*()-_=+";
  private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;

  public CreateUserRequest(String name, String identifier, String password) {
    this(name, identifier, identifier, password);
  }

  public String getEffectiveName() {
    if (name != null && !name.isBlank()) {
      return name;
    }
    final var id = getEffectiveIdentifier();
    final var atIndex = id.indexOf('@');
    if (atIndex > 0) {
      return id.substring(0, atIndex);
    }
    return "User";
  }

  public String getEffectiveIdentifier() {
    if (identifier != null && !identifier.isBlank()) {
      return identifier;
    }
    if (email != null && !email.isBlank()) {
      return email;
    }
    return "";
  }

  public String getEffectivePassword() {
    if (password != null && !password.isBlank()) {
      return password;
    }
    return generateSecurePassword();
  }

  private static String generateSecurePassword() {
    final var chars = new char[24];
    chars[0] = UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length()));
    chars[1] = LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length()));
    chars[2] = DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length()));
    chars[3] = SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length()));
    for (int i = 4; i < chars.length; i++) {
      chars[i] = ALL_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_CHARS.length()));
    }
    for (int i = chars.length - 1; i > 0; i--) {
      final int j = SECURE_RANDOM.nextInt(i + 1);
      final char tmp = chars[i];
      chars[i] = chars[j];
      chars[j] = tmp;
    }
    return new String(chars);
  }
}
