package com.quezap.api.v1.routes.pictures;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.response.pictures.PictureUploadResponseDto;
import com.quezap.api.v1.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager.StagedPictureQuotaExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("apiv1/pictures")
public class PictureUploadController {

  private static final Logger logger = LoggerFactory.getLogger(PictureUploadController.class);

  private final QuestionPictureManager questionPictureManager;
  private final StagedPictureManager stagedPictureManager;
  private final PictureMetadataValidatorService validatorService;
  private final UserRepository userRepository;

  public PictureUploadController(
      QuestionPictureManager questionPictureManager,
      StagedPictureManager stagedPictureManager,
      PictureMetadataValidatorService validatorService,
      UserRepository userRepository) {
    this.questionPictureManager = questionPictureManager;
    this.stagedPictureManager = stagedPictureManager;
    this.validatorService = validatorService;
    this.userRepository = userRepository;
  }

  @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public PictureUploadResponseDto upload(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    final var currentUserId = getCurrentUserId();
    if (stagedPictureManager.countActiveStagedPictures(currentUserId)
        >= StagedPictureManager.MAX_ACTIVE_STAGED_PICTURES_PER_USER) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Staged picture upload quota exceeded (max "
              + StagedPictureManager.MAX_ACTIVE_STAGED_PICTURES_PER_USER
              + " active pictures)");
    }

    final var mimeType = validatorService.validateAndGetMimeType(file);
    final var pictureType = PictureType.fromMimeType(mimeType);

    try (final var inputStream = file.getInputStream()) {
      final var uploadData = new PictureUploadData(inputStream, file.getSize(), pictureType);
      final var stored = questionPictureManager.store(uploadData);
      final var pictureUrl = stagedPictureManager.toPublicUrl(stored.objectKey());
      if (pictureUrl == null) {
        compensateS3Upload(stored.objectKey());
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve public picture URL");
      }

      try {
        final var staged =
            stagedPictureManager.registerStagedPicture(
                stored.objectKey(), pictureUrl, currentUserId, null);

        return new PictureUploadResponseDto(pictureUrl, stored.objectKey(), staged.expiresAt());
      } catch (StagedPictureQuotaExceededException e) {
        compensateS3Upload(stored.objectKey());
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), e);
      } catch (RuntimeException e) {
        compensateS3Upload(stored.objectKey());
        throw e;
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process uploaded file", e);
    }
  }

  private void compensateS3Upload(String objectKey) {
    try {
      questionPictureManager.removeByObjectKey(objectKey);
    } catch (Exception cleanupEx) {
      logger.warn("Failed to compensate S3 upload for objectKey {}", objectKey, cleanupEx);
    }
  }

  private UUID getCurrentUserId() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() != null) {
      final var principal = auth.getPrincipal().toString();
      try {
        final var userId = UUID.fromString(principal);
        final var userOpt = userRepository.find(new UserId(userId));
        if (userOpt.isPresent()) {
          return userOpt.get().getId().value();
        }
      } catch (IllegalArgumentException _) {
        final var userOpt = userRepository.findByName(principal);
        if (userOpt.isPresent()) {
          return userOpt.get().getId().value();
        }
      }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
  }
}
