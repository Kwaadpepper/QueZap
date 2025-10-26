package com.quezap.domain.models.entities;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public class Question extends AggregateRoot<QuestionId> {
  private static final int ANSWERS_MAX_SIZE = 4;

  private final QuestionType type;
  private final String value;
  private final @Nullable Picture picture;
  private final ThemeId theme;
  private final Set<Answer> answers;
  private final ZonedDateTime updatedAt;

  private static void validateCommonInvariants(
      QuestionType type, String question, Set<Answer> answers) {
    Domain.checkDomain(() -> !question.isBlank(), "Label cannot be blank");
    Domain.checkDomain(
        () -> question.trim().length() >= 15, "Label cannot be less than 15 characters");
    Domain.checkDomain(() -> question.length() <= 255, "Label cannot exceed 255 characters");

    // * Answers validations
    Domain.checkDomain(
        () -> answers.size() <= ANSWERS_MAX_SIZE, "Answers cannot exceed " + ANSWERS_MAX_SIZE);
    Domain.checkDomain(
        () -> answers.stream().filter(Answer::isCorrect).count() >= 1,
        "There must be at least one correct answer");

    switch (answers.size()) {
      case 1:
        // IGNORE --- No specific check for single answer questions here
        break;
      case 2:
        Domain.checkDomain(
            () -> answers.stream().filter(Answer::isCorrect).count() == 1,
            "There must be exactly one correct answer");
        break;
      case 3, 4:
        Domain.checkDomain(
            () -> answers.stream().filter(Answer::isCorrect).count() >= 1,
            "There must be at least one correct answer");
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + answers.size());
    }

    switch (type) {
      case QuestionType.BOOLEAN ->
          Domain.checkDomain(
              () -> answers.size() == 1, "Affirmation questions must have exactly 1 answer");
      case QuestionType.BINARY ->
          Domain.checkDomain(
              () -> answers.size() == 2, "Binary questions must have exactly 2 answers");
      case QuestionType.QUIZZ ->
          Domain.checkDomain(
              () -> answers.size() >= 2 && answers.size() <= 4,
              "Quizz questions must have between 2 and 4 answers");
      default -> throw new IllegalStateException("Unexpected value: " + type.name());
    }
  }

  public Question(
      QuestionType type,
      String value,
      @Nullable Picture picture,
      ThemeId theme,
      Set<Answer> answers,
      ZonedDateTime updatedAt) {
    super();
    validateCommonInvariants(type, value, answers);
    this.type = type;
    this.value = value;
    this.picture = picture;
    this.theme = theme;
    this.answers = new HashSet<>(answers);
    this.updatedAt = updatedAt;
  }

  protected Question(
      UUID id,
      QuestionType type,
      String value,
      @Nullable Picture picture,
      ThemeId theme,
      Set<Answer> answers,
      ZonedDateTime updatedAt) {
    super(id);
    validateCommonInvariants(type, value, answers);
    this.type = type;
    this.value = value;
    this.picture = picture;
    this.theme = theme;
    this.answers = new HashSet<>(answers);
    this.updatedAt = updatedAt;
  }

  public static Question hydrate(
      UUID id,
      QuestionType type,
      String value,
      @Nullable Picture picture,
      ThemeId theme,
      Set<Answer> answers,
      ZonedDateTime updatedAt) {
    return new Question(id, type, value, picture, theme, answers, updatedAt);
  }

  @Override
  public QuestionId getId() {
    return new QuestionId(rawId);
  }

  public QuestionType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public @Nullable Picture getPicture() {
    return picture;
  }

  public ThemeId getTheme() {
    return theme;
  }

  public Set<Answer> getAnswers() {
    return Set.copyOf(answers);
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Question that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawId);
  }
}
