package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;

@Repository
public class QuezapDataJpaAdapter implements QuezapDataPort {

  private final QuezapJpaRepository quezapJpaRepository;
  private final QuestionJpaRepository questionJpaRepository;
  private final ThemeJpaRepository themeJpaRepository;
  private final UserJpaRepository userJpaRepository;

  public QuezapDataJpaAdapter(
      QuezapJpaRepository quezapJpaRepository,
      QuestionJpaRepository questionJpaRepository,
      ThemeJpaRepository themeJpaRepository,
      UserJpaRepository userJpaRepository) {
    this.quezapJpaRepository = quezapJpaRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.themeJpaRepository = themeJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public Optional<UserSummary> findUserByPrincipal(String principal) {
    try {
      final var userId = UUID.fromString(principal);
      return userJpaRepository.findById(userId).map(u -> new UserSummary(u.getId(), u.getName()));
    } catch (IllegalArgumentException _) {
      return userJpaRepository
          .findByName(principal)
          .map(u -> new UserSummary(u.getId(), u.getName()));
    }
  }

  @Override
  public Optional<QuezapDetails> findById(UUID id) {
    return quezapJpaRepository.findById(id).map(this::toDetails);
  }

  @Override
  public PageOf<QuezapSummary> list(
      @Nullable String search,
      @Nullable UUID authorId,
      @Nullable UUID currentUserId,
      Pagination pagination) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

    final Page<QuezapJpaEntity> page;
    if (search != null && !search.isBlank()) {
      if (authorId != null) {
        if (authorId.equals(currentUserId)) {
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndAuthorId(
                  search, authorId, pageRequest);
        } else {
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndIsPublicTrue(
                  search, pageRequest);
        }
      } else if (currentUserId != null) {
        page =
            quezapJpaRepository.findByTitleContainingIgnoreCaseAndAuthorId(
                search, currentUserId, pageRequest);
      } else {
        page =
            quezapJpaRepository.findByTitleContainingIgnoreCaseAndIsPublicTrue(search, pageRequest);
      }
    } else if (authorId != null) {
      if (authorId.equals(currentUserId)) {
        page = quezapJpaRepository.findByAuthorId(authorId, pageRequest);
      } else {
        page = quezapJpaRepository.findByAuthorIdAndIsPublicTrue(authorId, pageRequest);
      }
    } else if (currentUserId != null) {
      page = quezapJpaRepository.findByAuthorId(currentUserId, pageRequest);
    } else {
      page = quezapJpaRepository.findByIsPublicTrue(pageRequest);
    }

    final var items =
        page.getContent().stream()
            .map(
                q ->
                    new QuezapSummary(
                        q.getId(),
                        q.getTitle(),
                        q.getDescription(),
                        Boolean.TRUE.equals(q.getIsPublic()),
                        q.getAuthor().getId(),
                        q.getQuestions().size(),
                        q.getCreatedAt()))
            .toList();

    return PageOf.of(pagination, items, page.getTotalElements());
  }

  @Override
  public QuezapSaved create(
      UUID authorId,
      String title,
      @Nullable String description,
      boolean isPublic,
      List<SlideInput> slides) {
    final UserJpaEntity author =
        userJpaRepository
            .findById(authorId)
            .orElseThrow(() -> new IllegalStateException("Author not found: " + authorId));

    final var now = Instant.now();
    final var quezap =
        new QuezapJpaEntity(UuidV7.randomUuid(), title, description, author, isPublic, now, now);

    var order = 1;
    for (final var slide : slides) {
      final var question = resolveOrCreateQuestion(slide, order, Set.of());
      final var qq =
          new QuezapQuestionJpaEntity(
              UuidV7.randomUuid(), quezap, question, slide.timerSeconds(), slide.points(), order++);
      quezap.addQuestion(qq);
    }

    final var saved = quezapJpaRepository.save(quezap);
    final var effectiveSaved = saved != null ? saved : quezap;
    return new QuezapSaved(effectiveSaved.getId(), collectPictureUrls(effectiveSaved));
  }

