package com.quezap.application.api.v1.routes.questions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.quezap.application.api.v1.dto.request.questions.NewAffirmationDto;
import com.quezap.application.api.v1.dto.request.questions.NewBinaryDto;
import com.quezap.application.api.v1.dto.response.questions.QuestionIdDto;
import com.quezap.application.api.v1.exceptions.ServerException;
import com.quezap.application.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import org.jspecify.annotations.Nullable;

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

    try (final var pictureStream = fromPicture(picture)) {

      final var pictureData = buildPictureUploadData(pictureStream, picture);
      final var input = new AddQuestion.Input.Affirmation(question, isTrue, pictureData, themeId);
      final var output = executor.execute(handler, input);

      return toDto(output);

    } catch (IOException e) {
      throw new ServerException("Failed to read uploaded file", e);
    }
  }

  @PostMapping("apiv1/questions/binary")
  public QuestionIdDto addBinary(NewBinaryDto request) {
    try {
      return executeBinaryQuestionCreation(request);
    } catch (IOException e) {
      throw new ServerException("Failed to read uploaded file", e);
    }
  }

  private QuestionIdDto executeBinaryQuestionCreation(NewBinaryDto request) throws IOException {
    final var picture = request.picture();
    final var answers = request.answers();

    try (final var questionPictureStream = fromPicture(picture)) {

      final var questionPictureData = buildPictureUploadData(questionPictureStream, picture);
      final var answerFiles =
          answers.stream().map(NewBinaryDto.AnswerDto::picture).collect(Collectors.toSet());

      return autoCloseMultipartFiles(
          answerFiles,
          answerStreams -> {
            final var answerDataSet = new HashSet<AddQuestion.Input.AnswerData>();
            final var answerList = new ArrayList<>(answers);

            for (int i = 0; i < answerList.size(); i++) {
              final var answerDto = answerList.get(i);
              final var answerIsTrue = answerDto.isTrue();
              final var answerText = answerDto.answerText();

              final var answerPictureStream = answerStreams.get(i);
              final var answerPicture = answerDto.picture();
              final var answerPictureData =
                  buildPictureUploadData(answerPictureStream, answerPicture);

              answerDataSet.add(
                  new AddQuestion.Input.AnswerData(answerText, answerPictureData, answerIsTrue));
            }

            final var input =
                new AddQuestion.Input.Binary(
                    request.question(), answerDataSet, questionPictureData, request.themeId());
            final var output = executor.execute(handler, input);

            return toDto(output);
          });
    }
  }

  private QuestionIdDto toDto(AddQuestion.Output output) {
    return new QuestionIdDto(output.id().value());
  }

  private @Nullable PictureUploadData buildPictureUploadData(
      @Nullable InputStream fileStream, @Nullable MultipartFile picture) {
    if (fileStream != null && picture != null) {
      final var mimeType = validatorService.validateAndGetMimeType(picture);
      final var fileSize = picture.getSize();
      final var pictureType = PictureType.fromMimeType(mimeType);

      return new PictureUploadData(fileStream, fileSize, pictureType);
    }

    if (fileStream == null && picture == null) {
      return null;
    }

    throw new IllegalArgumentException("Both fileStream and picture must be non-null or null");
  }

  private @Nullable InputStream fromPicture(@Nullable MultipartFile picture) throws IOException {
    if (picture != null) {
      return picture.getInputStream();
    }
    return null;
  }

  private <T> T autoCloseMultipartFiles(
      Set<@Nullable MultipartFile> files, Function<List<@Nullable InputStream>, T> consumer)
      throws IOException {
    final var streams = new ArrayList<@Nullable InputStream>();
    try {
      for (final var file : files) {
        streams.add(file != null ? file.getInputStream() : null);
      }

      return consumer.apply(streams);

    } finally {
      for (var stream : streams) {
        try {
          if (stream != null) {
            stream.close();
          }
        } catch (IOException _) {
          // Ignore to avoid masking the main exception
        }
      }
    }
  }
}
