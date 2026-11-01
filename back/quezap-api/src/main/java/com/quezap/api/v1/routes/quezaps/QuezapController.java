package com.quezap.api.v1.routes.quezaps;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.quezaps.CreateQuezapRequest;
import com.quezap.api.v1.dto.request.quezaps.QuezapSlideRequest;
import com.quezap.api.v1.dto.request.quezaps.UpdateQuezapRequest;
import com.quezap.api.v1.dto.response.PageOfDto;
import com.quezap.api.v1.dto.response.quezaps.AnswerDetailDto;
import com.quezap.api.v1.dto.response.quezaps.QuestionDetailDto;
import com.quezap.api.v1.dto.response.quezaps.QuezapDetailDto;
import com.quezap.api.v1.dto.response.quezaps.QuezapIdDto;
import com.quezap.api.v1.dto.response.quezaps.QuezapSlideDetailDto;
import com.quezap.api.v1.dto.response.quezaps.QuezapSummaryDto;
import com.quezap.api.v1.dto.response.quezaps.ThemeDetailDto;
import com.quezap.api.v1.dto.response.users.UserSummaryDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.utils.UuidV7;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/quezaps")
@Transactional
public class QuezapController {

  private static final String PUBLIC_BUCKET_PREFIX = "/quezap-public-images/";

  private final QuezapJpaRepository quezapJpaRepository;
  private final QuestionJpaRepository questionJpaRepository;
  private final ThemeJpaRepository themeJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final PaginationMapper paginationMapper;
  private final @Nullable QuestionPictureManager questionPictureManager;

  public QuezapController(
      QuezapJpaRepository quezapJpaRepository,
      QuestionJpaRepository questionJpaRepository,
      ThemeJpaRepository themeJpaRepository,
      UserJpaRepository userJpaRepository,
      PaginationMapper paginationMapper) {
    this(
        quezapJpaRepository,
        questionJpaRepository,
        themeJpaRepository,
        userJpaRepository,
        paginationMapper,
        null);
  }

