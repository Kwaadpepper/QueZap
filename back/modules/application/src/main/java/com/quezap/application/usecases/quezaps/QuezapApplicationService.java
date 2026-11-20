package com.quezap.application.usecases.quezaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.application.ports.quezaps.QuezapDataPort.AnswerDetails;
import com.quezap.application.ports.quezaps.QuezapDataPort.AnswerInput;
import com.quezap.application.ports.quezaps.QuezapDataPort.InlineQuestionInput;
import com.quezap.application.ports.quezaps.QuezapDataPort.QuestionDetails;
import com.quezap.application.ports.quezaps.QuezapDataPort.QuezapDetails;
import com.quezap.application.ports.quezaps.QuezapDataPort.QuezapSlideDetails;
import com.quezap.application.ports.quezaps.QuezapDataPort.QuezapSummary;
import com.quezap.application.ports.quezaps.QuezapDataPort.SlideInput;
import com.quezap.application.ports.quezaps.QuezapDataPort.UserSummary;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class QuezapApplicationService {

  private static final Logger logger = LoggerFactory.getLogger(QuezapApplicationService.class);

  private final QuezapDataPort quezapDataPort;
  private final @Nullable StagedPictureManager stagedPictureManager;
  private final @Nullable QuestionPictureManager questionPictureManager;

  public QuezapApplicationService(
      QuezapDataPort quezapDataPort,
      @Nullable StagedPictureManager stagedPictureManager,
      @Nullable QuestionPictureManager questionPictureManager) {
    this.quezapDataPort = quezapDataPort;
    this.stagedPictureManager = stagedPictureManager;
    this.questionPictureManager = questionPictureManager;
  }

  public UUID create(
      @Nullable String principal,
      String title,
      @Nullable String description,
      boolean isPublic,
      List<SlideInput> slides) {
    final var author = requireCurrentUser(principal);
    if (slides.isEmpty()) {
      throw new QuezapBadRequestException("Questions list cannot be empty");
    }

    final var inlinePictureUrls = new LinkedHashSet<String>();
    final var validatedSlides = validateAndNormalizeSlides(slides, inlinePictureUrls);

    final var saved =
        quezapDataPort.create(author.id(), title, description, isPublic, validatedSlides);
    claimNewlyAttachedPictures(inlinePictureUrls, Set.of(), author.id());
    return saved.id();
  }

  public UUID duplicate(@Nullable String principal, UUID id) {
    return cloneInternal(principal, id, false);
  }

  public UUID cloneQuezap(@Nullable String principal, UUID id) {
    return cloneInternal(principal, id, true);
  }

  private UUID cloneInternal(@Nullable String principal, UUID id, boolean keepPublicFlag) {
    final var original =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));
    final var currentUser = requireCurrentUser(principal);

    if (!original.author().id().equals(currentUser.id()) && !original.isPublic()) {
      final var action = keepPublicFlag ? "cloner" : "dupliquer";
      throw new QuezapForbiddenException(
          "Impossible de " + action + " un questionnaire privé d'un autre auteur");
    }

    return quezapDataPort.cloneQuezap(id, currentUser.id(), keepPublicFlag);
  }

  @Transactional(readOnly = true)
  public QuezapDetails getById(@Nullable String principal, UUID id) {
    final var quezap =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));

    final var isAuthor = principal != null && principal.equals(quezap.author().id().toString());
    if (!isAuthor && !quezap.isPublic()) {
      throw new QuezapForbiddenException("Access denied to private quezap");
    }

    final var mappedSlides = new ArrayList<QuezapSlideDetails>(quezap.slides().size());
    for (final var slide : quezap.slides()) {
      final var q = slide.question();
      final var mappedAnswers =
          q.answers().stream()
              .map(
                  a ->
                      new AnswerDetails(
                          a.id(),
                          a.label(),
                          isAuthor && a.isCorrect(),
                          toPublicUrl(a.pictureFilename())))
              .toList();
      final var mappedQuestion =
          new QuestionDetails(
              q.id(),
              q.type(),
              q.label(),
              toPublicUrl(q.pictureFilename()),
              q.theme(),
              mappedAnswers);
      mappedSlides.add(
          new QuezapSlideDetails(
              slide.questionOrder(), slide.timerSeconds(), slide.points(), mappedQuestion));
    }

    return new QuezapDetails(
        quezap.id(),
        quezap.title(),
        quezap.description(),
        quezap.isPublic(),
        quezap.author(),
        mappedSlides,
        quezap.createdAt(),
        quezap.updatedAt());
  }

  @Transactional(readOnly = true)
  public PageOf<QuezapSummary> list(
      @Nullable String principal,
      @Nullable String search,
      @Nullable UUID authorId,
      Pagination pagination) {
    final var currentUserId = parseUuidOrNull(principal);
    return quezapDataPort.list(search, authorId, currentUserId, pagination);
  }

  public void update(
      @Nullable String principal,
      UUID id,
      @Nullable String title,
      @Nullable String description,
      @Nullable Boolean isPublic,
      @Nullable List<SlideInput> slides) {
    final var existing =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));
    final var currentUser = requireQuezapAuthor(principal, existing);
    final var previousUrls = collectPictureUrls(existing);

    final var inlinePictureUrls = new LinkedHashSet<String>();
    final @Nullable List<SlideInput> validatedSlides =
        slides != null ? validateAndNormalizeSlides(slides, inlinePictureUrls) : null;

    final var saved = quezapDataPort.update(id, title, description, isPublic, validatedSlides);
    final var currentUrls = saved.pictureUrls();

    claimNewlyAttachedPictures(inlinePictureUrls, previousUrls, currentUser.id());

    final var removedUrls = previousUrls.stream().filter(u -> !currentUrls.contains(u)).toList();
    if (!removedUrls.isEmpty()) {
      quezapDataPort.flush();
      scheduleUnreferencedPictureDeletion(removedUrls);
    }
  }

  public void delete(@Nullable String principal, UUID id) {
    final var existing =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));
    requireQuezapAuthor(principal, existing);

    final var urlsToDelete = collectPictureUrls(existing);
    quezapDataPort.delete(id);

    if (!urlsToDelete.isEmpty()) {
      quezapDataPort.flush();
      scheduleUnreferencedPictureDeletion(urlsToDelete);
    }
  }

  public void addQuestion(@Nullable String principal, UUID id, SlideInput slide) {
    final var existing =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));
    final var currentUser = requireQuezapAuthor(principal, existing);
    final var previousUrls = collectPictureUrls(existing);

    final var order = existing.slides().size() + 1;
    final var inlinePictureUrls = new LinkedHashSet<String>();
    final var validatedSlide = validateAndNormalizeSlide(slide, order, inlinePictureUrls);

    quezapDataPort.addQuestion(id, validatedSlide);
    claimNewlyAttachedPictures(inlinePictureUrls, previousUrls, currentUser.id());
  }

  public void removeQuestion(@Nullable String principal, UUID id, UUID questionId) {
    final var existing =
        quezapDataPort
            .findById(id)
            .orElseThrow(() -> new QuezapNotFoundException("Quezap not found"));
    requireQuezapAuthor(principal, existing);

    final var removedUrls = quezapDataPort.removeQuestion(id, questionId);
    if (!removedUrls.isEmpty()) {
      quezapDataPort.flush();
      scheduleUnreferencedPictureDeletion(removedUrls);
    }
  }

  private List<SlideInput> validateAndNormalizeSlides(
      List<SlideInput> slides, Set<String> collectedInlinePictureUrls) {
    final var validated = new ArrayList<SlideInput>(slides.size());
    var order = 1;
    for (final var slide : slides) {
      validated.add(validateAndNormalizeSlide(slide, order++, collectedInlinePictureUrls));
    }
    return validated;
  }

  private SlideInput validateAndNormalizeSlide(
      SlideInput slide, int order, Set<String> collectedInlinePictureUrls) {
    if (slide.question() != null) {
      final var q = slide.question();
      final var answers = q.answers();
      if (answers == null || answers.isEmpty()) {
        throw new QuezapBadRequestException("Question #" + order + " has no answers");
      }

      final var resolvedQuestionPic = resolveValidPictureUrl(q.pictureFilename());
      if (resolvedQuestionPic != null) {
        collectedInlinePictureUrls.add(resolvedQuestionPic);
      }

      final var normalizedAnswers = new ArrayList<AnswerInput>(answers.size());
      for (final var ans : answers) {
        final var resolvedAnsPic = resolveValidPictureUrl(ans.pictureFilename());
        if (resolvedAnsPic != null) {
          collectedInlinePictureUrls.add(resolvedAnsPic);
        }
        normalizedAnswers.add(
            new AnswerInput(
                ans.id(), ans.label(), ans.isCorrect(), resolvedAnsPic, ans.answerOrder()));
      }

      final var normalizedQuestion =
          new InlineQuestionInput(
              q.id(),
              q.label(),
              q.type(),
              resolvedQuestionPic,
              q.themeId(),
              q.themeName(),
              normalizedAnswers);
      return new SlideInput(
          slide.questionId(), slide.timerSeconds(), slide.points(), normalizedQuestion);
    }

    if (slide.questionId() != null) {
      if (!quezapDataPort.existsQuestion(slide.questionId())) {
        throw new QuezapBadRequestException("Question not found: " + slide.questionId());
      }
      if (!quezapDataPort.questionHasAnswers(slide.questionId())) {
        throw new QuezapBadRequestException("Question #" + order + " has no answers");
      }
      return slide;
    }

    throw new QuezapBadRequestException(
        "Slide #" + order + " must provide a questionId or inline question");
  }

  private void claimNewlyAttachedPictures(
      Set<String> inlinePictureUrls, Set<String> previousUrls, UUID uploaderId) {
    if (stagedPictureManager == null || inlinePictureUrls.isEmpty()) {
      return;
    }
    final var newlyAttached =
        inlinePictureUrls.stream().filter(url -> !previousUrls.contains(url)).toList();
    if (!newlyAttached.isEmpty()) {
      stagedPictureManager.claimPictures(newlyAttached, uploaderId);
    }
  }

  private void scheduleUnreferencedPictureDeletion(Collection<String> candidateUrls) {
    if (questionPictureManager == null || candidateUrls.isEmpty()) {
      return;
    }

    final var keysToDelete = new LinkedHashSet<String>();
    for (final var url : candidateUrls) {
      final var objectKey = toObjectKey(url);
      if (objectKey == null) {
        continue;
      }
      final var referenceCandidates = buildReferenceCandidates(url, objectKey);
      if (!quezapDataPort.isPictureReferenced(referenceCandidates)) {
        keysToDelete.add(objectKey);
      }
    }

    if (keysToDelete.isEmpty()) {
      return;
    }

    final var manager = questionPictureManager;
    final Runnable deleteTask =
        () -> {
          for (final var key : keysToDelete) {
            try {
              manager.removeByObjectKey(key);
            } catch (Exception e) {
              logger.warn("Best-effort S3 picture cleanup failed for key {}", key, e);
            }
          }
        };

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              deleteTask.run();
            }
          });
    } else {
      deleteTask.run();
    }
  }

  private Set<String> buildReferenceCandidates(String rawUrl, String objectKey) {
    final var candidates = new HashSet<String>();
    final var trimmedUrl = rawUrl.trim();
    if (!trimmedUrl.isBlank()) {
      candidates.add(trimmedUrl);
    }
    candidates.add(objectKey);
    final var canonicalUrl = toPublicUrl(objectKey);
    if (canonicalUrl != null) {
      candidates.add(canonicalUrl);
    }
    return candidates;
  }

  private Set<String> collectPictureUrls(QuezapDetails quezap) {
    final var urls = new HashSet<String>();
    for (final var slide : quezap.slides()) {
      final var q = slide.question();
      if (q.pictureFilename() != null && !q.pictureFilename().isBlank()) {
        urls.add(q.pictureFilename().trim());
      }
      for (final var a : q.answers()) {
        if (a.pictureFilename() != null && !a.pictureFilename().isBlank()) {
          urls.add(a.pictureFilename().trim());
        }
      }
    }
    return urls;
  }

  private @Nullable String resolveValidPictureUrl(@Nullable String rawPicture) {
    return stagedPictureManager != null
        ? stagedPictureManager.resolveValidPublicPictureUrl(rawPicture)
        : StagedPictureManager.validateAndNormalizePublicUrl(rawPicture);
  }

  private @Nullable String toPublicUrl(@Nullable String pictureFilename) {
    return stagedPictureManager != null
        ? stagedPictureManager.toPublicUrl(pictureFilename)
        : StagedPictureManager.normalizePublicUrl(pictureFilename);
  }

  private @Nullable String toObjectKey(@Nullable String pictureUrl) {
    return stagedPictureManager != null
        ? stagedPictureManager.toObjectKey(pictureUrl)
        : StagedPictureManager.extractObjectKey(pictureUrl);
  }

  private UserSummary requireQuezapAuthor(@Nullable String principal, QuezapDetails quezap) {
    final var currentUser = requireCurrentUser(principal);
    if (!quezap.author().id().equals(currentUser.id())) {
      throw new QuezapForbiddenException(
          "Seul le créateur peut modifier ou supprimer ce questionnaire");
    }
    return currentUser;
  }

  private UserSummary requireCurrentUser(@Nullable String principal) {
    if (principal == null || principal.isBlank()) {
      throw new QuezapUnauthorizedException("No authenticated user available");
    }
    return quezapDataPort
        .findUserByPrincipal(principal)
        .orElseThrow(() -> new QuezapUnauthorizedException("No authenticated user available"));
  }

  private @Nullable UUID parseUuidOrNull(@Nullable String principal) {
    if (principal == null || principal.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(principal);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  public static class QuezapUnauthorizedException extends RuntimeException {
    public QuezapUnauthorizedException(String message) {
      super(message);
    }
  }

  public static class QuezapForbiddenException extends RuntimeException {
    public QuezapForbiddenException(String message) {
      super(message);
    }
  }

  public static class QuezapNotFoundException extends RuntimeException {
    public QuezapNotFoundException(String message) {
      super(message);
    }
  }

  public static class QuezapBadRequestException extends IllegalArgumentException {
    public QuezapBadRequestException(String message) {
      super(message);
    }
  }
}