  @Override
  public QuezapSaved update(
      UUID quezapId,
      @Nullable String title,
      @Nullable String description,
      @Nullable Boolean isPublic,
      @Nullable List<SlideInput> slides) {
    final var quezap =
        quezapJpaRepository
            .findById(quezapId)
            .orElseThrow(() -> new IllegalStateException("Quezap not found: " + quezapId));

    if (title != null) {
      quezap.setTitle(title);
    }
    quezap.setDescription(description);
    if (isPublic != null) {
      quezap.setIsPublic(isPublic);
    }

    if (slides != null) {
      final var ownedQuestionIds =
          quezap.getQuestions().stream()
              .map(qq -> qq.getQuestion().getId())
              .collect(Collectors.toSet());
      quezap.getQuestions().clear();
      var order = 1;
      for (final var slide : slides) {
        final var question = resolveOrCreateQuestion(slide, order, ownedQuestionIds);
        final var qq =
            new QuezapQuestionJpaEntity(
                UuidV7.randomUuid(),
                quezap,
                question,
                slide.timerSeconds(),
                slide.points(),
                order++);
        quezap.addQuestion(qq);
      }
    }

    quezap.setUpdatedAt(Instant.now());
    final var saved = quezapJpaRepository.save(quezap);
    final var effectiveSaved = saved != null ? saved : quezap;
    return new QuezapSaved(effectiveSaved.getId(), collectPictureUrls(effectiveSaved));
  }

  @Override
  public void delete(UUID quezapId) {
    final var quezap =
        quezapJpaRepository
            .findById(quezapId)
            .orElseThrow(() -> new IllegalStateException("Quezap not found: " + quezapId));
    quezapJpaRepository.detachSessions(quezapId);
    quezapJpaRepository.delete(quezap);
  }

  @Override
  public UUID cloneQuezap(UUID originalId, UUID authorId, boolean keepPublicFlag) {
    final var original =
        quezapJpaRepository
            .findById(originalId)
            .orElseThrow(() -> new IllegalStateException("Quezap not found: " + originalId));
    final var author =
        userJpaRepository
            .findById(authorId)
            .orElseThrow(() -> new IllegalStateException("Author not found: " + authorId));

    final var now = Instant.now();
    final var isPublic = keepPublicFlag && Boolean.TRUE.equals(original.getIsPublic());
    final var copy =
        new QuezapJpaEntity(
            UuidV7.randomUuid(),
            original.getTitle() + " (Copie)",
            original.getDescription(),
            author,
            isPublic,
            now,
            now);

    final var sortedOriginalQuestions =
        original.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    for (final var qq : sortedOriginalQuestions) {
      final var clonedQuestion = cloneQuestion(qq.getQuestion());
      final var copyQq =
          new QuezapQuestionJpaEntity(
              UuidV7.randomUuid(),
              copy,
              clonedQuestion,
              qq.getTimerSeconds(),
              qq.getPoints(),
              qq.getQuestionOrder());
      copy.addQuestion(copyQq);
    }

    final var saved = quezapJpaRepository.save(copy);
    final var effectiveSaved = saved != null ? saved : copy;
    return effectiveSaved.getId();
  }

  @Override
  public QuezapSaved addQuestion(UUID quezapId, SlideInput slide) {
    final var quezap =
        quezapJpaRepository
            .findById(quezapId)
            .orElseThrow(() -> new IllegalStateException("Quezap not found: " + quezapId));

    final var order = quezap.getQuestions().size() + 1;
    final var question = resolveOrCreateQuestion(slide, order, Set.of());
    final var qq =
        new QuezapQuestionJpaEntity(
            UuidV7.randomUuid(), quezap, question, slide.timerSeconds(), slide.points(), order);

    quezap.addQuestion(qq);
    quezap.setUpdatedAt(Instant.now());
    final var saved = quezapJpaRepository.save(quezap);
    final var effectiveSaved = saved != null ? saved : quezap;
    return new QuezapSaved(effectiveSaved.getId(), collectPictureUrls(effectiveSaved));
  }

  @Override
  public List<String> removeQuestion(UUID quezapId, UUID questionId) {
    final var quezap =
        quezapJpaRepository
            .findById(quezapId)
            .orElseThrow(() -> new IllegalStateException("Quezap not found: " + quezapId));

    final var removedQuestions =
        quezap.getQuestions().stream()
            .filter(qq -> qq.getQuestion().getId().equals(questionId))
            .map(QuezapQuestionJpaEntity::getQuestion)
            .toList();

    quezap.getQuestions().removeIf(qq -> qq.getQuestion().getId().equals(questionId));
    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);

