package com.quezap.infrastructure.adapter.services;

import java.io.BufferedInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;

import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.services.config.PicturesS3Config;
import com.quezap.lib.utils.UuidV7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class QuestionPictureManagerS3Impl implements QuestionPictureManager {
  private static final String PICTURE_FOLDER = "pictures/questions/";
  private static final Logger logger = LoggerFactory.getLogger(QuestionPictureManagerS3Impl.class);

  private final String bucketName;
  private final S3Client s3Client;

  public QuestionPictureManagerS3Impl(PicturesS3Config config) {
    this.bucketName = config.getBucketName();
    this.s3Client = config.getS3Client();
  }

  @Override
  public Picture store(PictureUploadData uploadData) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");

      try (final var digestStream = new DigestInputStream(uploadData.inputStream(), digest);
          final var bufferedStream = new BufferedInputStream(digestStream)) {
        final long contentLength = uploadData.contentLength();
        final var pictureType = uploadData.type();
        final var objectKey = generateUniqueKey(pictureType);

        final var putObjectRequest =
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength(contentLength)
                .contentType(pictureType.mimeType())
                .build();

        final var requestBody = RequestBody.fromInputStream(bufferedStream, contentLength);
        s3Client.putObject(putObjectRequest, requestBody);

        final var hash = new Sha256Hash(digest.digest());
        return new Picture(objectKey, pictureType, hash);
      }
    } catch (Exception e) {
      logStoreException(e);
      throw new QuestionPictureManagerException("Failed to store picture to S3.", e);
    }
  }

  @Override
  public boolean exists(Picture picture) {
    try {
      final var objectKey = picture.objectKey();
      s3Client.headObject(builder -> builder.bucket(bucketName).key(objectKey).build());

      logger.debug("Picture with key {} exists in S3 bucket {}", objectKey, bucketName);

      return true;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        logger.debug(
            "Picture with key {} does not exist in S3 bucket {}", picture.objectKey(), bucketName);
        return false;
      }
      logExistsException(e);
      throw new QuestionPictureManagerException("Failed to check picture existence in S3.", e);
    } catch (Exception e) {
      logExistsException(e);
      throw new QuestionPictureManagerException("Failed to check picture existence in S3.", e);
    }
  }

  @Override
  public Picture copy(Picture picture) {
    try {
      final var sourceKey = picture.objectKey();
      final var pictureHash = picture.hash();
      final var pictureType = picture.type();
      final var destinationKey = generateUniqueKey(pictureType);

      if (!exists(picture)) {
        throw new QuestionPictureManagerException(
            "Cannot copy picture: source picture does not exist in S3.");
      }

      final var response =
          s3Client.copyObject(
              builder ->
                  builder
                      .sourceBucket(bucketName)
                      .sourceKey(sourceKey)
                      .destinationBucket(bucketName)
                      .destinationKey(destinationKey));

      if (!response.sdkHttpResponse().isSuccessful()) {
        throw new QuestionPictureManagerException(
            "Error copying picture in S3: no result returned.");
      }

      logger.debug(
          "Copied picture from key {} to new key {} in S3 bucket {}",
          sourceKey,
          destinationKey,
          bucketName);

      return new Picture(destinationKey, pictureType, pictureHash);
    } catch (Exception e) {
      logCopyException(e);
      throw new QuestionPictureManagerException("Failed to copy picture in S3.", e);
    }
  }

  @Override
  public void remove(Picture picture) {
    removeByObjectKey(picture.objectKey());
  }

  @Override
  public void removeByObjectKey(String objectKey) {
    try {
      s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey).build());

      logger.debug("Removed picture with key {} from S3 bucket {}", objectKey, bucketName);

    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        logger.debug(
            "Picture with key {} does not exist in S3 bucket {}; nothing to remove",
            objectKey,
            bucketName);
      } else {
        throw e;
      }
    }
  }

  private String generateUniqueKey(PictureType pictureType) {
    final var extension = pictureType.extensions().get(0);
    return PICTURE_FOLDER + UuidV7.randomUuid().toString() + "." + extension;
  }

  private void logStoreException(Exception e) {
    logExceptionWithContext("Failed to store picture", e);
  }

  private void logExistsException(Exception e) {
    logExceptionWithContext("Failed to check picture existence", e);
  }

  private void logCopyException(Exception e) {
    logExceptionWithContext("Failed to copy picture", e);
  }

  private void logExceptionWithContext(String context, Exception e) {
    switch (e) {
      case SdkClientException sdkEx -> logger.error("{}: SdkClientException", context, sdkEx);
      case AwsServiceException awsEx -> {
        logger.error("{}: AwsServiceException", context, awsEx);
        logger.error("Status code: {}", awsEx.statusCode());
        logger.error("AWS Error Code: {}", awsEx.awsErrorDetails().errorCode());
        logger.error("Error Message: {}", awsEx.awsErrorDetails().errorMessage());
        logger.error("Request ID: {}", awsEx.requestId());
      }
      default -> logger.error("{}: Unexpected exception", context, e);
    }
  }

  public static class QuestionPictureManagerException extends RuntimeException {
    public QuestionPictureManagerException(String message, Throwable cause) {
      super(message, cause);
    }

    public QuestionPictureManagerException(String message) {
      super(message);
    }
  }
}
