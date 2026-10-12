package com.quezap.domain.models.entities;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.quezap.domain.events.sessions.SessionEnded;
import com.quezap.domain.events.sessions.SessionPaused;
import com.quezap.domain.events.sessions.SessionResumed;
import com.quezap.domain.events.sessions.SessionStarted;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.questions.QuestionAnswer;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public class Session extends AggregateRoot<SessionId> {
  public static final int QUESTIONS_COUNT_MAX_SIZE = 60;
  public static final int DEFAULT_EXPLANATION_DURATION_SECONDS = 15;

  private final SessionName name;
  private final SessionNumber number;
  private final Integer currentSlideIndex;
  private final Set<QuestionSlide> questionSlides;
  private final Set<Participant> participants;
  private final Set<QuestionAnswer> answers;
  private final UserId author;
  private ProgressionMode progressionMode;
  private boolean isPaused;
  private int explanationDurationSeconds;
  private boolean mobileCorrectionEnabled;
  private @Nullable TimelinePoint startedAt;
  private @Nullable TimelinePoint endedAt;

  private static void validateCommonInvariants(
      Set<QuestionSlide> questionSlides,
      Integer currentSlideIndex,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint endedAt) {
    Domain.checkDomain(() -> !questionSlides.isEmpty(), "Question slides cannot be empty");
    Domain.checkDomain(
        () -> questionSlides.size() <= QUESTIONS_COUNT_MAX_SIZE,
        "Question slides cannot exceed " + QUESTIONS_COUNT_MAX_SIZE);
    Domain.checkDomain(
        () -> currentSlideIndex >= 0 && currentSlideIndex < questionSlides.size(),
        "Current slide index must be within the range of question slides");

    if (endedAt != null) {
      Domain.checkDomain(() -> startedAt != null, "Session cannot end before it starts");
    }
    if (startedAt != null && endedAt != null) {
      Domain.checkDomain(() -> startedAt.isBefore(endedAt), "Session cannot end before it starts");
    }
  }

  public Session(
      SessionName name,
      SessionNumber number,
      Integer currentSlideIndex,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      Set<QuestionAnswer> answers,
      UserId author) {
    this(
        name,
        number,
        currentSlideIndex,
        questionSlides,
        participants,
        answers,
        author,
        ProgressionMode.MANUAL,
        false,
        DEFAULT_EXPLANATION_DURATION_SECONDS,
        false);
  }

  public Session(
      SessionName name,
      SessionNumber number,
      Integer currentSlideIndex,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      Set<QuestionAnswer> answers,
      UserId author,
      ProgressionMode progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled) {
    super();
    validateCommonInvariants(questionSlides, currentSlideIndex, startedAt, endedAt);
    this.name = name;
    this.number = number;
    this.currentSlideIndex = currentSlideIndex;
    this.questionSlides = new HashSet<>(questionSlides);
    this.participants = new HashSet<>(participants);
    this.answers = new HashSet<>(answers);
    this.author = author;
    this.progressionMode = progressionMode;
    this.isPaused = isPaused;
    this.explanationDurationSeconds = explanationDurationSeconds;
    this.mobileCorrectionEnabled = mobileCorrectionEnabled;
    this.startedAt = null;
    this.endedAt = null;
  }

  @SuppressWarnings("java:S107")
  protected Session(
      SessionId id,
      SessionName name,
      SessionNumber number,
      Integer currentSlideIndex,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      Set<QuestionAnswer> answers,
      UserId author,
      ProgressionMode progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint endedAt) {
    super(id.value());
    validateCommonInvariants(questionSlides, currentSlideIndex, startedAt, endedAt);
    this.name = name;
    this.number = number;
    this.currentSlideIndex = currentSlideIndex;
    this.questionSlides = new HashSet<>(questionSlides);
    this.participants = new HashSet<>(participants);
    this.answers = new HashSet<>(answers);
    this.author = author;
    this.progressionMode = progressionMode;
    this.isPaused = isPaused;
    this.explanationDurationSeconds = explanationDurationSeconds;
    this.mobileCorrectionEnabled = mobileCorrectionEnabled;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  @SuppressWarnings("java:S107")
  public static Session hydrate(
      SessionId id,
      SessionName name,
      SessionNumber number,
      Integer currentSlideIndex,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      Set<QuestionAnswer> answers,
      UserId author,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint endedAt) {
    return hydrate(
        id,
        name,
        number,
        currentSlideIndex,
        questionSlides,
        participants,
        answers,
        author,
        ProgressionMode.MANUAL,
        false,
        DEFAULT_EXPLANATION_DURATION_SECONDS,
        false,
        startedAt,
        endedAt);
  }

  @SuppressWarnings("java:S107")
  public static Session hydrate(
      SessionId id,
      SessionName name,
      SessionNumber number,
      Integer currentSlideIndex,
      Set<QuestionSlide> questionSlides,
      Set<Participant> participants,
      Set<QuestionAnswer> answers,
      UserId author,
      ProgressionMode progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint endedAt) {
    return new Session(
        id,
        name,
        number,
        currentSlideIndex,
        questionSlides,
        participants,
        answers,
        author,
        progressionMode,
        isPaused,
        explanationDurationSeconds,
        mobileCorrectionEnabled,
        startedAt,
        endedAt);
  }

  @Override
  public SessionId getId() {
    return new SessionId(rawId);
  }

  public SessionName getName() {
    return name;
  }

  public SessionNumber getNumber() {
    return number;
  }

  public Integer getCurrentSlideIndex() {
    return currentSlideIndex;
  }

  public Set<QuestionSlide> getQuestionSlides() {
    return Objects.requireNonNull(Set.copyOf(questionSlides));
  }

  public Set<QuestionAnswer> getAnswers() {
    return Objects.requireNonNull(Set.copyOf(answers));
  }

  public void addQuestion(QuestionSlide question) {
    Domain.checkDomain(() -> !isRunning(), "Cannot add question to a running session");
    Domain.checkDomain(() -> !hasEnded(), "Cannot add question to an ended session");
    Domain.checkDomain(
        () -> questionSlides.size() < QUESTIONS_COUNT_MAX_SIZE,
        "Cannot exceed maximum number of questions");

    questionSlides.add(question);
  }

  public void removeQuestion(QuestionSlide question) {
    Domain.checkDomain(() -> !isRunning(), "Cannot remove question from a running session");
    Domain.checkDomain(() -> !hasEnded(), "Cannot remove question from an ended session");

    questionSlides.remove(question);
  }

  public @Nullable TimelinePoint getStartedAt() {
    return startedAt;
  }

  public Set<Participant> getParticipants() {
    return Objects.requireNonNull(Set.copyOf(participants));
  }

  public UserId getAuthor() {
    return author;
  }

  public @Nullable TimelinePoint getEndedAt() {
    return endedAt;
  }

  @Override
  public TimelinePoint getCreatedAt() {
    return createdAt;
  }

  public ProgressionMode getProgressionMode() {
    return progressionMode;
  }

  public void setProgressionMode(ProgressionMode progressionMode) {
    this.progressionMode = progressionMode;
  }

  public boolean isPaused() {
    return isPaused;
  }

  public int getExplanationDurationSeconds() {
    return explanationDurationSeconds;
  }

  public void setExplanationDurationSeconds(int explanationDurationSeconds) {
    Domain.checkDomain(
        () -> explanationDurationSeconds >= 5 && explanationDurationSeconds <= 120,
        "Explanation duration must be between 5 and 120 seconds");
    this.explanationDurationSeconds = explanationDurationSeconds;
  }

  public boolean isMobileCorrectionEnabled() {
    return mobileCorrectionEnabled;
  }

  public void setMobileCorrectionEnabled(boolean mobileCorrectionEnabled) {
    this.mobileCorrectionEnabled = mobileCorrectionEnabled;
  }

  public void pauseSession() {
    Domain.checkDomain(this::isRunning, "Cannot pause a session that is not running");
    Domain.checkDomain(() -> !isPaused, "Session is already paused");
    this.isPaused = true;
    registerEvent(new SessionPaused(getId()));
  }

  public void resumeSession() {
    Domain.checkDomain(this::isRunning, "Cannot resume a session that is not running");
    Domain.checkDomain(() -> isPaused, "Session is not paused");
    this.isPaused = false;
    registerEvent(new SessionResumed(getId()));
  }

  public boolean isRunning() {
    return hasBegun() && !hasEnded();
  }

  public boolean hasBegun() {
    final var now = TimelinePoint.now();
    return startedAt != null && now.isAfter(startedAt);
  }

  public boolean hasEnded() {
    final var now = TimelinePoint.now();
    return endedAt != null && now.isAfter(endedAt);
  }

  public void startSession() {
    Domain.checkDomain(() -> !hasBegun(), "Session has already started");
    Domain.checkDomain(() -> !hasEnded(), "Session has already been ended");
    Domain.checkDomain(
        this::hasEnoughParticipantsToStart, "Not enough participants to start the session");
    Domain.checkDomain(
        this::hasEnoughQuestionsToStart, "Not enough questions to start the session");

    startedAt = TimelinePoint.now();
    registerEvent(new SessionStarted(getId()));
  }

  public void endSession() {
    if (endedAt != null) {
      throw new IllegalDomainStateException("Session has already ended");
    }
    if (startedAt == null) {
      throw new IllegalDomainStateException("Session has not started yet");
    }
    endedAt = TimelinePoint.now();
    registerEvent(new SessionEnded(getId()));
  }

  public void addParticipant(Participant participant) {
    Domain.checkDomain(
        () -> participants.stream().noneMatch(p -> p.name().equals(participant.name())),
        "Participant name is already taken");

    participants.add(participant);
  }

  public boolean hasEnoughParticipantsToStart() {
    return participants.size() >= 2;
  }

  public boolean hasEnoughQuestionsToStart() {
    return !questionSlides.isEmpty();
  }

  public void addAnswer(ParticipantName participantName, Integer slideIndex, Integer answerIndex) {
    if (participants.stream().noneMatch(p -> p.name().equals(participantName))) {
      throw new IllegalArgumentException("Participant not found in session");
    }

    Domain.checkDomain(this::hasBegun, "Session has not started yet");
    Domain.checkDomain(() -> !hasEnded(), "Session has already ended");

    Domain.checkDomain(
        () -> slideIndex >= 0 && slideIndex < questionSlides.size(),
        "Slide index is out of bounds");
    // * Cannot validate answerIndex against possible answers here
    Domain.checkDomain(() -> answerIndex >= 0, "Answer index is invalid");
    Domain.checkDomain(
        () -> slideIndex >= currentSlideIndex,
        "Cannot answer a slide that has already been passed");

    final var answer = new QuestionAnswer(participantName, slideIndex, answerIndex);

    answers.add(answer);
  }

  public static int calculateScore(
      int basePoints, int timerSeconds, long elapsedMillis, boolean isCorrect) {
    if (!isCorrect) {
      return 0;
    }
    if (timerSeconds <= 0) {
      return basePoints;
    }
    final var elapsedSeconds = elapsedMillis / 1000.0;
    final var remainingSeconds = Math.max(0.0, timerSeconds - elapsedSeconds);
    final var timeRatio = remainingSeconds / timerSeconds;
    return (int) Math.round(basePoints * (0.5 + 0.5 * timeRatio));
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
    return Objects.hash(rawId);
  }
}
