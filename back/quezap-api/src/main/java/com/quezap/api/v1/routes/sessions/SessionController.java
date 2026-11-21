package com.quezap.api.v1.routes.sessions;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.sessions.CreateSessionRequest;
import com.quezap.api.v1.dto.request.sessions.JoinSessionRequest;
import com.quezap.api.v1.dto.request.sessions.SubmitAnswerRequest;
import com.quezap.api.v1.dto.response.PageOfDto;
import com.quezap.api.v1.dto.response.sessions.AnswerSubmittedDto;
import com.quezap.api.v1.dto.response.sessions.JoinSessionResponseDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.NextQuestionResponseDto;
import com.quezap.api.v1.dto.response.sessions.SessionCreatedDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionStatusDto;
import com.quezap.api.v1.dto.response.sessions.SseTicketResponseDto;
import com.quezap.api.v1.dto.sessions.SessionPauseRequestDto;
import com.quezap.api.v1.dto.sessions.SessionPauseResponseDto;
import com.quezap.api.v1.dto.sessions.SessionSettingsRequestDto;
import com.quezap.api.v1.dto.sessions.SessionSettingsResponseDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.security.SessionEndpointRateLimiter;
import com.quezap.api.v1.services.SessionReportService;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.application.ports.sessions.CreateSession;
import com.quezap.application.ports.sessions.JoinSession;
import com.quezap.application.ports.sessions.KickSessionParticipant;
import com.quezap.application.ports.sessions.LeaveSession;
import com.quezap.application.ports.sessions.NextSessionQuestion;
import com.quezap.application.ports.sessions.RevealSessionQuestion;
import com.quezap.application.ports.sessions.StartSessionLifecycle;
import com.quezap.application.ports.sessions.SubmitSessionAnswer;
import com.quezap.application.ports.sessions.ToggleSessionPause;
import com.quezap.application.ports.sessions.UpdateSessionSettings;
import com.quezap.application.usecases.sessions.SessionQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/sessions")
public class SessionController {

  private final SessionCommandGateway commandGateway;
  private final SessionQueryService sessionQueryService;
  private final SessionReportService sessionReportService;
  private final SessionSseService sessionSseService;
  private final SessionSseRealtimePublisher realtimePublisher;
  private final PaginationMapper paginationMapper;
  private final SessionEndpointRateLimiter rateLimiter;

  public SessionController(
      SessionCommandGateway commandGateway,
      SessionQueryService sessionQueryService,
      SessionReportService sessionReportService,
      SessionSseService sessionSseService,
      SessionSseRealtimePublisher realtimePublisher,
      PaginationMapper paginationMapper) {
    this(
        commandGateway,
        sessionQueryService,
        sessionReportService,
        sessionSseService,
        realtimePublisher,
        paginationMapper,
        null);
  }

  @Autowired
  public SessionController(
      SessionCommandGateway commandGateway,
      SessionQueryService sessionQueryService,
      SessionReportService sessionReportService,
      SessionSseService sessionSseService,
      SessionSseRealtimePublisher realtimePublisher,
      PaginationMapper paginationMapper,
      @Nullable SessionEndpointRateLimiter rateLimiter) {
    this.commandGateway = commandGateway;
    this.sessionQueryService = sessionQueryService;
    this.sessionReportService = sessionReportService;
    this.sessionSseService = sessionSseService;
    this.realtimePublisher = realtimePublisher;
    this.paginationMapper = paginationMapper;
    this.rateLimiter = rateLimiter != null ? rateLimiter : new SessionEndpointRateLimiter();
  }

