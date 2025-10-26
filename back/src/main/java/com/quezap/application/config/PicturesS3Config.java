package com.quezap.application.config;

import java.util.Objects;

import org.springframework.context.annotation.Configuration;

@Configuration
public class PicturesS3Config {
  private final String host;
  private final String publicEndpoint;
  private final String bucketName;
  private final String accessKeyId;
  private final String secretAccessKey;

  public PicturesS3Config(
      String host,
      String publicEndpoint,
      String bucketName,
      String accessKeyId,
      String secretAccessKey) {
    this.host = host;
    this.publicEndpoint = publicEndpoint;
    this.bucketName = bucketName;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;

    Objects.requireNonNull(host, "Pictures S3 host must be provided");
    Objects.requireNonNull(publicEndpoint, "Pictures S3 public endpoint must be provided");
    Objects.requireNonNull(bucketName, "Pictures S3 bucket name must be provided");
    Objects.requireNonNull(accessKeyId, "Pictures S3 access key ID must be provided");
    Objects.requireNonNull(secretAccessKey, "Pictures S3 secret access key must be provided");

    if (host.isBlank()) {
      throw new IllegalArgumentException("Pictures S3 host must not be blank");
    }
    if (publicEndpoint.isBlank()) {
      throw new IllegalArgumentException("Pictures S3 public endpoint must not be blank");
    }
    if (bucketName.isBlank()) {
      throw new IllegalArgumentException("Pictures S3 bucket name must not be blank");
    }
    if (accessKeyId.isBlank()) {
      throw new IllegalArgumentException("Pictures S3 access key ID must not be blank");
    }
    if (secretAccessKey.isBlank()) {
      throw new IllegalArgumentException("Pictures S3 secret access key must not be blank");
    }
  }

  public String getHost() {
    return host;
  }

  public String getPublicEndpoint() {
    return publicEndpoint;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }
}
