package com.quezap.application.ports.quezaps;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

/** Application persistence port for Quezap quizzes, questions, answers, and picture references. */
public interface QuezapDataPort {

  Optional<UserSummary> findUserByPrincipal(String principal);

  Optional<QuezapDetails> findById(UUID id);

  PageOf<QuezapSummary> list(
      @Nullable String search,
      @Nullable UUID authorId,
      @Nullable UUID currentUserId,
      Pagination pagination);

  QuezapSaved create(
      UUID authorId,
      String title,
      @Nullable String description,
      boolean isPublic,
      List<SlideInput> slides);

  QuezapSaved update(
      UUID quezapId,
      @Nullable String title,
      @Nullable String description,
      @Nullable Boolean isPublic,
      @Nullable List<SlideInput> slides);

  void delete(UUID quezapId);

  UUID cloneQuezap(UUID originalId, UUID authorId, boolean keepPublicFlag);

  QuezapSaved addQuestion(UUID quezapId, SlideInput slide);

  List<String> removeQuestion(UUID quezapId, UUID questionId);

  boolean existsQuestion(UUID questionId);

  boolean questionHasAnswers(UUID questionId);

  QuestionAccessStatus checkQuestionAccess(UUID questionId, UUID userId);

  long countQuezapsReferencingQuestion(UUID questionId);

  boolean isPictureReferenced(Collection<String> pictureCandidates);

  void flush();

  enum QuestionAccessStatus {
    OWNED,
    PUBLIC_CLONE_REQUIRED,
    FORBIDDEN
  }

  record UserSummary(UUID id, String name) {}

  record ThemeSummary(UUID id, String name) {}

  record AnswerDetails(
      UUID id, @Nullable String label, boolean isCorrect, @Nullable String pictureFilename) {}

  record QuestionDetails(
      UUID id,
      String type,
      String label,
      @Nullable String pictureFilename,
      ThemeSummary theme,
      List<AnswerDetails> answers) {}

  record QuezapSlideDetails(
      int questionOrder, int timerSeconds, int points, QuestionDetails question) {}

  record QuezapDetails(
      UUID id,
      String title,
      @Nullable String description,
      boolean isPublic,
      UserSummary author,
      List<QuezapSlideDetails> slides,
      Instant createdAt,
      Instant updatedAt) {}

  record QuezapSummary(
      UUID id,
      String title,
      @Nullable String description,
      boolean isPublic,
      UUID authorId,
      int questionCount,
      Instant createdAt) {}

  record QuezapSaved(UUID id, Set<String> pictureUrls) {}

  record AnswerInput(
      @Nullable UUID id,
      String label,
      boolean isCorrect,
      @Nullable String pictureFilename,
      @Nullable Integer answerOrder) {}

  record InlineQuestionInput(
      @Nullable UUID id,
      String label,
      @Nullable String type,
      @Nullable String pictureFilename,
      @Nullable UUID themeId,
      @Nullable String themeName,
      @Nullable List<AnswerInput> answers) {}

  record SlideInput(
      @Nullable UUID questionId,
      int timerSeconds,
      int points,
      @Nullable InlineQuestionInput question) {}
}