  @GetMapping
  public PageOfDto<SessionDetailDto> list(
      @RequestParam(required = false) @Nullable UUID authorId, PaginationDto paginationDto) {
    final var currentUserId = requireAuthenticatedUserId();
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var page =
        commandGateway.translateDomainError(
            () -> sessionQueryService.listSessions(currentUserId, authorId, pagination));
    return paginationMapper.fromDomain(page, SessionDtoMapper::toDetailDto);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SessionCreatedDto create(@Valid @RequestBody CreateSessionRequest request) {
    final var authorId = requireAuthenticatedUserId();
    final var out =
        commandGateway.create(
            new CreateSession.Input(authorId, request.name(), request.quezapId()));
    return new SessionCreatedDto(out.id(), out.number(), out.code());
  }

  public SessionDetailDto getById(@PathVariable UUID id) {
    return getById(id, null);
  }

  @GetMapping("{id}")
  public SessionDetailDto getById(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken) {
    assertHostOrParticipant(id, headerToken);
    final var view = commandGateway.translateDomainError(() -> sessionQueryService.getById(id));
    return SessionDtoMapper.toDetailDto(view);
  }

  public SessionDetailDto getByCode(@PathVariable String code) {
    return getByCode(code, null);
  }

  @GetMapping("by-code/{code}")
  public SessionDetailDto getByCode(
      @PathVariable String code, @Nullable HttpServletRequest httpRequest) {
    enforceRateLimit("by-code", httpRequest);
    final var view = commandGateway.translateDomainError(() -> sessionQueryService.getByCode(code));
    return SessionDtoMapper.toPublicDetailDto(view);
  }

  public JoinSessionResponseDto join(@Valid @RequestBody JoinSessionRequest request) {
    return join(request, null, null);
  }

  public JoinSessionResponseDto join(
      @Valid @RequestBody JoinSessionRequest request,
      @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken) {
    return join(request, headerToken, null);
  }

  @PostMapping("join")
  public JoinSessionResponseDto join(
      @Valid @RequestBody JoinSessionRequest request,
      @io.swagger.v3.oas.annotations.Parameter(hidden = true)
          @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken,
      @Nullable HttpServletRequest httpRequest) {
    enforceRateLimit("join", httpRequest);
    final var out =
        commandGateway.join(
            new JoinSession.Input(request.code(), request.participantName(), headerToken));
    return new JoinSessionResponseDto(
        out.sessionId(), out.participationToken(), out.participantName());
  }

  @PostMapping("{id}/start")
  public SessionStatusDto start(@PathVariable UUID id) {
    final var requesterId = requireAuthenticatedUserId();
    final var out = commandGateway.start(new StartSessionLifecycle.Input(id, requesterId));
    return new SessionStatusDto(out.status());
  }

  @PostMapping("{id}/next-question")
  public NextQuestionResponseDto nextQuestion(@PathVariable UUID id) {
    final var requesterId = requireAuthenticatedUserId();
    final var out = commandGateway.nextQuestion(new NextSessionQuestion.Input(id, requesterId));
    return new NextQuestionResponseDto(out.currentSlideIndex(), out.totalSlides());
  }

  @PostMapping("{id}/answers")
  @ResponseStatus(HttpStatus.CREATED)
  public AnswerSubmittedDto submitAnswer(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false) @Nullable String headerToken,
      @Valid @RequestBody SubmitAnswerRequest request) {
    final var out =
        commandGateway.submitAnswer(
            new SubmitSessionAnswer.Input(
                id,
                headerToken,
                request.slideIndex(),
                request.answerIndex(),
                request.textAnswer()));
    return new AnswerSubmittedDto(out.accepted(), out.receivedAt());
  }

  @PostMapping("{id}/reveal")
  public void reveal(@PathVariable UUID id) {
    final var requesterId = requireAuthenticatedUserId();
    commandGateway.reveal(new RevealSessionQuestion.Input(id, requesterId));
  }

  @PostMapping("{id}/pause")
  public SessionPauseResponseDto pauseSession(
      @PathVariable UUID id, @Valid @RequestBody SessionPauseRequestDto request) {
    final var requesterId = requireAuthenticatedUserId();
    final var out =
        commandGateway.togglePause(new ToggleSessionPause.Input(id, requesterId, request.paused()));
    return new SessionPauseResponseDto(out.sessionId(), out.isPaused());
  }

  @PutMapping("{id}/settings")
  public SessionSettingsResponseDto updateSettings(
      @PathVariable UUID id, @Valid @RequestBody SessionSettingsRequestDto request) {
    final var requesterId = requireAuthenticatedUserId();
    final var out =
        commandGateway.updateSettings(
            new UpdateSessionSettings.Input(
                id,
                requesterId,
                request.progressionMode(),
                request.explanationDurationSeconds(),
                request.mobileCorrectionEnabled()));
    return new SessionSettingsResponseDto(
        out.progressionMode(), out.explanationDurationSeconds(), out.mobileCorrectionEnabled());
  }

  @GetMapping(value = "{id}/export/csv", produces = "text/csv")
  public ResponseEntity<String> exportCsv(@PathVariable UUID id) {
    final var requesterId = requireAuthenticatedUserId();
    return commandGateway.translateDomainError(
        () -> sessionReportService.exportCsv(id, requesterId));
  }

  public static String escapeCsvCell(@Nullable String value) {
    return SessionReportService.escapeCsvCell(value);
  }

