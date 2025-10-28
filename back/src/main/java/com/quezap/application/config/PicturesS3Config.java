package com.quezap.application.config;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;

@Configuration
public class PicturesS3Config {
  private final String host;
  private final Integer port;
  private final String publicEndpoint;
  private final String bucketName;
  private final String accessKeyId;
  private final String secretAccessKey;

  public PicturesS3Config(
      @Value("${pictures-s3.host}") String host,
      @Value("${pictures-s3.port}") Integer port,
      @Value("${pictures-s3.public-endpoint}") String publicEndpoint,
      @Value("${pictures-s3.bucket-name}") String bucketName,
      @Value("${pictures-s3.access-key-id}") String accessKeyId,
      @Value("${pictures-s3.secret-access-key}") String secretAccessKey) {
    this.host = host;
    this.port = port;
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

  public String getPublicEndpoint() {
    return publicEndpoint;
  }

  public String getBucketName() {
    return bucketName;
  }

  public S3Client getS3Client() {
    final var endpoint = createEndpoint();
    final var credentialsProvider = createProvider();

    return S3Client.builder()
        .endpointProvider(
            (S3EndpointParams endpointParams) ->
                CompletableFuture.completedFuture(
                    Endpoint.builder()
                        .url(URI.create(endpoint + "/" + endpointParams.bucket()))
                        .build()))
        .credentialsProvider(credentialsProvider)
        .region(Region.EU_WEST_1)
        .build();
  }

  private URI createEndpoint() {
    final var endpoint = String.format("http://%s:%d", host, port);
    return URI.create(endpoint);
  }

  private StaticCredentialsProvider createProvider() {
    return StaticCredentialsProvider.create(createCredentials());
  }

  private AwsBasicCredentials createCredentials() {
    return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
  }
}
