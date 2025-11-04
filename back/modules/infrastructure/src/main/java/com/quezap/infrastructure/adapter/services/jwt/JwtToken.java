package com.quezap.infrastructure.adapter.services.jwt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public interface JwtToken {

  /**
   * Represents the JWT Payload, containing claims (RFC 7519, Section 4).
   *
   * <p>Use the {@link Builder} to construct this record safely.
   *
   * @param sub (Subject) RFC 7519, Sec 4.1.2: Token subject.
   * @param aud (Audience) RFC 7519, Sec 4.1.3: Token audience(s).
   * @param exp (Expiration Time) RFC 7519, Sec 4.1.4: Expiration (NumericDate).
   * @param iat (Issued At) RFC 7519, Sec 4.1.6: Issue time (NumericDate).
   * @param nbf (Not Before) RFC 7519, Sec 4.1.5: Time before which token is invalid (NumericDate).
   * @param jti (JWT ID) RFC 7519, Sec 4.1.7: Unique token identifier.
   * @param customClaims All private (non-registered) claims.
   */
  public static record JwtPayload(
      String sub,
      List<String> aud,
      Long exp,
      Long iat,
      @Nullable Long nbf,
      @Nullable String jti,
      Map<String, Object> customClaims) {

    /**
     * Returns a Builder to construct a {@link JwtPayload}.
     *
     * @param sub Subject (required).
     * @param exp Expiration time in seconds (required).
     * @return A new Builder.
     */
    public static Builder builder(String sub, Long exp) {
      return new Builder(sub, exp);
    }

    /** Builder for {@link JwtPayload}, handling validation and default values. */
    public static class Builder {
      private final String sub;
      private final Long exp;
      private Long iat;
      private @Nullable Long nbf;
      private @Nullable String jti;
      private final List<String> aud = new ArrayList<>();
      private final Map<String, Object> customClaims = new HashMap<>();

      public Builder(String sub, Long exp) {
        this.sub = sub;
        this.exp = exp;
        // Default 'iat' to now. Can be overridden.
        this.iat = Instant.now().getEpochSecond();
      }

      /** Sets the (Issued At). If not set, {@code Instant.now().getEpochSecond()} is used. */
      public Builder issuedAt(Long iat) {
        this.iat = iat;
        return this;
      }

      public Builder notBefore(@Nullable Long nbf) {
        this.nbf = nbf;
        return this;
      }

      /** Sets the (JWT ID). If not set, {@code UUID.randomUUID().toString()} is used. */
      public Builder jwtId(@Nullable String jti) {
        this.jti = jti;
        return this;
      }

      /** Adds a single audience value. */
      public Builder audience(String audience) {
        if (!audience.isBlank()) {
          this.aud.add(audience);
        }
        return this;
      }

      /** Adds a list of audience values. */
      public Builder audiences(List<String> audiences) {
        this.aud.addAll(audiences);
        return this;
      }

      /** Adds a private (custom) claim. */
      public Builder claim(String key, String value) {
        this.customClaims.put(key, value);
        return this;
      }

      /** Adds multiple private claims. */
      public Builder claims(Map<String, Object> claims) {
        this.customClaims.putAll(claims);
        return this;
      }

      /** Builds the final {@link JwtPayload} after validation. */
      public JwtPayload build() {
        if (sub == null || sub.isBlank()) {
          throw new IllegalStateException("Subject (sub) cannot be null or empty");
        }
        if (exp == null || exp <= 0) {
          throw new IllegalStateException("Expiration (exp) must be a positive number");
        }
        if (iat == null || iat <= 0) {
          throw new IllegalStateException("IssuedAt (iat) must be a positive number");
        }
        if (exp <= iat) {
          throw new IllegalStateException("Expiration (exp) must be greater than IssuedAt (iat)");
        }
        if (nbf != null && nbf > exp) {
          throw new IllegalStateException("NotBefore (nbf) cannot be after Expiration (exp)");
        }
        if (jti != null && jti.isBlank()) {
          throw new IllegalStateException("JWT ID (jti) cannot be blank (if provided)");
        }
        if (aud.stream().anyMatch(String::isBlank)) {
          throw new IllegalStateException("Audience (aud) entries cannot be blank");
        }

        final var finalJti = (jti != null) ? jti : UUID.randomUUID().toString();

        final var audienceCopy = Objects.requireNonNull(List.copyOf(aud));
        final var customClaimsCopy = Objects.requireNonNull(Map.copyOf(customClaims));

        return new JwtPayload(sub, audienceCopy, exp, iat, nbf, finalJti, customClaimsCopy);
      }
    }
  }
}
