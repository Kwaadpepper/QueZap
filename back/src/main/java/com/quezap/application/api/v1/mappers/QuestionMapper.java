package com.quezap.application.api.v1.mappers;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.quezap.application.api.v1.dto.internal.AnswerWithStream;
import com.quezap.application.api.v1.dto.response.questions.QuestionIdDto;
import com.quezap.application.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.usecases.questions.AddQuestion;

import org.jspecify.annotations.Nullable;

@Component
public class QuestionMapper {
  private final PictureMetadataValidatorService validatorService;

  public QuestionMapper(PictureMetadataValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  public @Nullable PictureUploadData toPictureUploadData(
      @Nullable InputStream stream, @Nullable MultipartFile picture) {

    if (stream == null && picture == null) {
      return null;
    }

    if (stream != null && picture != null) {
      final var mimeType = validatorService.validateAndGetMimeType(picture);
      final var pictureType = PictureType.fromMimeType(mimeType);

      return new PictureUploadData(stream, picture.getSize(), pictureType);
    }

    throw new IllegalStateException(
        "Inconsistent picture state: stream and picture must both be null or both be non-null");
  }

  public Set<AddQuestion.Input.AnswerData> toAnswerDataSet(
      List<AnswerWithStream> answersWithStream) {

    final var answerDataSet = new HashSet<AddQuestion.Input.AnswerData>();

    for (final var answerWithStream : answersWithStream) {
      final var dto = answerWithStream.dto();
      final var stream = answerWithStream.stream();

      final var pictureData = toPictureUploadData(stream, dto.picture());
      final var answerData =
          new AddQuestion.Input.AnswerData(dto.answerText(), pictureData, dto.isTrue());

      answerDataSet.add(answerData);
    }

    return answerDataSet;
  }

  public QuestionIdDto toDto(AddQuestion.Output output) {
    return new QuestionIdDto(output.id().value());
  }
}
