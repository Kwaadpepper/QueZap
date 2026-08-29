package com.quezap.infrastructure.adapter.persistence.jpa.mappers;

import java.util.HashSet;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.domain.ports.directories.views.QuestionView;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;

@Component
public class QuestionJpaMapper {

  public QuestionJpaEntity toEntity(Question domain, ThemeJpaEntity themeEntity) {
    final var entity =
        new QuestionJpaEntity(
            domain.getId().value(),
            domain.getType().name(),
            domain.getValue(),
            domain.getPicture() != null ? domain.getPicture().objectKey() : null,
            themeEntity,
            domain.getCreatedAt().position(),
            domain.getUpdatedAt().position());

    var order = 0;
    for (final var answer : domain.getAnswers()) {
      final var answerEntity =
          new AnswerJpaEntity(
              UuidV7.randomUuid(),
              entity,
              answer.value(),
              answer.picture() != null ? answer.picture().objectKey() : null,
              answer.isCorrect(),
              order++);
      entity.addAnswer(answerEntity);
    }

    return entity;
  }

  public Question toDomain(QuestionJpaEntity entity) {
    final var domainAnswers = new HashSet<Answer>();
    for (final var a : entity.getAnswers()) {
      final var pic = toPicture(a.getPictureFilename());
      domainAnswers.add(new Answer(a.getLabel(), pic, a.getIsCorrect()));
    }

    final var questionPic = toPicture(entity.getPictureFilename());

    return Question.hydrate(
        new QuestionId(entity.getId()),
        QuestionType.valueOf(entity.getType()),
        entity.getLabel(),
        questionPic,
        new ThemeId(entity.getTheme().getId()),
        domainAnswers,
        TimelinePoint.at(entity.getUpdatedAt()));
  }

  public QuestionView toView(QuestionJpaEntity entity) {
    return new QuestionView(
        new QuestionId(entity.getId()),
        entity.getLabel(),
        new ThemeId(entity.getTheme().getId()),
        TimelinePoint.at(entity.getCreatedAt()));
  }

  private @Nullable Picture toPicture(@Nullable String pictureFilename) {
    if (pictureFilename == null || pictureFilename.isBlank()) {
      return null;
    }
    final var lastDot = pictureFilename.lastIndexOf('.');
    final var extension =
        lastDot != -1 ? pictureFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT) : "jpg";
    final var pictureType =
        switch (extension) {
          case "png" -> PictureType.PNG;
          case "webp" -> PictureType.WEBP;
          default -> PictureType.JPG;
        };

    // Construct valid picture key if needed
    final var validKey =
        pictureFilename.contains("/") ? pictureFilename : "pictures/questions/" + pictureFilename;

    try {
      return new Picture(validKey, pictureType, new Sha256Hash(new byte[32]));
    } catch (Exception _) {
      return null;
    }
  }
}
