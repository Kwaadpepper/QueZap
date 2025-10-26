package com.quezap.infrastructure.adapter.services;

import org.springframework.stereotype.Component;

import com.quezap.application.config.PicturesS3Config;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.utils.UuidV7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class QuestionPictureManagerS3Impl implements QuestionPictureManager {
  private final Logger logger = LoggerFactory.getLogger(QuestionPictureManagerS3Impl.class);
  private static final String PICTURE_FOLDER = "pictures/questions/";
  private final String bucketName;
  private final S3Client s3Client;

  public QuestionPictureManagerS3Impl(PicturesS3Config config) {
    this.bucketName = config.getBucketName();
    this.s3Client = config.getS3Client();
  }

  @Override
  public Picture store(PictureUploadData uploadData) {
    try {
      final var contentLength = uploadData.contentLength();
      final var pictureType = uploadData.type();
      final var objectKey = generateUniqueKey(pictureType);

      final var putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(objectKey)
              .contentLength(contentLength)
              .contentType(pictureType.mimeType())
              .build();
      final var requestBody = RequestBody.fromInputStream(uploadData.inputStream(), contentLength);

      s3Client.putObject(putObjectRequest, requestBody);

      logger.debug("Stored question picture with key {}", objectKey);

      return new Picture(objectKey, pictureType);
    } catch (Exception e) {
      throw new QuestionPictureManagerException("Error storing picture in S3.", e);
    }
  }

  @Override
  public boolean exists(Picture picture) {
    try {
      final var objectKey = picture.objectKey();
      s3Client.headObject(builder -> builder.bucket(bucketName).key(objectKey).build());

      logger.debug("Picture with key {} exists in S3 bucket {}", objectKey, bucketName);

      return true;
    } catch (NoSuchKeyException _) {

      logger.debug(
          "Picture with key {} does not exist in S3 bucket {}", picture.objectKey(), bucketName);

      return false;
    } catch (Exception e) {
      throw new QuestionPictureManagerException("Error checking picture existence in S3.", e);
    }
  }

  @Override
  public Picture copy(Picture picture) {
    try {
      final var sourceKey = picture.objectKey();
      final var pictureType = picture.pictureType();
      final var destinationKey = generateUniqueKey(pictureType);

      if (!exists(picture)) {
        throw new QuestionPictureManagerException(
            "Cannot copy picture: source picture does not exist in S3.");
      }

      s3Client.copyObject(
          builder ->
              builder
                  .sourceBucket(bucketName)
                  .sourceKey(sourceKey)
                  .destinationBucket(bucketName)
                  .destinationKey(destinationKey));

      logger.debug(
          "Copied picture from key {} to new key {} in S3 bucket {}",
          sourceKey,
          destinationKey,
          bucketName);

      return new Picture(destinationKey, pictureType);
    } catch (Exception e) {
      throw new QuestionPictureManagerException("Error copying picture in S3.", e);
    }
  }

  @Override
  public void remove(Picture picture) {
    try {
      final var objectKey = picture.objectKey();
      s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey).build());

      logger.debug("Removed picture with key {} from S3 bucket {}", objectKey, bucketName);

    } catch (NoSuchKeyException _) {
      logger.debug(
          "Picture with key {} does not exist in S3 bucket {}; nothing to remove",
          picture.objectKey(),
          bucketName);
    }
  }

  private final String generateUniqueKey(PictureType pictureType) {
    final var extension = pictureType.extensions().get(0);
    return PICTURE_FOLDER + UuidV7.randomUuid().toString() + "." + extension;
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
