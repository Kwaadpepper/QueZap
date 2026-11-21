package com.quezap.infrastructure.adapter.services;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.infrastructure.adapter.services.config.IdentifierHasherConfig;

@Service
public class IdentifierHasherImpl implements IdentifierHasher {
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final SecretKey hmacKey;

  public IdentifierHasherImpl() {
    this(new IdentifierHasherConfig(IdentifierHasherConfig.DEFAULT_DEV_PEPPER));
  }

  @Autowired
  public IdentifierHasherImpl(IdentifierHasherConfig config) {
    this.hmacKey = config.hmacKey();
  }

  @Override
  public HashedIdentifier hash(RawIdentifier identifier) {
    return new HashedIdentifier(hmacSha256(identifier.value()));
  }

  private String hmacSha256(final String base) throws HashException {
    try {
      final var mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);
      final var hash = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new HashException("Error hashing input with HMAC-SHA256", e);
    }
  }

  public static class HashException extends RuntimeException {
    public HashException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
