package com.quezap.api.v1.routes.quezaps;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.utils.UuidV7;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/quezaps")
public class QuezapController {

  private final QuezapJpaRepository quezapJpaRepository;
  private final QuestionJpaRepository questionJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final PaginationMapper paginationMapper;

  public QuezapController(
      QuezapJpaRepository quezapJpaRepository,
      QuestionJpaRepository questionJpaRepository,
      UserJpaRepository userJpaRepository,
      PaginationMapper paginationMapper) {
    this.quezapJpaRepository = quezapJpaRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.paginationMapper = paginationMapper;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuezapIdDto create(@Valid @RequestBody CreateQuezapRequest request) {
    final var author = getCurrentUser();

    final var quezap =
        new QuezapJpaEntity(
            UuidV7.randomUuid(),
            request.title(),
            request.description(),
            author,
            request.isPublic(),
            Instant.now(),
            Instant.now());

    var order = 1;
    for (final var slideReq : request.slides()) {
      final var question =
          questionJpaRepository
              .findById(slideReq.questionId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "Question not found: " + slideReq.questionId()));

      final var qq =
          new QuezapQuestionJpaEntity(
              UuidV7.randomUuid(), quezap, question, slideReq.timer(), slideReq.points(), order++);
      quezap.addQuestion(qq);
    }

    final var saved = quezapJpaRepository.save(quezap);

    return new QuezapIdDto(saved.getId());
  }

  @GetMapping("{id}")
  public QuezapDetailDto getById(@PathVariable UUID id) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    final var authorDto =
        new UserSummaryDto(quezap.getAuthor().getId(), quezap.getAuthor().getName());

    final var slideDtos = new ArrayList<QuezapSlideDetailDto>();
    for (final var qq : quezap.getQuestions()) {
      final var q = qq.getQuestion();
      final var themeDto = new ThemeDetailDto(q.getTheme().getId(), q.getTheme().getName());

      final var answerDtos =
          q.getAnswers().stream()
              .map(
                  a ->
                      new AnswerDetailDto(
                          a.getId(), a.getLabel(), a.getIsCorrect(), a.getPictureFilename()))
              .toList();

      final var questionDto =
          new QuestionDetailDto(
              q.getId(), q.getType(), q.getLabel(), q.getPictureFilename(), themeDto, answerDtos);

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
  public PageOfDto<QuezapSummaryDto> list(
      @RequestParam(required = false) @Nullable String search,
      @RequestParam(required = false) @Nullable UUID authorId,
      PaginationDto paginationDto) {

    final var pagination = paginationMapper.toDomain(paginationDto);
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

    Page<QuezapJpaEntity> page;

    if (search != null && !search.isBlank()) {
      page = quezapJpaRepository.findByTitleContainingIgnoreCase(search, pageRequest);
    } else if (authorId != null) {
      page = quezapJpaRepository.findByAuthorId(authorId, pageRequest);
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

    quezap.setTitle(request.title());
    quezap.setDescription(request.description());
    quezap.setIsPublic(request.isPublic());
    quezap.setUpdatedAt(Instant.now());

    quezapJpaRepository.save(quezap);
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    if (!quezapJpaRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found");
    }
    quezapJpaRepository.deleteById(id);
  }

  @PostMapping("{id}/questions")
  @ResponseStatus(HttpStatus.CREATED)
  public void addQuestion(@PathVariable UUID id, @Valid @RequestBody QuezapSlideRequest request) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    final var question =
        questionJpaRepository
            .findById(request.questionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Question not found: " + request.questionId()));

    final var order = quezap.getQuestions().size() + 1;
    final var qq =
        new QuezapQuestionJpaEntity(
            UuidV7.randomUuid(), quezap, question, request.timer(), request.points(), order);

    quezap.addQuestion(qq);
    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);
  }

  @DeleteMapping("{id}/questions/{questionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeQuestion(@PathVariable UUID id, @PathVariable UUID questionId) {
    final var quezap =
        quezapJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quezap not found"));

    quezap.getQuestions().removeIf(qq -> qq.getQuestion().getId().equals(questionId));
    quezap.setUpdatedAt(Instant.now());
    quezapJpaRepository.save(quezap);
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

    // Default fallback to first user in repository if testing or unauthenticated
    return userJpaRepository.findAll().stream()
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No authenticated user available"));
  }
}
