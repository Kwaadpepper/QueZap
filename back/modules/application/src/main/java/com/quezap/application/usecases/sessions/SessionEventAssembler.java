package com.quezap.application.usecases.sessions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.quezap.application.ports.sessions.SessionRealtimePublisher.GroupMetricsView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.OptionStatView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.ParticipantView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.QuestionRevealEvent;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.QuestionStartEvent;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.SafeOptionView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.SafeQuestionView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.SessionEndedEvent;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideOption;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.domain.services.TextNormalizer;

import org.jspecify.annotations.Nullable;

public final class SessionEventAssembler {

  private static final String PUBLIC_BUCKET_PREFIX = "/quezap-public-images/";

  private SessionEventAssembler() {}

  public static @Nullable String toPublicPictureUrl(@Nullable String pictureFilename) {
    if (pictureFilename == null || pictureFilename.isBlank()) {
      return null;
    }
    final var trimmed = pictureFilename.trim();
    if (trimmed.startsWith("http://")
        || trimmed.startsWith("https://")
        || trimmed.startsWith("data:")
        || trimmed.startsWith("/")) {
      return trimmed;
    }
    if (trimmed.startsWith("quezap-public-images/")) {
      return "/" + trimmed;
    }
    if (trimmed.startsWith("pictures/")) {
      return PUBLIC_BUCKET_PREFIX + trimmed;
    }
    return PUBLIC_BUCKET_PREFIX + "pictures/questions/" + trimmed;
  }

  public static long parseSessionCode(String code) {
    final var digitsOnly = code.replaceAll("[^0-9]", "");
    if (digitsOnly.isBlank()) {
      throw SessionDomainException.notFound("Invalid session code: " + code);
    }
    try {
      return Long.parseLong(digitsOnly);
    } catch (NumberFormatException _) {
      throw SessionDomainException.notFound("Invalid session code: " + code);
    }
  }

  public static void validateSlidesCompleteness(
      @Nullable String quezapTitle, List<SessionSlideSpec> slides) {
    if (slides.isEmpty()) {
      final var title = quezapTitle != null ? quezapTitle : "";
      throw SessionDomainException.badRequest(
          "Le Quezap \"" + title + "\" ne contient aucune question");
    }
    for (final var slide : slides) {
      if (slide.label() == null || slide.label().isBlank()) {
        throw SessionDomainException.badRequest(
            "La question #" + slide.slideOrder() + " n'a pas d'énoncé");
      }
      final var options = slide.options();
      if (options.isEmpty()) {
        throw SessionDomainException.badRequest(
            "La question #" + slide.slideOrder() + " ne comporte aucune proposition de réponse");
      }
      final var type = slide.type() != null ? slide.type().toUpperCase(Locale.ROOT) : "QUIZZ";
      if (!"BOOLEAN".equals(type) && !"AFFIRMATION".equals(type) && !"CASH".equals(type)) {
        if (options.size() < 2) {
          throw SessionDomainException.badRequest(
              "La question #"
                  + slide.slideOrder()
                  + " doit comporter au moins 2 propositions de réponse");
        }
      }
      if (!"BOOLEAN".equals(type) && !"AFFIRMATION".equals(type)) {
        if (options.stream().noneMatch(SessionSlideOption::isCorrect)) {
          throw SessionDomainException.badRequest(
              "La question #" + slide.slideOrder() + " doit comporter au moins une bonne réponse");
        }
      }
      for (final var option : options) {
        if (!option.hasContent()) {
          throw SessionDomainException.badRequest(
              "Une des propositions de la question #" + slide.slideOrder() + " est vide");
        }
      }
    }
  }

  public static List<ParticipantView> buildActiveParticipantViews(Session session) {
    return session.getSessionParticipants().stream()
        .filter(p -> !p.isKicked())
        .map(p -> new ParticipantView(p.name(), p.joinedAt()))
        .toList();
  }

  public static int countActiveAnswersForCurrentSlide(Session session) {
    final var currentSlide = session.getCurrentSlideIndex();
    final var activeNames = new HashSet<String>();
    for (final var p : session.getSessionParticipants()) {
      if (!p.isKicked()) {
        activeNames.add(p.name());
      }
    }
    var count = 0;
    for (final var a : session.getSessionAnswers()) {
      if (a.slideIndex() == currentSlide && activeNames.contains(a.participantName())) {
        count++;
      }
    }
    return count;
  }

  public static QuestionStartEvent buildQuestionStartEvent(
      Session session, int slideIndex, int answeredCount, Instant now) {
    final var slides = session.getSlideSpecs();
    final var slide = slides.get(Math.min(slideIndex - 1, slides.size() - 1));
    final var safeAnswers = new ArrayList<SafeOptionView>();
    if (slide.isBooleanQuestion()) {
      safeAnswers.add(new SafeOptionView(0, "Vrai", null));
      safeAnswers.add(new SafeOptionView(1, "Faux", null));
    } else if (!slide.isCashQuestion()) {
      var answerIndex = 0;
      for (final var opt : slide.options()) {
        safeAnswers.add(
            new SafeOptionView(
                answerIndex++, opt.label(), toPublicPictureUrl(opt.pictureFilename())));
      }
    }

    final var safeQuestion =
        new SafeQuestionView(
            slide.questionId(),
            slide.normalizedType(),
            slide.label() != null ? slide.label() : "",
            toPublicPictureUrl(slide.pictureFilename()),
            safeAnswers);

    final var timerSeconds = slide.effectiveTimerSeconds();
    final var slideStartedAt =
        session.getCurrentSlideStartedAt() != null
            ? session.getCurrentSlideStartedAt().position()
            : null;
    final var pausedAt =
        session.isPaused() && session.getPausedAt() != null
            ? session.getPausedAt().position()
            : null;
    final var effectiveNow = pausedAt != null ? pausedAt : now;
    final var elapsedSeconds =
        slideStartedAt != null
            ? (int) Math.max(0, Duration.between(slideStartedAt, effectiveNow).toSeconds())
            : 0;
    final var remainingSeconds = Math.max(0, timerSeconds - elapsedSeconds);
    final var deadline =
        slideStartedAt != null && !session.isPaused()
            ? slideStartedAt.plusSeconds(timerSeconds)
            : now.plusSeconds(remainingSeconds);

    return new QuestionStartEvent(
        slideIndex,
        slides.size(),
        timerSeconds,
        slide.effectivePoints(),
        safeQuestion,
        deadline,
        answeredCount,
        remainingSeconds);
  }