  public LeaderboardResponseDto getScores(@PathVariable UUID id) {
    return getScores(id, null);
  }

  @GetMapping("{id}/scores")
  public LeaderboardResponseDto getScores(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken) {
    assertHostOrParticipant(id, headerToken);
    return commandGateway.translateDomainError(() -> sessionReportService.buildScores(id));
  }

  @GetMapping("{id}/report")
  public SessionReportDto getReport(@PathVariable UUID id) {
    final var requesterId = requireAuthenticatedUserId();
    return commandGateway.translateDomainError(
        () -> sessionReportService.buildSessionReport(id, requesterId));
  }

  @PostMapping("{id}/participants/{name}/kick")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void kickParticipant(@PathVariable UUID id, @PathVariable String name) {
    final var requesterId = requireAuthenticatedUserId();
    commandGateway.kickParticipant(new KickSessionParticipant.Input(id, requesterId, name));
  }

  @PostMapping("{id}/leave")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leaveSession(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken) {
    commandGateway.leaveSession(new LeaveSession.Input(id, headerToken));
  }

  @PostMapping("{id}/events/ticket")
  public SseTicketResponseDto createEventsTicket(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false)
          @Nullable String headerToken) {
    assertHostOrParticipant(id, headerToken);
    final var ticket = sessionSseService.issueTicket(id);
    return new SseTicketResponseDto(ticket, SessionSseService.SSE_TICKET_TTL.toSeconds());
  }

  public SseEmitter streamEvents(
      @PathVariable UUID id,
      @RequestParam(value = "ticket", required = false) @Nullable String ticket,
      @RequestHeader(value = "Last-Event-ID", required = false) @Nullable String lastEventId) {
    return streamEvents(id, ticket, lastEventId, null);
  }

  @GetMapping(value = "{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(
      @PathVariable UUID id,
      @RequestParam(value = "ticket", required = false) @Nullable String ticket,
      @RequestHeader(value = "Last-Event-ID", required = false) @Nullable String lastEventId,
      @Nullable HttpServletRequest httpRequest) {
    if (!sessionSseService.consumeTicket(id, ticket)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid, expired, or missing SSE ticket");
    }
    final var initialData =
        commandGateway.translateDomainError(() -> sessionQueryService.getInitialStreamData(id));
    final var emitter =
        httpRequest != null
            ? sessionSseService.registerEmitter(
                id, SessionEndpointRateLimiter.resolveClientIp(httpRequest))
            : sessionSseService.registerEmitter(id);

    if (lastEventId != null && !lastEventId.isBlank()) {
      try {
        final var lastSeq = Long.parseLong(lastEventId.trim());
        final var missedEvents = sessionSseService.getEventsSince(id, lastSeq);
        if (!missedEvents.isEmpty()) {
          for (final var ev : missedEvents) {
            sessionSseService.sendStoredEventToEmitter(emitter, id, ev);
          }
          return emitter;
        }
      } catch (NumberFormatException _) {
        // Fall back to initial full state
      }
    }

    realtimePublisher.sendInitialState(emitter, id, initialData);
    return emitter;
  }

  private void enforceRateLimit(String endpointKey, @Nullable HttpServletRequest httpRequest) {
    if (httpRequest == null) {
      return;
    }
    final var clientIp = SessionEndpointRateLimiter.resolveClientIp(httpRequest);
    if (!rateLimiter.tryAcquire(endpointKey + ":" + clientIp)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Trop de requêtes. Veuillez réessayer plus tard.");
    }
  }

  private void assertHostOrParticipant(UUID sessionId, @Nullable String headerToken) {
    final var authenticatedUserId = resolveAuthenticatedUserIdOrNull();
    commandGateway.translateDomainError(
        () -> {
          sessionQueryService.assertHostOrParticipant(sessionId, authenticatedUserId, headerToken);
          return Boolean.TRUE;
        });
  }

  private @Nullable UUID resolveAuthenticatedUserIdOrNull() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return null;
    }
    final var principal = auth.getPrincipal().toString();
    if (principal.isBlank() || "anonymousUser".equals(principal)) {
      return null;
    }
    return commandGateway.translateDomainError(() -> sessionQueryService.resolveUserId(principal));
  }

  private UUID requireAuthenticatedUserId() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
    }
    final var principal = auth.getPrincipal().toString();
    if (principal.isBlank() || "anonymousUser".equals(principal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
    }
    return commandGateway.translateDomainError(() -> sessionQueryService.resolveUserId(principal));
  }
}
