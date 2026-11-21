package com.quezap.api.v1.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TurnstileService {
  private static final Logger logger = LoggerFactory.getLogger(TurnstileService.class);

  private final String secretKey;
  private final String verifyUrl;
  private final RestClient restClient;

  @Autowired
  public TurnstileService(
      @Value("${app.turnstile.secret-key:}") String secretKey,
      @Value(
              "${app.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
          String verifyUrl) {
    this(secretKey, verifyUrl, RestClient.builder().build());
  }

  public TurnstileService(String secretKey, String verifyUrl, RestClient restClient) {
    this.secretKey = secretKey;
    this.verifyUrl = verifyUrl;
    this.restClient = restClient;
  }

  public boolean isEnabled() {
    return !secretKey.isBlank();
  }

  public void verifyOrThrow(@Nullable String token, @Nullable String remoteIp) {
    if (!isEnabled()) {
      return;
    }

    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Turnstile verification is required");
    }

    final var formData = new LinkedMultiValueMap<String, String>();
    formData.add("secret", secretKey);
    formData.add("response", token.trim());
    if (remoteIp != null && !remoteIp.isBlank()) {
      formData.add("remoteip", remoteIp.trim());
    }

    try {
      final var verifyResponse =
          restClient
              .post()
              .uri(verifyUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formData)
              .retrieve()
              .body(TurnstileVerifyResponse.class);

      if (verifyResponse == null || !verifyResponse.success()) {
        logger.warn(
            "Cloudflare Turnstile verification rejected: errorCodes={}",
            verifyResponse != null ? verifyResponse.errorCodes() : "null-response");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turnstile verification failed");
      }
    } catch (RestClientException e) {
      logger.error("Failed to reach Cloudflare Turnstile verification endpoint", e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify Turnstile challenge", e);
    }
  }

  public record TurnstileVerifyResponse(
      boolean success, @JsonProperty("error-codes") @Nullable List<String> errorCodes) {}
}