  @Autowired
  public QuezapController(
      QuezapJpaRepository quezapJpaRepository,
      QuestionJpaRepository questionJpaRepository,
      ThemeJpaRepository themeJpaRepository,
      UserJpaRepository userJpaRepository,
      PaginationMapper paginationMapper,
      @Nullable QuestionPictureManager questionPictureManager) {
    this.quezapJpaRepository = quezapJpaRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.themeJpaRepository = themeJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.paginationMapper = paginationMapper;
    this.questionPictureManager = questionPictureManager;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuezapIdDto create(@Valid @RequestBody CreateQuezapRequest request) {
    final var author = getCurrentUser();

    final var isPublic = Objects.requireNonNullElse(request.isPublic(), Boolean.TRUE);
    final var quezap =
        new QuezapJpaEntity(
            UuidV7.randomUuid(),
            request.title(),
            request.description(),
            author,
            isPublic,
            Instant.now(),
            Instant.now());

    final var effectiveSlides = request.getEffectiveSlides();
    if (effectiveSlides.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questions list cannot be empty");
    }

    var order = 1;
    for (final var slideReq : effectiveSlides) {
      final var question = resolveOrCreateQuestion(slideReq, order, java.util.Set.of());
      final var timer = slideReq.getEffectiveTimer();
      final var points = Objects.requireNonNullElse(slideReq.points(), 1000);
      final var qq =
          new QuezapQuestionJpaEntity(
              UuidV7.randomUuid(), quezap, question, timer, points, order++);
      quezap.addQuestion(qq);
    }

    final var saved = quezapJpaRepository.save(quezap);

    return new QuezapIdDto(saved.getId());
  }

  @PostMapping("{id}/duplicate")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public QuezapIdDto duplicate(@PathVariable UUID id) {
    final var original =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    final var currentUser = getCurrentUser();
    if (!original.getAuthor().getId().equals(currentUser.getId())
        && !Boolean.TRUE.equals(original.getIsPublic())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Impossible de dupliquer un questionnaire privé d'un autre auteur");
    }

    final var copy =
        new QuezapJpaEntity(
            UuidV7.randomUuid(),
            original.getTitle() + " (Copie)",
            original.getDescription(),
            currentUser,
            false,
            Instant.now(),
            Instant.now());

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
    return new QuezapIdDto(saved.getId());
  }

  @GetMapping("{id}")
  @Transactional(readOnly = true)
  public QuezapDetailDto getById(@PathVariable UUID id) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    final var auth = SecurityContextHolder.getContext().getAuthentication();
    final var currentUserId =
        (auth != null && auth.getPrincipal() != null) ? auth.getPrincipal().toString() : null;
    final var isAuthor =
        currentUserId != null && currentUserId.equals(quezap.getAuthor().getId().toString());

    if (!isAuthor && !Boolean.TRUE.equals(quezap.getIsPublic())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to private quezap");
    }

    final var authorDto =
        new UserSummaryDto(quezap.getAuthor().getId(), quezap.getAuthor().getName());

    final var slideDtos = new ArrayList<QuezapSlideDetailDto>();
    final var sortedQuestions =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    for (final var qq : sortedQuestions) {
      final var q = qq.getQuestion();
      final var themeDto = new ThemeDetailDto(q.getTheme().getId(), q.getTheme().getName());

      final var answerDtos =
          q.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .map(
                  a ->
                      new AnswerDetailDto(
                          a.getId(),
                          a.getLabel(),
                          isAuthor ? a.getIsCorrect() : false,
                          toPublicPictureUrl(a.getPictureFilename())))
              .toList();

      final var questionDto =
          new QuestionDetailDto(
              q.getId(),
              q.getType(),
              q.getLabel(),
              toPublicPictureUrl(q.getPictureFilename()),
              themeDto,
              answerDtos);

      slideDtos.add(
          new QuezapSlideDetailDto(
              qq.getQuestionOrder(), qq.getTimerSeconds(), qq.getPoints(), questionDto));
    }

    return new QuezapDetailDto(
        quezap.getId(),
        quezap.getTitle(),
        quezap.getDescription(),
        quezap.getIsPublic(),
        authorDto,
        slideDtos,
        quezap.getCreatedAt(),
        quezap.getUpdatedAt());
  }

  @GetMapping
  @Transactional(readOnly = true)
  public PageOfDto<QuezapSummaryDto> list(
      @RequestParam(required = false) @Nullable String search,
      @RequestParam(required = false) @Nullable UUID authorId,
      PaginationDto paginationDto) {

    final var pagination = paginationMapper.toDomain(paginationDto);
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

    final var auth = SecurityContextHolder.getContext().getAuthentication();
    final var currentUserId =
        (auth != null && auth.getPrincipal() != null) ? auth.getPrincipal().toString() : null;

    Page<QuezapJpaEntity> page;

    if (search != null && !search.isBlank()) {
      if (authorId != null) {
        final var isAuthor = currentUserId != null && currentUserId.equals(authorId.toString());
        if (isAuthor) {
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndAuthorId(
                  search, authorId, pageRequest);
        } else {
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndIsPublicTrue(
                  search, pageRequest);
        }
      } else if (currentUserId != null) {
        try {
          final var userUuid = UUID.fromString(currentUserId);
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndAuthorId(
                  search, userUuid, pageRequest);
        } catch (IllegalArgumentException _) {
          page =
              quezapJpaRepository.findByTitleContainingIgnoreCaseAndIsPublicTrue(
                  search, pageRequest);
        }
      } else {
        page =
            quezapJpaRepository.findByTitleContainingIgnoreCaseAndIsPublicTrue(search, pageRequest);
      }
    } else if (authorId != null) {
      final var isAuthor = currentUserId != null && currentUserId.equals(authorId.toString());
      if (isAuthor) {
        page = quezapJpaRepository.findByAuthorId(authorId, pageRequest);
      } else {
        page = quezapJpaRepository.findByAuthorIdAndIsPublicTrue(authorId, pageRequest);
      }
    } else if (currentUserId != null) {
      try {
        final var userUuid = UUID.fromString(currentUserId);
        page = quezapJpaRepository.findByAuthorId(userUuid, pageRequest);
      } catch (IllegalArgumentException _) {
        page = quezapJpaRepository.findByIsPublicTrue(pageRequest);
      }
    } else {
      page = quezapJpaRepository.findByIsPublicTrue(pageRequest);
    }

    final var items =
        page.getContent().stream()
            .map(
                q ->
                    new QuezapSummaryDto(
                        q.getId(),
                        q.getTitle(),
                        q.getDescription(),
                        q.getIsPublic(),
                        q.getAuthor().getId(),
                        q.getQuestions().size(),
                        q.getCreatedAt()))
            .toList();

    return new PageOfDto<>(
        items,
        page.getTotalElements(),
        page.getTotalPages(),
        pagination.pageNumber(),
        pagination.pageSize(),
        page.hasNext(),
        page.hasPrevious());
  }

