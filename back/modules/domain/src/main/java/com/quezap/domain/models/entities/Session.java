package com.quezap.domain.models.entities;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import com.quezap.domain.models.valueobjects.sessions.SessionAdvanceResult;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerEvaluation;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.domain.services.TextNormalizer;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public class Session extends AggregateRoot<SessionId> {
  public static final int QUESTIONS_COUNT_MAX_SIZE = 60;
  public static final int DEFAULT_EXPLANATION_DURATION_SECONDS = 15;
  public static final int ANSWER_GRACE_SECONDS = 2;
  public static final int MAX_TEXT_ANSWER_LENGTH = 255;

  private final @Nullable SessionName name;
  private final String rawName;
  private final SessionNumber number;
  private Integer currentSlideIndex;
  private final Set<QuestionSlide> questionSlides;
  private final Set<Participant> participants;
  private final Set<QuestionAnswer> answers;
  private final UserId author;
  private @Nullable UUID quezapId;
  private @Nullable String quezapTitle;
  private ProgressionMode progressionMode;
  private boolean isPaused;
  private int explanationDurationSeconds;
  private boolean mobileCorrectionEnabled;
  private @Nullable TimelinePoint startedAt;
  private @Nullable TimelinePoint currentSlideStartedAt;
  private @Nullable TimelinePoint pausedAt;
  private @Nullable TimelinePoint endedAt;
  private @Nullable Integer lastRevealedSlideIndex;
  private final @Nullable TimelinePoint liveCreatedAt;
  private final List<SessionSlideSpec> slideSpecs;
  private final List<SessionParticipant> sessionParticipants;
  private final List<SessionAnswerRecord> sessionAnswers;

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
        () -> currentSlideIndex >= 0 && currentSlideIndex <= questionSlides.size(),
        "Current slide index must be within the range of question slides");

    if (endedAt != null) {
      Domain.checkDomain(() -> startedAt != null, "Session cannot end before it starts");
    }
    if (startedAt != null && endedAt != null) {
      Domain.checkDomain(() -> !endedAt.isBefore(startedAt), "Session cannot end before it starts");
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
    this.rawName = name.value();
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
    this.currentSlideStartedAt = null;
    this.pausedAt = null;
    this.endedAt = null;
    this.lastRevealedSlideIndex = 0;
    this.liveCreatedAt = null;
    this.slideSpecs = new ArrayList<>();
    this.sessionParticipants = new ArrayList<>();
    this.sessionAnswers = new ArrayList<>();
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
    this.rawName = name.value();
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
    this.currentSlideStartedAt = null;
    this.pausedAt = null;
    this.endedAt = endedAt;
    this.lastRevealedSlideIndex = 0;
    this.liveCreatedAt = null;
    this.slideSpecs = new ArrayList<>();
    this.sessionParticipants = new ArrayList<>();
    this.sessionAnswers = new ArrayList<>();
  }

  @SuppressWarnings("java:S107")
  private Session(
      SessionId id,
      String rawName,
      SessionNumber number,
      int currentSlideIndex,
      UserId author,
      @Nullable UUID quezapId,
      @Nullable String quezapTitle,
      ProgressionMode progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint currentSlideStartedAt,
      @Nullable TimelinePoint pausedAt,
      @Nullable TimelinePoint endedAt,
      @Nullable Integer lastRevealedSlideIndex,
      TimelinePoint createdAt,
      List<SessionSlideSpec> slideSpecs,
      List<SessionParticipant> sessionParticipants,
      List<SessionAnswerRecord> sessionAnswers) {
    super(id.value());
    this.rawName = rawName;
    this.name = rawName.trim().length() >= 6 ? new SessionName(rawName) : null;
    this.number = number;
    this.currentSlideIndex = Math.max(0, currentSlideIndex);
    this.questionSlides = new HashSet<>();
    this.participants = new HashSet<>();
    this.answers = new HashSet<>();
    this.author = author;
    this.quezapId = quezapId;
    this.quezapTitle = quezapTitle;
    this.progressionMode = progressionMode;
    this.isPaused = isPaused;
    this.explanationDurationSeconds = explanationDurationSeconds;
    this.mobileCorrectionEnabled = mobileCorrectionEnabled;
    this.startedAt = startedAt;
    this.currentSlideStartedAt = currentSlideStartedAt;
    this.pausedAt = pausedAt;
    this.endedAt = endedAt;
    this.lastRevealedSlideIndex = lastRevealedSlideIndex;
    this.liveCreatedAt = createdAt;
    this.slideSpecs = new ArrayList<>(slideSpecs);
    this.sessionParticipants = new ArrayList<>(sessionParticipants);
    this.sessionAnswers = new ArrayList<>(sessionAnswers);
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

  @SuppressWarnings("java:S107")
  public static Session createLive(
      SessionId id,
      String rawName,
      SessionNumber number,
      UserId author,
      UUID quezapId,
      @Nullable String quezapTitle,
      TimelinePoint createdAt,
      List<SessionSlideSpec> slideSpecs) {
    if (rawName.isBlank() || rawName.length() > 100) {
      throw SessionDomainException.badRequest("Session name must be between 1 and 100 characters");
    }
    if (slideSpecs.isEmpty()) {
      throw SessionDomainException.badRequest("Session must contain at least one question");
    }
    return new Session(
        id,
        rawName.trim(),
        number,
        0,
        author,
        quezapId,
        quezapTitle,
        ProgressionMode.MANUAL,
        false,
        DEFAULT_EXPLANATION_DURATION_SECONDS,
        false,
        null,
        null,
        null,
        null,
        0,
        createdAt,
        slideSpecs,
        List.of(),
        List.of());
  }

  @SuppressWarnings("java:S107")
  public static Session hydrateLive(
      SessionId id,
      String rawName,
      SessionNumber number,
      int currentSlideIndex,
      UserId author,
      @Nullable UUID quezapId,
      @Nullable String quezapTitle,
      ProgressionMode progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled,
      @Nullable TimelinePoint startedAt,
      @Nullable TimelinePoint currentSlideStartedAt,
      @Nullable TimelinePoint pausedAt,
      @Nullable TimelinePoint endedAt,
      @Nullable Integer lastRevealedSlideIndex,
      TimelinePoint createdAt,
      List<SessionSlideSpec> slideSpecs,
      List<SessionParticipant> sessionParticipants,
      List<SessionAnswerRecord> sessionAnswers) {
    return new Session(
        id,
        rawName,
        number,
        currentSlideIndex,
        author,
        quezapId,
        quezapTitle,
        progressionMode,
        isPaused,
        explanationDurationSeconds,
        mobileCorrectionEnabled,
        startedAt,
        currentSlideStartedAt,
        pausedAt,
        endedAt,
        lastRevealedSlideIndex,
        createdAt,
        slideSpecs,
        sessionParticipants,
        sessionAnswers);
  }

  @Override
  public SessionId getId() {
    return new SessionId(rawId);
  }

  public SessionName getName() {
    if (name != null) {
      return name;
    }
    final var trimmed = rawName.trim();
    if (trimmed.length() >= 6) {
      return new SessionName(trimmed);
    }
    return new SessionName(String.format("%-6s", trimmed).replace(' ', '_'));
  }

  public String getRawName() {
    return rawName;
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

  public @Nullable TimelinePoint getCurrentSlideStartedAt() {
    return currentSlideStartedAt;
  }

  public @Nullable TimelinePoint getPausedAt() {
    return pausedAt;
  }

  public @Nullable Integer getLastRevealedSlideIndex() {
    return lastRevealedSlideIndex;
  }

  public @Nullable UUID getQuezapId() {
    return quezapId;
  }

  public @Nullable String getQuezapTitle() {
    return quezapTitle;
  }

  public List<SessionSlideSpec> getSlideSpecs() {
    return Objects.requireNonNull(List.copyOf(slideSpecs));
  }

  public List<SessionParticipant> getSessionParticipants() {
    return Objects.requireNonNull(List.copyOf(sessionParticipants));
  }

  public List<SessionAnswerRecord> getSessionAnswers() {
    return Objects.requireNonNull(List.copyOf(sessionAnswers));
  }

  public int getTotalSlides() {
    return !slideSpecs.isEmpty() ? slideSpecs.size() : questionSlides.size();
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
    return liveCreatedAt != null ? liveCreatedAt : createdAt;
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

  public String getStatus() {
    if (endedAt != null) {
      return "ENDED";
    }
    if (startedAt != null) {
      return "STARTED";
    }
    return "LOBBY";
  }

  public void assertHost(UUID requesterId) {
    if (!author.value().equals(requesterId)) {
      throw SessionDomainException.forbidden("Seul le créateur peut piloter cette session");
    }
  }

  public void start(TimelinePoint now) {
    if (startedAt != null) {
      throw SessionDomainException.badRequest("Session has already started");
    }
    if (endedAt != null) {
      throw SessionDomainException.badRequest("Session has already ended");
    }
    if (slideSpecs.isEmpty()) {
      throw SessionDomainException.badRequest("Session does not contain questions");
    }
    this.startedAt = now;
    this.currentSlideIndex = 1;
    this.currentSlideStartedAt = now;
    this.isPaused = false;
    this.pausedAt = null;
    registerEvent(new SessionStarted(getId()));
  }

  public SessionAdvanceResult advanceToNextSlide(TimelinePoint now) {
    if (endedAt != null) {
      throw SessionDomainException.badRequest("Session has already ended");
    }
    if (slideSpecs.isEmpty()) {
      throw SessionDomainException.badRequest("Session does not contain questions");
    }
    final var totalSlides = slideSpecs.size();
    final var wasPaused = isPaused;
    final var nextIndex = currentSlideIndex + 1;

    if (nextIndex > totalSlides) {
      this.isPaused = false;
      this.pausedAt = null;
      this.endedAt = now;
      registerEvent(new SessionEnded(getId()));
      return new SessionAdvanceResult(true, wasPaused, currentSlideIndex, totalSlides);
    }

    if (this.startedAt == null) {
      this.startedAt = now;
    }
    this.isPaused = false;
    this.pausedAt = null;
    this.currentSlideIndex = nextIndex;
    this.currentSlideStartedAt = now;
    return new SessionAdvanceResult(false, wasPaused, nextIndex, totalSlides);
  }

  public void setPaused(boolean paused, TimelinePoint now) {
    if (endedAt != null) {
      throw SessionDomainException.badRequest("Session has already ended");
    }
    if (paused && !this.isPaused) {
      this.isPaused = true;
      this.pausedAt = now;
      registerEvent(new SessionPaused(getId()));
    } else if (!paused && this.isPaused) {
      if (this.pausedAt != null && this.currentSlideStartedAt != null) {
        final var pausedDuration = Duration.between(this.pausedAt.position(), now.position());
        if (!pausedDuration.isNegative()) {
          this.currentSlideStartedAt =
              TimelinePoint.at(this.currentSlideStartedAt.position().plus(pausedDuration));
        }
      }
      this.isPaused = false;
      this.pausedAt = null;
      registerEvent(new SessionResumed(getId()));
    }
  }

  public int revealCurrentSlide() {
    if (startedAt == null || currentSlideIndex <= 0 || endedAt != null) {
      throw SessionDomainException.badRequest("Session is not currently active");
    }
    this.lastRevealedSlideIndex = currentSlideIndex;
    return currentSlideIndex;
  }

  public void updateSettings(
      @Nullable String progressionModeRaw,
      @Nullable Integer newExplanationDuration,
      @Nullable Boolean newMobileCorrection) {
    if (progressionModeRaw != null) {
      try {
        this.progressionMode =
            ProgressionMode.valueOf(progressionModeRaw.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        throw SessionDomainException.badRequest("Invalid progression mode: " + progressionModeRaw);
      }
    }
    if (newExplanationDuration != null) {
      if (newExplanationDuration < 5 || newExplanationDuration > 120) {
        throw SessionDomainException.badRequest(
            "Explanation duration must be between 5 and 120 seconds");
      }
      this.explanationDurationSeconds = newExplanationDuration;
    }
    if (newMobileCorrection != null) {
      this.mobileCorrectionEnabled = newMobileCorrection;
    }
  }

  public void addLiveParticipant(SessionParticipant participant) {
    sessionParticipants.add(participant);
  }

  public SessionParticipant kickParticipant(String targetName) {
    for (var i = 0; i < sessionParticipants.size(); i++) {
      final var p = sessionParticipants.get(i);
      if (p.name().equalsIgnoreCase(targetName)) {
        final var kicked = p.withKicked(true);
        sessionParticipants.set(i, kicked);
        return kicked;
      }
    }
    throw SessionDomainException.notFound("Participant not found");
  }

  public Optional<SessionParticipant> leaveParticipant(@Nullable String participationToken) {
    if (participationToken == null || participationToken.isBlank()) {
      return Optional.empty();
    }
    for (var i = 0; i < sessionParticipants.size(); i++) {
      final var p = sessionParticipants.get(i);
      if (participationToken.equals(p.participationToken()) && !p.isKicked()) {
        sessionParticipants.remove(i);
        sessionAnswers.removeIf(a -> a.participantName().equalsIgnoreCase(p.name()));
        return Optional.of(p);
      }
    }
    return Optional.empty();
  }

  public SessionAnswerEvaluation evaluateAnswer(
      @Nullable Integer requestedSlideIndex,
      @Nullable Integer answerIndex,
      @Nullable String textAnswer,
      Instant now) {
    if (startedAt == null || endedAt != null) {
      throw SessionDomainException.badRequest("Session is not currently active");
    }
    if (isPaused) {
      throw SessionDomainException.conflict("La session est actuellement en pause");
    }
    if (textAnswer != null && textAnswer.length() > MAX_TEXT_ANSWER_LENGTH) {
      throw SessionDomainException.badRequest(
          "La réponse texte ne doit pas dépasser 255 caractères");
    }
    if (requestedSlideIndex != null && !requestedSlideIndex.equals(currentSlideIndex)) {
      throw SessionDomainException.badRequest("Invalid slide index for current question");
    }

    final var effectiveSlideIndex =
        requestedSlideIndex != null ? requestedSlideIndex : Math.max(currentSlideIndex, 1);

    if (lastRevealedSlideIndex != null && lastRevealedSlideIndex >= effectiveSlideIndex) {
      throw SessionDomainException.conflict("Les réponses sont verrouillées pour cette question");
    }

    final var slideIdx = effectiveSlideIndex - 1;
    if (slideIdx < 0 || slideIdx >= slideSpecs.size()) {
      return new SessionAnswerEvaluation(false, 0, 0L);
    }

    final var slide = slideSpecs.get(slideIdx);
    final var timerSeconds = slide.effectiveTimerSeconds();
    final var slideStartedAt =
        currentSlideStartedAt != null ? currentSlideStartedAt.position() : startedAt.position();
    final var elapsedMillis = Math.max(0L, Duration.between(slideStartedAt, now).toMillis());
    final var elapsedSeconds = Duration.between(slideStartedAt, now).toSeconds();

    if (elapsedSeconds > timerSeconds + ANSWER_GRACE_SECONDS) {
      throw SessionDomainException.badRequest("Le temps imparti est écoulé");
    }

    var isCorrect = false;
    if (slide.isCashQuestion()) {
      if (textAnswer != null && !textAnswer.isBlank()) {
        isCorrect =
            slide.options().stream()
                .anyMatch(
                    opt ->
                        opt.isCorrect()
                            && opt.label() != null
                            && TextNormalizer.matches(textAnswer, opt.label()));
      }
    } else if (slide.isBooleanQuestion()) {
      final var ansIdx = answerIndex != null ? answerIndex : -1;
      if (ansIdx == 0 || ansIdx == 1) {
        final var statementIsTrue =
            !slide.options().isEmpty() && slide.options().get(0).isCorrect();
        isCorrect = (ansIdx == 0 && statementIsTrue) || (ansIdx == 1 && !statementIsTrue);
      }
    } else {
      final var ansIdx = answerIndex != null ? answerIndex : -1;
      if (ansIdx >= 0 && ansIdx < slide.options().size()) {
        isCorrect = slide.options().get(ansIdx).isCorrect();
      }
    }

    final var pointsAwarded =
        isCorrect ? calculateScore(slide.effectivePoints(), timerSeconds, elapsedMillis, true) : 0;
    return new SessionAnswerEvaluation(isCorrect, pointsAwarded, elapsedMillis);
  }

  public List<SessionParticipant> getRankedActiveParticipants() {
    return sessionParticipants.stream()
        .filter(p -> !p.isKicked())
        .sorted(SessionParticipant.RANKING_COMPARATOR)
        .toList();
  }

  public List<SessionLeaderboardEntry> buildLeaderboardEntries(int slideIndex) {
    final var sorted = getRankedActiveParticipants();
    final var indexedAnswers = indexAnswersByParticipantAndSlide(sessionAnswers);

    final var entries = new ArrayList<SessionLeaderboardEntry>(sorted.size());
    var rank = 1;
    for (final var p : sorted) {
      final var participantSlides = indexedAnswers.getOrDefault(p.name(), Map.of());
      final var currentSlideAnswer = slideIndex > 0 ? participantSlides.get(slideIndex) : null;
      final var delta = currentSlideAnswer != null ? currentSlideAnswer.pointsAwarded() : 0;

      var streak = 0;
      for (var s = slideIndex; s >= 1; s--) {
        final var ans = participantSlides.get(s);
        if (ans != null && ans.isCorrect()) {
          streak++;
        } else {
          break;
        }
      }

      var correctCount = 0;
      for (final var ans : participantSlides.values()) {
        if (ans.isCorrect()) {
          correctCount++;
        }
      }

      entries.add(
          new SessionLeaderboardEntry(rank++, p.name(), p.score(), delta, streak, correctCount));
    }
    return entries;
  }

  public static Map<String, Map<Integer, SessionAnswerRecord>> indexAnswersByParticipantAndSlide(
      List<SessionAnswerRecord> answerRecords) {
    final var result = new HashMap<String, Map<Integer, SessionAnswerRecord>>();
    for (final var answer : answerRecords) {
      result
          .computeIfAbsent(answer.participantName(), k -> new HashMap<>())
          .put(answer.slideIndex(), answer);
    }
    return result;
  }

  public static Map<Integer, List<SessionAnswerRecord>> indexActiveAnswersBySlide(
      List<SessionAnswerRecord> answerRecords, Set<String> kickedParticipantNames) {
    final var result = new HashMap<Integer, List<SessionAnswerRecord>>();
    for (final var answer : answerRecords) {
      if (!kickedParticipantNames.contains(answer.participantName())) {
        result.computeIfAbsent(answer.slideIndex(), k -> new ArrayList<>()).add(answer);
      }
    }
    return result;
  }

  public void pauseSession() {
    Domain.checkDomain(this::isRunning, "Cannot pause a session that is not running");
    Domain.checkDomain(() -> !isPaused, "Session is already paused");
    this.isPaused = true;
    this.pausedAt = TimelinePoint.now();
    registerEvent(new SessionPaused(getId()));
  }

  public void resumeSession() {
    Domain.checkDomain(this::isRunning, "Cannot resume a session that is not running");
    Domain.checkDomain(() -> isPaused, "Session is not paused");
    final var now = TimelinePoint.now();
    if (this.pausedAt != null && this.currentSlideStartedAt != null) {
      final var pausedDuration = Duration.between(this.pausedAt.position(), now.position());
      if (!pausedDuration.isNegative()) {
        this.currentSlideStartedAt =
            TimelinePoint.at(this.currentSlideStartedAt.position().plus(pausedDuration));
      }
    }
    this.isPaused = false;
    this.pausedAt = null;
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

    final var now = TimelinePoint.now();
    this.startedAt = now;
    this.currentSlideIndex = 1;
    this.currentSlideStartedAt = now;
    this.isPaused = false;
    this.pausedAt = null;
    registerEvent(new SessionStarted(getId()));
  }

  public void endSession() {
    if (endedAt != null) {
      throw new IllegalDomainStateException("Session has already ended");
    }
    if (startedAt == null) {
      throw new IllegalDomainStateException("Session has not started yet");
    }
    this.isPaused = false;
    this.pausedAt = null;
    this.endedAt = TimelinePoint.now();
    registerEvent(new SessionEnded(getId()));
  }

  public void addParticipant(Participant participant) {
    Domain.checkDomain(
        () -> participants.stream().noneMatch(p -> p.name().equals(participant.name())),
        "Participant name is already taken");

    participants.add(participant);
  }

  public boolean hasEnoughParticipantsToStart() {
    // Classroom and demo sessions can start with 0 or more participants.
    return true;
  }

  public boolean hasEnoughQuestionsToStart() {
    return !questionSlides.isEmpty() || !slideSpecs.isEmpty();
  }

  public void addAnswer(ParticipantName participantName, Integer slideIndex, Integer answerIndex) {
    if (participants.stream().noneMatch(p -> p.name().equals(participantName))) {
      throw new IllegalArgumentException("Participant not found in session");
    }

    Domain.checkDomain(this::hasBegun, "Session has not started yet");
    Domain.checkDomain(() -> !hasEnded(), "Session has already ended");
    Domain.checkDomain(() -> !isPaused, "Session is paused");

    Domain.checkDomain(
        () -> slideIndex >= 0 && slideIndex <= getTotalSlides(), "Slide index is out of bounds");
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

  public static Comparator<SessionParticipant> rankingComparator() {
    return SessionParticipant.RANKING_COMPARATOR;
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
