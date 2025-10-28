package com.quezap.application.api.v1.routes.questions;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.request.questions.NewAffirmationDto;
import com.quezap.application.api.v1.dto.response.questions.QuestionIdDto;
import com.quezap.application.api.v1.exceptions.ServerException;
import com.quezap.application.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@RestController
public class AddQuestionController {
  private final UseCaseExecutor executor;
  private final AddQuestion.Handler handler;
  private final PictureMetadataValidatorService validatorService; // Injecté

  AddQuestionController(
      UseCaseExecutor executor,
      AddQuestion.Handler handler,
      PictureMetadataValidatorService validatorService) {
    this.executor = executor;
    this.handler = handler;
    this.validatorService = validatorService;
  }

  @PostMapping("apiv1/questions/affirmation")
  public QuestionIdDto addAffirmation(NewAffirmationDto request) {
    final var question = request.question();
    final var isTrue = request.isTrue();
    final var picture = request.picture();
    final var themeId = request.themeId();

    try (final var pictureStream = picture.getInputStream()) {

      final String mimeType = validatorService.validateAndGetMimeType(picture);
      final PictureUploadData pictureData =
          buildPictureUploadData(pictureStream, mimeType, picture.getSize());

      final var input = new AddQuestion.Input.Affirmation(question, isTrue, pictureData, themeId);
      final var output = executor.execute(handler, input);

      return toDto(output);

    } catch (IOException e) {
      throw new ServerException("Failed to read uploaded file", e);
    }
  }

  private QuestionIdDto toDto(AddQuestion.Output output) {
    return new QuestionIdDto(output.id().value());
  }

  private PictureUploadData buildPictureUploadData(
      InputStream fileStream, String mimeType, long fileSize) {

    final var pictureType = PictureType.fromMimeType(mimeType);

    return new PictureUploadData(fileStream, fileSize, pictureType);
  }
}