  @PutMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable UUID id, @Valid @RequestBody UpdateQuezapRequest request) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    assertQuezapAuthor(quezap);

    if (request.title() != null) {
      quezap.setTitle(request.title());
    }
    quezap.setDescription(request.description());
    if (request.isPublic() != null) {
      quezap.setIsPublic(request.isPublic());
    }

    if (request.slides() != null) {
      final var ownedQuestionIds =
          quezap.getQuestions().stream()
              .map(qq -> qq.getQuestion().getId())
              .collect(java.util.stream.Collectors.toSet());
      quezap.getQuestions().clear();
      var order = 1;
      for (final var slideReq : request.slides()) {
        final var question = resolveOrCreateQuestion(slideReq, order, ownedQuestionIds);
        final var timer = slideReq.getEffectiveTimer();
        final var points = Objects.requireNonNullElse(slideReq.points(), 1000);
        final var qq =
            new QuezapQuestionJpaEntity(
                UuidV7.randomUuid(), quezap, question, timer, points, order++);
        quezap.addQuestion(qq);
      }
    }

    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    assertQuezapAuthor(quezap);
    quezapJpaRepository.delete(quezap);
  }

  @PostMapping("{id}/questions")
  @ResponseStatus(HttpStatus.CREATED)
  public void addQuestion(@PathVariable UUID id, @Valid @RequestBody QuezapSlideRequest request) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    assertQuezapAuthor(quezap);

    final var order = quezap.getQuestions().size() + 1;
    final var question = resolveOrCreateQuestion(request, order, java.util.Set.of());
    final var timer = request.getEffectiveTimer();
    final var points = Objects.requireNonNullElse(request.points(), 1000);
    final var qq =
        new QuezapQuestionJpaEntity(UuidV7.randomUuid(), quezap, question, timer, points, order);

    quezap.addQuestion(qq);
    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);
  }

  private QuestionJpaEntity resolveOrCreateQuestion(
      QuezapSlideRequest slideReq, int order, java.util.Set<UUID> allowedQuestionIds) {
    if (slideReq.question() != null) {
      final var qReq = slideReq.question();
      final var answersReq = qReq.answers();
      if (answersReq == null || answersReq.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Question #" + order + " has no answers");
      }

      final var resolvedQuestionPicture = resolvePictureFilename(qReq.pictureFilename());

      if (qReq.id() != null && allowedQuestionIds.contains(qReq.id())) {
        final var existingOpt = questionJpaRepository.findById(qReq.id());
        if (existingOpt.isPresent()) {
          final var existing = existingOpt.get();
          existing.setLabel(qReq.label());
          if (qReq.type() != null) {
            existing.setType(qReq.type());
          }
          existing.setPictureFilename(resolvedQuestionPicture);
          if (qReq.themeId() != null || qReq.themeName() != null) {
            existing.setTheme(resolveTheme(qReq.themeId(), qReq.themeName()));
          }
          final var existingAnswersById = new java.util.HashMap<UUID, AnswerJpaEntity>();
          for (final var existingAns : existing.getAnswers()) {
            existingAnswersById.put(existingAns.getId(), existingAns);
          }
          final var retainedIds = new java.util.HashSet<UUID>();
          final var newAnswers = new ArrayList<AnswerJpaEntity>();
          var ansOrder = 1;
          for (final var ansReq : answersReq) {
            final var effectiveOrder =
                ansReq.answerOrder() != null ? ansReq.answerOrder() : ansOrder++;
            final var effectiveCorrect =
                Objects.requireNonNullElse(ansReq.isCorrect(), Boolean.FALSE);
            final var effectivePic = resolvePictureFilename(ansReq.pictureFilename());
            if (ansReq.id() != null && existingAnswersById.containsKey(ansReq.id())) {
              final var matched = existingAnswersById.get(ansReq.id());
              matched.setLabel(ansReq.label());
              matched.setPictureFilename(effectivePic);
              matched.setIsCorrect(effectiveCorrect);
              matched.setAnswerOrder(effectiveOrder);
              retainedIds.add(ansReq.id());
            } else {
              newAnswers.add(
                  new AnswerJpaEntity(
                      UuidV7.randomUuid(),
                      existing,
                      ansReq.label(),
                      effectivePic,
                      effectiveCorrect,
                      effectiveOrder));
            }
          }
          existing.getAnswers().removeIf(a -> !retainedIds.contains(a.getId()));
          for (final var ansEntity : newAnswers) {
            existing.addAnswer(ansEntity);
          }
          existing.setUpdatedAt(Instant.now());
          return questionJpaRepository.save(existing);
        }
      }

      final var theme = resolveTheme(qReq.themeId(), qReq.themeName());
      final var newQuestionId =
          (qReq.id() != null && allowedQuestionIds.contains(qReq.id()))
              ? qReq.id()
              : UuidV7.randomUuid();
      final var newQuestion =
          new QuestionJpaEntity(
              newQuestionId,
              qReq.type() != null ? qReq.type() : "QUIZZ",
              qReq.label(),
              resolvedQuestionPicture,
              theme,
              Instant.now(),
              Instant.now());

      var ansOrder = 1;
      for (final var ansReq : answersReq) {
        final var ansEntity =
            new AnswerJpaEntity(
                UuidV7.randomUuid(),
                newQuestion,
                ansReq.label(),
                resolvePictureFilename(ansReq.pictureFilename()),
                Objects.requireNonNullElse(ansReq.isCorrect(), Boolean.FALSE),
                ansReq.answerOrder() != null ? ansReq.answerOrder() : ansOrder++);
        newQuestion.addAnswer(ansEntity);
      }
      return questionJpaRepository.save(newQuestion);
    }

    if (slideReq.questionId() != null) {
      final var question =
          questionJpaRepository
              .findById(slideReq.questionId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "Question not found: " + slideReq.questionId()));

      final var answers = question.getAnswers();
      if (answers == null || answers.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Question #" + order + " has no answers");
      }

      return question;
    }

    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Slide #" + order + " must provide a questionId or inline question");
  }

  private @Nullable String resolvePictureFilename(@Nullable String rawPicture) {
    if (rawPicture == null || rawPicture.isBlank()) {
      return null;
    }

    final var trimmed = rawPicture.trim();
    if (!trimmed.startsWith("data:")) {
      return toPublicPictureUrl(trimmed);
    }

    final var commaIndex = trimmed.indexOf(',');
    if (commaIndex <= 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data URL");
    }

    final var header = trimmed.substring(5, commaIndex);
    final var base64Payload = trimmed.substring(commaIndex + 1).trim();
    final var semicolonIndex = header.indexOf(';');
    final var mimeType =
        (semicolonIndex >= 0 ? header.substring(0, semicolonIndex) : header)
            .trim()
            .toLowerCase(Locale.ROOT);
    final var pictureType = resolvePictureType(mimeType);

    final byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(base64Payload);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base64 image data", e);
    }

    if (bytes.length == 0) {
      return null;
    }

    if (questionPictureManager != null) {
      final var uploadData =
          new PictureUploadData(new ByteArrayInputStream(bytes), bytes.length, pictureType);
      final var storedPicture = questionPictureManager.store(uploadData);
      return toPublicPictureUrl(storedPicture.objectKey());
    }

    final var extension = pictureType.extensions().get(0);
    return PUBLIC_BUCKET_PREFIX + "pictures/questions/" + UuidV7.randomUuid() + "." + extension;
  }

  private PictureType resolvePictureType(String mimeType) {
    return switch (mimeType) {
      case "image/png" -> PictureType.PNG;
      case "image/webp" -> PictureType.WEBP;
      case "image/jpeg", "image/jpg" -> PictureType.JPG;
      default -> PictureType.PNG;
    };
  }

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

  private ThemeJpaEntity resolveTheme(@Nullable UUID themeId, @Nullable String themeName) {
    if (themeId != null) {
      return themeJpaRepository
          .findById(themeId)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.BAD_REQUEST, "Theme not found: " + themeId));
    }
    if (themeName != null && !themeName.isBlank()) {
      return themeJpaRepository
          .findByName(themeName.trim())
          .orElseGet(
              () ->
                  themeJpaRepository.save(
                      new ThemeJpaEntity(UuidV7.randomUuid(), themeName.trim(), Instant.now())));
    }
    return themeJpaRepository
        .findByName("Général")
        .orElseGet(
            () ->
                themeJpaRepository.save(
                    new ThemeJpaEntity(UuidV7.randomUuid(), "Général", Instant.now())));
  }

  @DeleteMapping("{id}/questions/{questionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeQuestion(@PathVariable UUID id, @PathVariable UUID questionId) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    assertQuezapAuthor(quezap);

    quezap.getQuestions().removeIf(qq -> qq.getQuestion().getId().equals(questionId));
    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);
  }

  private void assertQuezapAuthor(QuezapJpaEntity quezap) {
    final var currentUser = getCurrentUser();
    if (!quezap.getAuthor().getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Seul le créateur peut modifier ou supprimer ce questionnaire");
    }
  }

  @PostMapping("{id}/clone")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public QuezapIdDto clone(@PathVariable UUID id) {
    final var original =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));
    final var author = getCurrentUser();
    if (!original.getAuthor().getId().equals(author.getId())
        && !Boolean.TRUE.equals(original.getIsPublic())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Impossible de cloner un questionnaire privé d'un autre auteur");
    }

    final var cloned =
        new QuezapJpaEntity(
            UuidV7.randomUuid(),
            original.getTitle() + " (Copie)",
            original.getDescription(),
            author,
            original.getIsPublic(),
            Instant.now(),
            Instant.now());

    final var sortedOriginalQuestions =
        original.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    for (final var qq : sortedOriginalQuestions) {
      final var clonedQuestion = cloneQuestion(qq.getQuestion());
      final var newQq =
          new QuezapQuestionJpaEntity(
              UuidV7.randomUuid(),
              cloned,
              clonedQuestion,
              qq.getTimerSeconds(),
              qq.getPoints(),
              qq.getQuestionOrder());
      cloned.addQuestion(newQq);
    }

    final var saved = quezapJpaRepository.save(cloned);
    return new QuezapIdDto(saved.getId());
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

  private com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity
      getCurrentUser() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() != null) {
      final var principal = auth.getPrincipal().toString();
      try {
        final var userId = UUID.fromString(principal);
        final var userOpt = userJpaRepository.findById(userId);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      } catch (IllegalArgumentException _) {
        final var userOpt = userJpaRepository.findByName(principal);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
  }
}
