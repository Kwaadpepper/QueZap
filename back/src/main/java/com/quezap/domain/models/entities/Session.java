package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.errors.sessions.AddQuestionError;
import com.quezap.domain.errors.sessions.ParticipateSessionError;
import com.quezap.domain.errors.sessions.RemoveQuestionError;
import com.quezap.domain.errors.sessions.StartSessionError;
import com.quezap.domain.models.valueobjects.QuestionSlide;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.Domain;

import org.eclipse.jdt.annotation.Nullable;

public class Session extends AggregateRoot {
  public static final int QUESTIONS_COUNT_MAX_SIZE = 60;

  private final SessionName name;
  private final SessionCode code;
  private final Set<QuestionSlide> questionSlides;
  private final Set<Participant> participants;
  private final UserId author;
  private @Nullable ZonedDateTime startedAt;
  private @Nullable ZonedDateTime endedAt;

  private static void validateCommonInvariants(
      Set<QuestionSlide> questionSlides,
      @Nullable ZonedDateTime startedAt,
      @Nullable ZonedDateTime endedAt) {
    Domain.checkDomain(() -> !questionSlides.isEmpty(), "Question slides cannot be empty");
    Domain.checkDomain(
        () -> questionSlides.size() <= QUESTIONS_COUNT_MAX_SIZE,
        "Question slides cannot exceed " + QUESTIONS_COUNT_MAX_SIZE);
    if (endedAt != null) {
      Domain.checkDomain(() -> startedAt != null, "Session cannot end before it starts");
    }
    if (startedAt != null && endedAt != null) {
      Domain.checkDomain(() -> startedAt.isBefore(endedAt), "Session cannot end before it starts");
    }
  }

  public Session(
      SessionName name,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      UserId author,
      @Nullable ZonedDateTime startedAt,
      @Nullable ZonedDateTime endedAt) {
    super();
    validateCommonInvariants(questionSlides, startedAt, endedAt);
    this.name = name;
    this.code = code;
    this.questionSlides = new HashSet<>(questionSlides);
    this.participants = new HashSet<>(participants);
    this.author = author;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  protected Session(
      UUID id,
      SessionName name,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      UserId author,
      @Nullable ZonedDateTime startedAt,
      @Nullable ZonedDateTime endedAt) {
    super(id);
    validateCommonInvariants(questionSlides, startedAt, endedAt);
    this.name = name;
    this.code = code;
    this.questionSlides = new HashSet<>(questionSlides);
    this.participants = new HashSet<>(participants);
    this.author = author;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  public static Session hydrate(
      UUID id,
      SessionName name,
      SessionCode code,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      UserId author,
      @Nullable ZonedDateTime startedAt,
      @Nullable ZonedDateTime endedAt) {
    return new Session(id, name, code, questionSlides, participants, author, startedAt, endedAt);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public SessionName getName() {
    return name;
  }

  public SessionCode getCode() {
    return code;
  }

  public Set<QuestionSlide> getQuestionSlides() {
    return Set.copyOf(questionSlides);
  }

  public void addQuestion(QuestionSlide question) {
    if (isRunning()) {
      throw new DomainConstraintException(AddQuestionError.SESSION_IS_RUNNING);
    }
    if (isEnded()) {
      throw new DomainConstraintException(AddQuestionError.SESSION_IS_ENDED);
    }
    if (questionSlides.size() >= QUESTIONS_COUNT_MAX_SIZE) {
      throw new DomainConstraintException(AddQuestionError.MAX_QUESTIONS_REACHED);
    }
    questionSlides.add(question);
  }

  public void removeQuestion(QuestionSlide question) {
    if (isRunning()) {
      throw new DomainConstraintException(RemoveQuestionError.SESSION_IS_RUNNING);
    }
    if (isEnded()) {
      throw new DomainConstraintException(RemoveQuestionError.SESSION_IS_ENDED);
    }
    questionSlides.remove(question);
  }

  public @Nullable ZonedDateTime getStartedAt() {
    return startedAt;
  }

  public Set<Participant> getParticipants() {
    return Set.copyOf(participants);
  }

  public UserId getAuthor() {
    return author;
  }

  public @Nullable ZonedDateTime getEndedAt() {
    return endedAt;
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isRunning() {
    return isStarted() && !isEnded();
  }

  public boolean isStarted() {
    final var now = ZonedDateTime.now(ZoneId.of("UTC"));
    return now.isAfter(startedAt) && now.isBefore(endedAt);
  }

  public boolean isEnded() {
    final var now = ZonedDateTime.now(ZoneId.of("UTC"));
    return endedAt != null && now.isAfter(endedAt);
  }

  public void startSession() {
    if (isStarted()) {
      throw new DomainConstraintException(StartSessionError.SESSION_ALREADY_STARTED);
    }
    if (isEnded()) {
      throw new DomainConstraintException(StartSessionError.SESSION_ENDED);
    }
    if (!hasEnoughParticipantsToStart()) {
      throw new DomainConstraintException(StartSessionError.NOT_ENOUGH_PARTICIPANTS);
    }
    if (!hasEnoughQuestionsToStart()) {
      throw new DomainConstraintException(StartSessionError.NOT_ENOUGH_QUESTIONS_TO_START);
    }
    startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    // TODO: Emmit event SessionStarted
  }

  public void endSession() {
    if (endedAt != null) {
      throw new IllegalDomainStateException("Session has already ended");
    }
    if (startedAt == null) {
      throw new IllegalDomainStateException("Session has not started yet");
    }
    endedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    // TODO: Emmit event SessionEnded
  }

  public void addParticipant(Participant participant) {
    if (participants.stream().anyMatch(p -> p.name().equals(participant.name()))) {
      throw new DomainConstraintException(ParticipateSessionError.NAME_ALREADY_TAKEN);
    }

    participants.add(participant);
  }

  public boolean hasEnoughParticipantsToStart() {
    return participants.size() >= 2;
  }

  public boolean hasEnoughQuestionsToStart() {
    return questionSlides.size() >= 1;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Session that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