    final var urls = new ArrayList<String>();
    for (final var q : removedQuestions) {
      if (q.getPictureFilename() != null && !q.getPictureFilename().isBlank()) {
        urls.add(q.getPictureFilename().trim());
      }
      for (final var a : q.getAnswers()) {
        if (a.getPictureFilename() != null && !a.getPictureFilename().isBlank()) {
          urls.add(a.getPictureFilename().trim());
        }
      }
    }
    return urls;
  }

  @Override
  public boolean existsQuestion(UUID questionId) {
    return questionJpaRepository.findById(questionId).isPresent();
  }

  @Override
  public boolean questionHasAnswers(UUID questionId) {
    return questionJpaRepository
        .findById(questionId)
        .map(q -> q.getAnswers() != null && !q.getAnswers().isEmpty())
        .orElse(false);
  }

  @Override
  public boolean isPictureReferenced(Collection<String> pictureCandidates) {
    if (pictureCandidates.isEmpty()) {
      return false;
    }
    return questionJpaRepository.existsByPictureFilenameIn(pictureCandidates)
        || questionJpaRepository.existsAnswerByPictureFilenameIn(pictureCandidates);
  }

  @Override
  public void flush() {
    quezapJpaRepository.flush();
    questionJpaRepository.flush();
  }

  private QuestionJpaEntity resolveOrCreateQuestion(
      SlideInput slide, int order, Set<UUID> allowedQuestionIds) {
    final var inlineQuestion = slide.question();
    if (inlineQuestion != null) {
      final var answersInput = inlineQuestion.answers();
      if (answersInput == null || answersInput.isEmpty()) {
        throw new IllegalArgumentException("Question #" + order + " has no answers");
      }

      if (inlineQuestion.id() != null && allowedQuestionIds.contains(inlineQuestion.id())) {
        final var existingOpt = questionJpaRepository.findById(inlineQuestion.id());
        if (existingOpt.isPresent()) {
          return updateExistingQuestion(existingOpt.get(), inlineQuestion, answersInput);
        }
      }

      final var theme = resolveTheme(inlineQuestion.themeId(), inlineQuestion.themeName());
      final var newQuestionId =
          (inlineQuestion.id() != null && allowedQuestionIds.contains(inlineQuestion.id()))
              ? inlineQuestion.id()
              : UuidV7.randomUuid();
      final var now = Instant.now();
      final var newQuestion =
          new QuestionJpaEntity(
              newQuestionId,
              inlineQuestion.type() != null ? inlineQuestion.type() : "QUIZZ",
              inlineQuestion.label(),
              inlineQuestion.pictureFilename(),
              theme,
              now,
              now);

      var ansOrder = 1;
      for (final var ansInput : answersInput) {
        final var effectiveOrder =
            ansInput.answerOrder() != null ? ansInput.answerOrder() : ansOrder++;
        final var ansEntity =
            new AnswerJpaEntity(
                UuidV7.randomUuid(),
                newQuestion,
                ansInput.label(),
                ansInput.pictureFilename(),
                ansInput.isCorrect(),
                effectiveOrder);
        newQuestion.addAnswer(ansEntity);
      }
      final var saved = questionJpaRepository.save(newQuestion);
      return saved != null ? saved : newQuestion;
    }

    if (slide.questionId() != null) {
      return questionJpaRepository
          .findById(slide.questionId())
          .orElseThrow(
              () -> new IllegalArgumentException("Question not found: " + slide.questionId()));
    }

    throw new IllegalArgumentException(
        "Slide #" + order + " must provide a questionId or inline question");
  }

  private QuestionJpaEntity updateExistingQuestion(
      QuestionJpaEntity existing,
      InlineQuestionInput inlineQuestion,
      List<AnswerInput> answersInput) {
    existing.setLabel(inlineQuestion.label());
    if (inlineQuestion.type() != null) {
      existing.setType(inlineQuestion.type());
    }
    existing.setPictureFilename(inlineQuestion.pictureFilename());
    if (inlineQuestion.themeId() != null || inlineQuestion.themeName() != null) {
      existing.setTheme(resolveTheme(inlineQuestion.themeId(), inlineQuestion.themeName()));
    }

    final var existingAnswersById = new HashMap<UUID, AnswerJpaEntity>();
    for (final var existingAns : existing.getAnswers()) {
      existingAnswersById.put(existingAns.getId(), existingAns);
    }

    final var retainedIds = new HashSet<UUID>();
    final var newAnswers = new ArrayList<AnswerJpaEntity>();
    var ansOrder = 1;
    for (final var ansInput : answersInput) {
      final var effectiveOrder =
          ansInput.answerOrder() != null ? ansInput.answerOrder() : ansOrder++;
      if (ansInput.id() != null && existingAnswersById.containsKey(ansInput.id())) {
        final var matched = existingAnswersById.get(ansInput.id());
        if (matched != null) {
          matched.setLabel(ansInput.label());
          matched.setPictureFilename(ansInput.pictureFilename());
          matched.setIsCorrect(ansInput.isCorrect());
          matched.setAnswerOrder(effectiveOrder);
          retainedIds.add(ansInput.id());
        }
      } else {
        newAnswers.add(
            new AnswerJpaEntity(
                UuidV7.randomUuid(),
                existing,
                ansInput.label(),
                ansInput.pictureFilename(),
                ansInput.isCorrect(),
                effectiveOrder));
      }
    }

    existing.getAnswers().removeIf(a -> !retainedIds.contains(a.getId()));
    for (final var ansEntity : newAnswers) {
      existing.addAnswer(ansEntity);
    }
    existing.setUpdatedAt(Instant.now());
    final var saved = questionJpaRepository.save(existing);
    return saved != null ? saved : existing;
  }

  private QuestionJpaEntity cloneQuestion(QuestionJpaEntity source) {
    final var now = Instant.now();
    final var clonedQuestion =
        new QuestionJpaEntity(
            UuidV7.randomUuid(),
            source.getType(),
            source.getLabel(),
            source.getPictureFilename(),
            source.getTheme(),
            now,
            now);
    final var sortedAnswers =
        source.getAnswers().stream()
            .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
            .toList();
    for (final var ans : sortedAnswers) {
      final var clonedAnswer =
          new AnswerJpaEntity(
              UuidV7.randomUuid(),
              clonedQuestion,
              ans.getLabel(),
              ans.getPictureFilename(),
              ans.getIsCorrect(),
              ans.getAnswerOrder());
      clonedQuestion.addAnswer(clonedAnswer);
    }
    final var saved = questionJpaRepository.save(clonedQuestion);
    return saved != null ? saved : clonedQuestion;
  }

  private ThemeJpaEntity resolveTheme(@Nullable UUID themeId, @Nullable String themeName) {
    if (themeId != null) {
      return themeJpaRepository
          .findById(themeId)
          .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));
    }
    if (themeName != null && !themeName.isBlank()) {
      final var trimmedName = themeName.trim();
      return themeJpaRepository
          .findByName(trimmedName)
          .orElseGet(
              () -> {
                final var entity =
                    new ThemeJpaEntity(UuidV7.randomUuid(), trimmedName, Instant.now());
                final var saved = themeJpaRepository.save(entity);
                return saved != null ? saved : entity;
              });
    }
    return themeJpaRepository
        .findByName("Général")
        .orElseGet(
            () -> {
              final var entity = new ThemeJpaEntity(UuidV7.randomUuid(), "Général", Instant.now());
              final var saved = themeJpaRepository.save(entity);
              return saved != null ? saved : entity;
            });
  }

  private Set<String> collectPictureUrls(QuezapJpaEntity quezap) {
    final var urls = new HashSet<String>();
    for (final var qq : quezap.getQuestions()) {
      final var q = qq.getQuestion();
      if (q.getPictureFilename() != null && !q.getPictureFilename().isBlank()) {
        urls.add(q.getPictureFilename().trim());
      }
      for (final var a : q.getAnswers()) {
        if (a.getPictureFilename() != null && !a.getPictureFilename().isBlank()) {
          urls.add(a.getPictureFilename().trim());
        }
      }
    }
    return urls;
  }

  private QuezapDetails toDetails(QuezapJpaEntity quezap) {
    final var author = new UserSummary(quezap.getAuthor().getId(), quezap.getAuthor().getName());

    final var sortedQuestions =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();

    final var slides = new ArrayList<QuezapSlideDetails>(sortedQuestions.size());
    for (final var qq : sortedQuestions) {
      final var q = qq.getQuestion();
      final var theme = new ThemeSummary(q.getTheme().getId(), q.getTheme().getName());
      final var answers =
          q.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .map(
                  a ->
                      new AnswerDetails(
                          a.getId(),
                          a.getLabel(),
                          Boolean.TRUE.equals(a.getIsCorrect()),
                          a.getPictureFilename()))
              .toList();
      final var question =
          new QuestionDetails(
              q.getId(), q.getType(), q.getLabel(), q.getPictureFilename(), theme, answers);
      slides.add(
          new QuezapSlideDetails(
              qq.getQuestionOrder(), qq.getTimerSeconds(), qq.getPoints(), question));
    }

    return new QuezapDetails(
        quezap.getId(),
        quezap.getTitle(),
        quezap.getDescription(),
        Boolean.TRUE.equals(quezap.getIsPublic()),
        author,
        slides,
        quezap.getCreatedAt(),
        quezap.getUpdatedAt());
  }
}
