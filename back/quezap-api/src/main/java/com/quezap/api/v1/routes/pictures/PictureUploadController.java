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
import com.quezap.api.v1.routes.quezaps.QuezapController;
import com.quezap.api.v1.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.infrastructure.adapter.services.StagedPictureService;

@RestController
@RequestMapping("apiv1/pictures")
public class PictureUploadController {

  private final QuestionPictureManager questionPictureManager;
  private final StagedPictureService stagedPictureService;
  private final PictureMetadataValidatorService validatorService;
  private final UserJpaRepository userJpaRepository;

  public PictureUploadController(
      QuestionPictureManager questionPictureManager,
      StagedPictureService stagedPictureService,
      PictureMetadataValidatorService validatorService,
      UserJpaRepository userJpaRepository) {
    this.questionPictureManager = questionPictureManager;
    this.stagedPictureService = stagedPictureService;
    this.validatorService = validatorService;
    this.userJpaRepository = userJpaRepository;
  }

  @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public PictureUploadResponseDto upload(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    final var currentUser = getCurrentUser();
    final var mimeType = validatorService.validateAndGetMimeType(file);
    final var pictureType = PictureType.fromMimeType(mimeType);

    try (final var inputStream = file.getInputStream()) {
      final var uploadData = new PictureUploadData(inputStream, file.getSize(), pictureType);
      final var stored = questionPictureManager.store(uploadData);
      final var pictureUrl = QuezapController.toPublicPictureUrl(stored.objectKey());
      if (pictureUrl == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve public picture URL");
      }

      final var staged =
          stagedPictureService.registerStagedPicture(
              stored.objectKey(), pictureUrl, currentUser.getId(), null);

      return new PictureUploadResponseDto(pictureUrl, stored.objectKey(), staged.getExpiresAt());
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process uploaded file", e);
    }
  }

  private UserJpaEntity getCurrentUser() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() != null) {
      final var principal = auth.getPrincipal().toString();
      try {
        final var userId = UUID.fromString(principal);
        final var userOpt = userJpaRepository.findById(userId);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      } catch (IllegalArgumentException _) {
        final var userOpt = userJpaRepository.findByName(principal);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
  }
}
