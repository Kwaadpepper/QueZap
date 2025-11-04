package com.quezap.infrastructure.adapter.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.ports.services.IdentifierHasher;

@Service
public class IdentifierHasherImpl implements IdentifierHasher {

  @Override
  public HashedIdentifier hash(RawIdentifier identifier) {
    return new HashedIdentifier(sha512(identifier.value()));
  }

  private String sha512(final String base) throws HashException {
    return shaHash(base, "SHA-512");
  }

  private String shaHash(final String base, final String algorithm) throws HashException {
    try {
      final MessageDigest digest = MessageDigest.getInstance(algorithm);
      final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
      final StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < hash.length; i++) {
        final String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new HashException("Error hashing input with SHA-256", e);
    }
  }

  public static class HashException extends RuntimeException {
    public HashException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
