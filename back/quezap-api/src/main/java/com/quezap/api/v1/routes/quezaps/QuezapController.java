package com.quezap.api.v1.routes.quezaps;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
import com.quezap.application.ports.quezaps.QuezapDataPort.AnswerInput;
import com.quezap.application.ports.quezaps.QuezapDataPort.InlineQuestionInput;
import com.quezap.application.ports.quezaps.QuezapDataPort.QuezapDetails;
import com.quezap.application.ports.quezaps.QuezapDataPort.SlideInput;
import com.quezap.application.usecases.quezaps.QuezapApplicationService;
import com.quezap.application.usecases.quezaps.QuezapApplicationService.QuezapBadRequestException;
import com.quezap.application.usecases.quezaps.QuezapApplicationService.QuezapForbiddenException;
import com.quezap.application.usecases.quezaps.QuezapApplicationService.QuezapNotFoundException;
import com.quezap.application.usecases.quezaps.QuezapApplicationService.QuezapUnauthorizedException;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager.InvalidPictureReferenceException;
import com.quezap.domain.ports.services.StagedPictureManager.PictureOwnershipException;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/quezaps")
public class QuezapController {

  private final QuezapApplicationService quezapService;
  private final PaginationMapper paginationMapper;

  public QuezapController(
      QuezapApplicationService quezapService, PaginationMapper paginationMapper) {
    this.quezapService = quezapService;
    this.paginationMapper = paginationMapper;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuezapIdDto create(@Valid @RequestBody CreateQuezapRequest request) {
    final var principal = currentPrincipal();
    final var isPublic = Objects.requireNonNullElse(request.isPublic(), Boolean.TRUE);
    final var slides = mapSlides(request.getEffectiveSlides());
    final var id =
        execute(
            () ->
                quezapService.create(
                    principal, request.title(), request.description(), isPublic, slides));
    return new QuezapIdDto(id);
  }

  @PostMapping("{id}/duplicate")
  @ResponseStatus(HttpStatus.CREATED)
  public QuezapIdDto duplicate(@PathVariable UUID id) {
    final var principal = currentPrincipal();
    return new QuezapIdDto(execute(() -> quezapService.duplicate(principal, id)));
  }

  @PostMapping("{id}/clone")
  @ResponseStatus(HttpStatus.CREATED)
  public QuezapIdDto clone(@PathVariable UUID id) {
    final var principal = currentPrincipal();
    return new QuezapIdDto(execute(() -> quezapService.cloneQuezap(principal, id)));
  }

  @GetMapping("{id}")
  public QuezapDetailDto getById(@PathVariable UUID id) {
    final var principal = currentPrincipal();
    final var details = execute(() -> quezapService.getById(principal, id));
    return toDetailDto(details);
  }

  @GetMapping
  public PageOfDto<QuezapSummaryDto> list(
      @RequestParam(required = false) @Nullable String search,
      @RequestParam(required = false) @Nullable UUID authorId,
      PaginationDto paginationDto) {
    final var principal = currentPrincipal();
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var page = execute(() -> quezapService.list(principal, search, authorId, pagination));

    final var items =
        page.items().stream()
            .map(
                q ->
                    new QuezapSummaryDto(
                        q.id(),
                        q.title(),
                        q.description(),
                        q.isPublic(),
                        q.authorId(),
                        q.questionCount(),
                        q.createdAt()))
            .toList();

    return new PageOfDto<>(
        items,
        page.totalItems(),
        page.totalPages(),
        page.currentPage(),
        page.pageSize(),
        page.hasNextPage(),
        page.hasPreviousPage());
  }

  @PutMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable UUID id, @Valid @RequestBody UpdateQuezapRequest request) {
    final var principal = currentPrincipal();
    final var slides = request.slides() != null ? mapSlides(request.slides()) : null;
    executeVoid(
        () ->
            quezapService.update(
                principal, id, request.title(), request.description(), request.isPublic(), slides));
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    final var principal = currentPrincipal();
    executeVoid(() -> quezapService.delete(principal, id));
  }

  @PostMapping("{id}/questions")
  @ResponseStatus(HttpStatus.CREATED)
  public void addQuestion(@PathVariable UUID id, @Valid @RequestBody QuezapSlideRequest request) {
    final var principal = currentPrincipal();
    final var slide = mapSlide(request);
    executeVoid(() -> quezapService.addQuestion(principal, id, slide));
  }

  @DeleteMapping("{id}/questions/{questionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeQuestion(@PathVariable UUID id, @PathVariable UUID questionId) {
    final var principal = currentPrincipal();
    executeVoid(() -> quezapService.removeQuestion(principal, id, questionId));
  }

  public static @Nullable String toPublicPictureUrl(@Nullable String pictureFilename) {
    return StagedPictureManager.normalizePublicUrl(pictureFilename);
  }

  private List<SlideInput> mapSlides(List<QuezapSlideRequest> requests) {
    return requests.stream().map(this::mapSlide).toList();
  }

  private SlideInput mapSlide(QuezapSlideRequest slideReq) {
    final var timer = slideReq.getEffectiveTimer();
    final var points = Objects.requireNonNullElse(slideReq.points(), 1000);
    final var inlineReq = slideReq.question();
    if (inlineReq == null) {
      return new SlideInput(slideReq.questionId(), timer, points, null);
    }

    final var answers =
        inlineReq.answers() != null
            ? inlineReq.answers().stream()
                .map(
                    a ->
                        new AnswerInput(
                            a.id(),
                            a.label(),
                            Objects.requireNonNullElse(a.isCorrect(), Boolean.FALSE),
                            a.pictureFilename(),
                            a.answerOrder()))
                .toList()
            : null;
    final var inlineQuestion =
        new InlineQuestionInput(
            inlineReq.id(),
            inlineReq.label(),
            inlineReq.type(),
            inlineReq.pictureFilename(),
            inlineReq.themeId(),
            inlineReq.themeName(),
            answers);
    return new SlideInput(slideReq.questionId(), timer, points, inlineQuestion);
  }

  private QuezapDetailDto toDetailDto(QuezapDetails details) {
    final var authorDto = new UserSummaryDto(details.author().id(), details.author().name());
    final var slideDtos =
        details.slides().stream()
            .map(
                s -> {
                  final var q = s.question();
                  final var themeDto = new ThemeDetailDto(q.theme().id(), q.theme().name());
                  final var answerDtos =
                      q.answers().stream()
                          .map(
                              a ->
                                  new AnswerDetailDto(
                                      a.id(), a.label(), a.isCorrect(), a.pictureFilename()))
                          .toList();
                  final var questionDto =
                      new QuestionDetailDto(
                          q.id(), q.type(), q.label(), q.pictureFilename(), themeDto, answerDtos);
                  return new QuezapSlideDetailDto(
                      s.questionOrder(), s.timerSeconds(), s.points(), questionDto);
                })
            .toList();

    return new QuezapDetailDto(
        details.id(),
        details.title(),
        details.description(),
        details.isPublic(),
        authorDto,
        slideDtos,
        details.createdAt(),
        details.updatedAt());
  }

  private @Nullable String currentPrincipal() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null && auth.getPrincipal() != null) ? auth.getPrincipal().toString() : null;
  }

  private void executeVoid(Runnable action) {
    execute(
        () -> {
          action.run();
          return null;
        });
  }

  private <T> T execute(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (QuezapUnauthorizedException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
    } catch (QuezapForbiddenException | PictureOwnershipException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (QuezapNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (QuezapBadRequestException | InvalidPictureReferenceException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }
}
