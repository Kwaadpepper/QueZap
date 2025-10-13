package com.quezap.domain.models.entities;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.QuestionSlide;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.utils.Domain;

import org.eclipse.jdt.annotation.Nullable;

public class Session extends AggregateRoot {
  public static final int QUESTIONS_COUNT_MAX_SIZE = 60;

  private final String label;
  private final SessionCode code;
  private final Set<QuestionSlide> questionSlides;
  private final UserId author;
  private final ZonedDateTime startedAt;
  private final ZonedDateTime endedAt;

  private static void validateCommonInvariants(
      String label,
      Set<QuestionSlide> questionSlides,
      ZonedDateTime startedAt,
      ZonedDateTime endedAt) {
    Domain.checkDomain(() -> !label.isBlank(), "Label cannot be blank");
    Domain.checkDomain(() -> label.length() <= 120, "Label cannot exceed 120 characters");
    Domain.checkDomain(() -> !questionSlides.isEmpty(), "Question slides cannot be empty");
    Domain.checkDomain(
        () -> questionSlides.size() <= QUESTIONS_COUNT_MAX_SIZE,
        "Question slides cannot exceed " + QUESTIONS_COUNT_MAX_SIZE);
    Domain.checkDomain(() -> startedAt.isBefore(endedAt), "Session cannot end before it starts");
  }

  public Session(
      String label,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      UserId author,
      ZonedDateTime startedAt,
      ZonedDateTime endedAt) {
    super();
    validateCommonInvariants(label, questionSlides, startedAt, endedAt);
    this.label = label;
    this.code = code;
    this.questionSlides = questionSlides;
    this.author = author;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  protected Session(
      UUID id,
      String label,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      UserId author,
      ZonedDateTime startedAt,
      ZonedDateTime endedAt) {
    super(id);
    validateCommonInvariants(label, questionSlides, startedAt, endedAt);
    this.label = label;
    this.code = code;
    this.questionSlides = questionSlides;
    this.author = author;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  public static Session hydrate(
      UUID id,
      String label,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      UserId author,
      ZonedDateTime startedAt,
      ZonedDateTime endedAt) {
    return new Session(id, label, code, questionSlides, author, startedAt, endedAt);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public SessionCode getCode() {
    return code;
  }

  public Set<QuestionSlide> getQuestionSlides() {
    return questionSlides;
  }

  public ZonedDateTime getStartedAt() {
    return startedAt;
  }

  public UserId getAuthor() {
    return author;
  }

  public ZonedDateTime getEndedAt() {
    return endedAt;
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Credential that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