  public static @Nullable QuestionRevealEvent buildQuestionRevealEvent(
      Session session, int slideIndex) {
    final var slides = session.getSlideSpecs();
    final var slideIdx = slideIndex - 1;
    if (slideIdx < 0 || slideIdx >= slides.size()) {
      return null;
    }

    final var slide = slides.get(slideIdx);
    final var kickedNames = new HashSet<String>();
    for (final var p : session.getSessionParticipants()) {
      if (p.isKicked()) {
        kickedNames.add(p.name());
      }
    }

    final var slideAnswers = new ArrayList<SessionAnswerRecord>();
    for (final var a : session.getSessionAnswers()) {
      if (a.slideIndex() == slideIndex && !kickedNames.contains(a.participantName())) {
        slideAnswers.add(a);
      }
    }

    final var totalCount = Math.max(slideAnswers.size(), 1);
    final var correctIndices = new ArrayList<Integer>();
    final var stats = new ArrayList<OptionStatView>();
    List<String> acceptedAnswers = null;

    if (slide.isCashQuestion()) {
      acceptedAnswers =
          slide.options().stream()
              .filter(SessionSlideOption::isCorrect)
              .map(SessionSlideOption::label)
              .filter(label -> label != null && !label.isBlank())
              .toList();
      for (var i = 0; i < acceptedAnswers.size(); i++) {
        correctIndices.add(i);
        final var expectedLabel = acceptedAnswers.get(i);
        final var count =
            (int)
                slideAnswers.stream()
                    .filter(
                        sa ->
                            sa.textAnswer() != null
                                && TextNormalizer.matches(sa.textAnswer(), expectedLabel))
                    .count();
        final var percentage = slideAnswers.isEmpty() ? 0.0 : ((double) count / totalCount * 100.0);
        stats.add(new OptionStatView(i, count, percentage));
      }
    } else if (slide.isBooleanQuestion()) {
      final var statementIsTrue = !slide.options().isEmpty() && slide.options().get(0).isCorrect();
      correctIndices.add(statementIsTrue ? 0 : 1);
      for (var i = 0; i < 2; i++) {
        final var target = i;
        final var count =
            (int) slideAnswers.stream().filter(sa -> sa.answerIndex() == target).count();
        final var percentage = (double) count / totalCount * 100.0;
        stats.add(new OptionStatView(i, count, percentage));
      }
    } else {
      final var options = slide.options();
      for (var i = 0; i < options.size(); i++) {
        if (options.get(i).isCorrect()) {
          correctIndices.add(i);
        }
        final var target = i;
        final var count =
            (int) slideAnswers.stream().filter(sa -> sa.answerIndex() == target).count();
        final var percentage = (double) count / totalCount * 100.0;
        stats.add(new OptionStatView(i, count, percentage));
      }
    }

    return new QuestionRevealEvent(
        slideIndex,
        correctIndices,
        null,
        stats,
        session.isMobileCorrectionEnabled(),
        acceptedAnswers);
  }

  public static SessionEndedEvent buildSessionEndedEvent(Session session, Instant endedAt) {
    final var leaderboard = session.buildLeaderboardEntries(session.getCurrentSlideIndex());
    final var totalParticipants = leaderboard.size();
    final var averageScore =
        totalParticipants > 0
            ? (int) Math.round(leaderboard.stream().mapToInt(e -> e.score()).average().orElse(0.0))
            : 0;

    final var activeNames = new HashSet<String>();
    for (final var p : session.getSessionParticipants()) {
      if (!p.isKicked()) {
        activeNames.add(p.name());
      }
    }

    var totalAnswers = 0;
    var correctAnswers = 0;
    for (final var a : session.getSessionAnswers()) {
      if (activeNames.contains(a.participantName())) {
        totalAnswers++;
        if (a.isCorrect()) {
          correctAnswers++;
        }
      }
    }
    final var globalSuccessRate =
        totalAnswers > 0 ? Math.round((double) correctAnswers / totalAnswers * 1000.0) / 10.0 : 0.0;

    final var firstName = !leaderboard.isEmpty() ? leaderboard.get(0).participantName() : "";
    final var secondName = leaderboard.size() > 1 ? leaderboard.get(1).participantName() : null;
    final var thirdName = leaderboard.size() > 2 ? leaderboard.get(2).participantName() : null;
    final var top5 = leaderboard.subList(0, Math.min(5, leaderboard.size()));

    return new SessionEndedEvent(
        endedAt,
        leaderboard,
        firstName,
        secondName,
        thirdName,
        new GroupMetricsView(totalParticipants, averageScore, globalSuccessRate),
        top5);
  }
}
