package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.IntermediateLeaderboardPayload;
import com.quezap.api.v1.dto.events.QuestionRevealPayload;
import com.quezap.api.v1.dto.events.QuestionStartPayload;
import com.quezap.api.v1.dto.events.SessionEndedPayload;
import com.quezap.api.v1.dto.events.SessionPausePayload;
import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.sessions.CreateSessionRequest;
import com.quezap.api.v1.dto.request.sessions.JoinSessionRequest;
import com.quezap.api.v1.dto.request.sessions.SubmitAnswerRequest;
import com.quezap.api.v1.dto.sessions.SessionPauseRequestDto;
import com.quezap.api.v1.dto.sessions.SessionSettingsRequestDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.sessions.SessionCommandGateway;
import com.quezap.api.v1.routes.sessions.SessionController;
import com.quezap.api.v1.routes.sessions.SessionSseRealtimePublisher;
import com.quezap.api.v1.security.SessionEndpointRateLimiter;
import com.quezap.api.v1.services.SessionReportService;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.SessionStorePort.SessionQuezapSnapshot;
import com.quezap.application.ports.sessions.SessionStorePort.SessionSummary;
import com.quezap.application.usecases.sessions.CreateSessionHandler;
import com.quezap.application.usecases.sessions.JoinSessionHandler;
import com.quezap.application.usecases.sessions.KickSessionParticipantHandler;
import com.quezap.application.usecases.sessions.LeaveSessionHandler;
import com.quezap.application.usecases.sessions.NextSessionQuestionHandler;
import com.quezap.application.usecases.sessions.RevealSessionQuestionHandler;
import com.quezap.application.usecases.sessions.SessionQueryService;
import com.quezap.application.usecases.sessions.SessionReportApplicationService;
import com.quezap.application.usecases.sessions.StartSessionLifecycleHandler;
import com.quezap.application.usecases.sessions.SubmitSessionAnswerHandler;
import com.quezap.application.usecases.sessions.ToggleSessionPauseHandler;
import com.quezap.application.usecases.sessions.UpdateSessionSettingsHandler;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideOption;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SessionControllerTest {

  private InMemorySessionStore sessionStore;
  private SessionSseService sessionSseService;
  private UserNameSanitizer userNameSanitizer;
  private SessionController sessionController;
  private final UUID authorId = UuidV7.randomUuid();

  @BeforeEach
  void setUp() {
    sessionStore = new InMemorySessionStore();
    sessionStore.registerUser(authorId, "HostUser");

    sessionSseService = Mockito.mock(SessionSseService.class);
    userNameSanitizer = Mockito.mock(UserNameSanitizer.class);
    when(userNameSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

    final var publisher = new SessionSseRealtimePublisher(sessionSseService);
    final var executor = new DirectUsecaseExecutor();
    final var commandGateway =
        new SessionCommandGateway(
            executor,
            new CreateSessionHandler(sessionStore),
            new JoinSessionHandler(sessionStore, userNameSanitizer, publisher),
            new StartSessionLifecycleHandler(sessionStore, publisher),
            new NextSessionQuestionHandler(sessionStore, publisher),
            new SubmitSessionAnswerHandler(sessionStore, publisher),
            new RevealSessionQuestionHandler(sessionStore, publisher),
            new ToggleSessionPauseHandler(sessionStore, publisher),
            new UpdateSessionSettingsHandler(sessionStore),
            new KickSessionParticipantHandler(sessionStore, publisher),
            new LeaveSessionHandler(sessionStore, publisher));

    final var queryService = new SessionQueryService(sessionStore);
    final var reportService =
        new SessionReportService(new SessionReportApplicationService(sessionStore));

    sessionController =
        new SessionController(
            commandGateway,
            queryService,
            reportService,
            sessionSseService,
            publisher,
            new PaginationMapper());

    authenticateAs(authorId);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static void authenticateAs(UUID userId) {
    final var auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private static SessionSlideSpec quizzSlide(
      int order, String label, int timerSeconds, int points, List<SessionSlideOption> options) {
    return new SessionSlideSpec(
        order, UuidV7.randomUuid(), "QUIZZ", label, null, timerSeconds, points, options);
  }

  private static SessionSlideSpec booleanSlide(
      int order, String label, boolean statementIsTrue, int timerSeconds, int points) {
    return new SessionSlideSpec(
        order,
        UuidV7.randomUuid(),
        "BOOLEAN",
        label,
        null,
        timerSeconds,
        points,
        List.of(
            new SessionSlideOption(0, statementIsTrue ? "True" : "False", null, statementIsTrue)));
  }

  private static SessionSlideSpec cashSlide(
      int order, String label, List<String> accepted, int timerSeconds, int points) {
    final var options = new ArrayList<SessionSlideOption>();
    for (var i = 0; i < accepted.size(); i++) {
      options.add(new SessionSlideOption(i, accepted.get(i), null, true));
    }
    return new SessionSlideSpec(
        order, UuidV7.randomUuid(), "CASH", label, null, timerSeconds, points, options);
  }

  private Session createStoredSession(
      UUID sessionId,
      long number,
      String name,
      int currentSlideIndex,
      @Nullable Instant startedAt,
      @Nullable Instant currentSlideStartedAt,
      @Nullable Instant pausedAt,
      @Nullable Instant endedAt,
      boolean isPaused,
      @Nullable Integer lastRevealedSlideIndex,
      List<SessionSlideSpec> slides,
      List<SessionParticipant> participants,
      List<SessionAnswerRecord> answers) {
    final var session =
        Session.hydrateLive(
            new SessionId(sessionId),
            name,
            new SessionNumber(number),
            currentSlideIndex,
            new UserId(authorId),
            slides.isEmpty() ? null : UuidV7.randomUuid(),
            slides.isEmpty() ? null : name,
            ProgressionMode.MANUAL,
            isPaused,
            15,
            false,
            startedAt != null ? TimelinePoint.at(startedAt) : null,
            currentSlideStartedAt != null ? TimelinePoint.at(currentSlideStartedAt) : null,
            pausedAt != null ? TimelinePoint.at(pausedAt) : null,
            endedAt != null ? TimelinePoint.at(endedAt) : null,
            lastRevealedSlideIndex,
            TimelinePoint.at(Instant.now()),
            slides,
            participants,
            answers);
    sessionStore.saveSession(session);
    return session;
  }

  @Test
  void shouldCreateSession() {
    final var quezapId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(
            1,
            "Speed of light?",
            20,
            100,
            List.of(
                new SessionSlideOption(0, "300,000 km/s", null, true),
                new SessionSlideOption(1, "150,000 km/s", null, false)));
    sessionStore.registerQuezap(
        new SessionQuezapSnapshot(quezapId, "Physics Finals", authorId, true, List.of(slide)));

    final var response =
        sessionController.create(new CreateSessionRequest("Physics Finals", quezapId));

    assertThat(response.id()).isNotNull();
    assertThat(response.code()).matches("\\d+");
    assertThat(sessionStore.findSessionById(response.id())).isPresent();
  }

  @Test
  void shouldJoinSessionAndBroadcastLobbyState() {
    final var sessionId = UuidV7.randomUuid();
    createStoredSession(
        sessionId,
        1042L,
        "Physics Finals",
        0,
        null,
        null,
        null,
        null,
        false,
        0,
        List.of(),
        List.of(),
        List.of());

    final var response = sessionController.join(new JoinSessionRequest("QZ-1042", "Arthur"));

    assertThat(response.sessionId()).isEqualTo(sessionId);
    assertThat(response.participantName()).isEqualTo("Arthur");
    assertThat(response.participationToken()).startsWith("ptk_");
    verify(sessionSseService).broadcast(eq(sessionId), eq("LOBBY_STATE"), eq(1042L), any());
  }

  @Test
  void shouldStartSessionAndBroadcastSessionStartedAndQuestionStart() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(
            1,
            "Speed of light?",
            15,
            100,
            List.of(
                new SessionSlideOption(0, "300,000 km/s", null, true),
                new SessionSlideOption(1, "150,000 km/s", null, false)));
    createStoredSession(
        sessionId,
        1042L,
        "Physics Finals",
        0,
        null,
        null,
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(),
        List.of());

    final var response = sessionController.start(sessionId);

    assertThat(response.status()).isEqualTo("STARTED");
    verify(sessionSseService).broadcast(eq(sessionId), eq("SESSION_STARTED"), eq(1042L), any());

    final var captor = ArgumentCaptor.forClass(QuestionStartPayload.class);
    verify(sessionSseService)
        .broadcast(eq(sessionId), eq("QUESTION_START"), eq(1042L), captor.capture());
    assertThat(captor.getValue().timerSeconds()).isEqualTo(15);
    assertThat(captor.getValue().remainingSeconds()).isEqualTo(15);
    assertThat(captor.getValue().deadline()).isNotNull();
  }

  @Test
  void shouldAdvanceToNextQuestionAndBroadcastQuestionStart() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(
            1,
            "Speed of light?",
            20,
            100,
            List.of(new SessionSlideOption(0, "300,000 km/s", null, true)));
    createStoredSession(
        sessionId,
        1042L,
        "Physics Finals",
        0,
        null,
        null,
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(),
        List.of());

    final var response = sessionController.nextQuestion(sessionId);

    assertThat(response.currentSlideIndex()).isEqualTo(1);
    assertThat(response.totalSlides()).isEqualTo(1);
    verify(sessionSseService).broadcast(eq(sessionId), eq("QUESTION_START"), eq(1042L), any());
  }

  @Test
  void shouldSubmitAnswerAndCalculateScoreWithSpeedBonus() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(
            1,
            "What is 5x5?",
            20,
            1000,
            List.of(
                new SessionSlideOption(0, "25", null, true),
                new SessionSlideOption(1, "10", null, false)));
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(), "Speedy", 0, "ptk_speed", false, Instant.now(), 0L, 0, 0L);
    createStoredSession(
        sessionId,
        1042L,
        "Speed Quiz",
        1,
        Instant.now().minusSeconds(10),
        Instant.now().minusSeconds(10),
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(participant),
        List.of());

    final var response =
        sessionController.submitAnswer(sessionId, "ptk_speed", new SubmitAnswerRequest(1, 0));

    assertThat(response.accepted()).isTrue();
    final var updated = sessionStore.findParticipantByToken(sessionId, "ptk_speed").orElseThrow();
    assertThat(updated.score()).isBetween(740, 760);
    assertThat(updated.totalResponseTimeMs()).isGreaterThan(0L);
  }

  @Test
  void shouldRejectAnswerSubmissionWhilePausedWithConflict() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "2+2=?", 20, 1000, List.of(new SessionSlideOption(0, "4", null, true)));
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(), "PausedPlayer", 0, "ptk_paused", false, Instant.now(), 0L, 0, 0L);
    createStoredSession(
        sessionId,
        1042L,
        "Paused Quiz",
        1,
        Instant.now().minusSeconds(5),
        Instant.now().minusSeconds(5),
        Instant.now().minusSeconds(2),
        null,
        true,
        0,
        List.of(slide),
        List.of(participant),
        List.of());

    assertThatThrownBy(
            () ->
                sessionController.submitAnswer(
                    sessionId, "ptk_paused", new SubmitAnswerRequest(1, 0)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
  }

  @Test
  void shouldRejectAnswerSubmissionPostRevealWithConflict() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "2+2=?", 20, 1000, List.of(new SessionSlideOption(0, "4", null, true)));
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(), "LatePlayer", 0, "ptk_late", false, Instant.now(), 0L, 0, 0L);
    createStoredSession(
        sessionId,
        1042L,
        "Math Quiz",
        1,
        Instant.now().minusSeconds(5),
        Instant.now().minusSeconds(5),
        null,
        null,
        false,
        1,
        List.of(slide),
        List.of(participant),
        List.of());

    assertThatThrownBy(
            () ->
                sessionController.submitAnswer(
                    sessionId, "ptk_late", new SubmitAnswerRequest(1, 0)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
  }

  @Test
  void shouldRejectAnswerSubmissionAfterTimerExpiryWithBadRequest() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "2+2=?", 10, 1000, List.of(new SessionSlideOption(0, "4", null, true)));
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(),
            "TooLatePlayer",
            0,
            "ptk_toolate",
            false,
            Instant.now(),
            0L,
            0,
            0L);
    createStoredSession(
        sessionId,
        1042L,
        "Math Quiz",
        1,
        Instant.now().minusSeconds(20),
        Instant.now().minusSeconds(20),
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(participant),
        List.of());

    assertThatThrownBy(
            () ->
                sessionController.submitAnswer(
                    sessionId, "ptk_toolate", new SubmitAnswerRequest(1, 0)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldSubmitBooleanAndCashAnswersAndAllowModification() {
    final var sessionId = UuidV7.randomUuid();
    final var cash = cashSlide(1, "Quelle fleur ?", List.of("Tulipe", "Tulipa"), 30, 1000);
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(), "Botaniste", 0, "ptk_cash", false, Instant.now(), 0L, 0, 0L);
    createStoredSession(
        sessionId,
        1043L,
        "Botanique Quiz",
        1,
        Instant.now().minusSeconds(2),
        Instant.now().minusSeconds(2),
        null,
        null,
        false,
        0,
        List.of(cash),
        List.of(participant),
        List.of());

    final var correctResp =
        sessionController.submitAnswer(
            sessionId, "ptk_cash", new SubmitAnswerRequest(1, null, "tuliipe 🌷"));
    assertThat(correctResp.accepted()).isTrue();
    assertThat(sessionStore.findParticipantByToken(sessionId, "ptk_cash").orElseThrow().score())
        .isGreaterThan(0);

    // Modify to wrong answer
    sessionController.submitAnswer(sessionId, "ptk_cash", new SubmitAnswerRequest(1, null, "rose"));
    assertThat(sessionStore.findParticipantByToken(sessionId, "ptk_cash").orElseThrow().score())
        .isZero();

    // Modify to 2nd accepted variant and reveal
    sessionController.submitAnswer(
        sessionId, "ptk_cash", new SubmitAnswerRequest(1, null, "Tulipa"));
    sessionController.reveal(sessionId);

    final var revealCaptor = ArgumentCaptor.forClass(QuestionRevealPayload.class);
    verify(sessionSseService)
        .broadcast(eq(sessionId), eq("QUESTION_REVEAL"), eq(1043L), revealCaptor.capture());
    assertThat(revealCaptor.getValue().acceptedAnswers()).containsExactly("Tulipe", "Tulipa");
    assertThat(revealCaptor.getValue().answersStats().get(0).count()).isZero();
    assertThat(revealCaptor.getValue().answersStats().get(1).count()).isEqualTo(1);
  }

  @Test
  void shouldRequireAuthenticationAndRestrictListToCurrentUser() {
    final var otherUserId = UuidV7.randomUuid();
    sessionStore.registerUser(otherUserId, "OtherTeacher");
    createStoredSession(
        UuidV7.randomUuid(),
        1111L,
        "Host Session",
        0,
        null,
        null,
        null,
        null,
        false,
        0,
        List.of(),
        List.of(),
        List.of());

    // Anonymous / unauthenticated -> 401
    SecurityContextHolder.clearContext();
    assertThatThrownBy(
            () -> sessionController.list(null, new PaginationDto(1L, 10L, null, null, null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNAUTHORIZED);

    // Non-UUID / anonymousUser principal -> 401 (never findAllSessions!)
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
    assertThatThrownBy(
            () -> sessionController.list(null, new PaginationDto(1L, 10L, null, null, null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNAUTHORIZED);

    // Asking for another teacher's sessions -> 403
    authenticateAs(authorId);
    assertThatThrownBy(
            () ->
                sessionController.list(
                    otherUserId, new PaginationDto(1L, 10L, null, null, null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

    // Own sessions -> 200 with only current user's sessions
    final var page =
        sessionController.list(null, new PaginationDto(1L, 10L, null, null, null, null));
    assertThat(page.data()).hasSize(1);
    assertThat(page.data().get(0).authorId()).isEqualTo(authorId);
  }

  @Test
  void shouldPersistPausedAtAndShiftSlideStartOnResume() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "1+1=?", 10, 1000, List.of(new SessionSlideOption(0, "2", null, true)));
    final var slideStart = Instant.now().minusSeconds(5);
    createStoredSession(
        sessionId,
        1045L,
        "Pause Quiz",
        1,
        slideStart,
        slideStart,
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(),
        List.of());

    final var pauseResp =
        sessionController.pauseSession(sessionId, new SessionPauseRequestDto(true));
    assertThat(pauseResp.isPaused()).isTrue();
    final var pausedSession = sessionStore.findSessionById(sessionId).orElseThrow();
    assertThat(pausedSession.getPausedAt()).isNotNull();

    final var resumeResp =
        sessionController.pauseSession(sessionId, new SessionPauseRequestDto(false));
    assertThat(resumeResp.isPaused()).isFalse();
    final var resumedSession = sessionStore.findSessionById(sessionId).orElseThrow();
    assertThat(resumedSession.getPausedAt()).isNull();
    assertThat(Objects.requireNonNull(resumedSession.getCurrentSlideStartedAt()).position())
        .isAfterOrEqualTo(slideStart);
    verify(sessionSseService, Mockito.times(2))
        .broadcast(
            eq(sessionId), eq("SESSION_PAUSE_STATUS"), eq(1045L), any(SessionPausePayload.class));
  }

  @Test
  void shouldUpdateSettingsKickLeaveAndHandleEndedSessions() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "Q1", 30, 100, List.of(new SessionSlideOption(0, "A", null, true)));
    final var p1 =
        new SessionParticipant(
            UuidV7.randomUuid(), "Alice", 100, "ptk_alice", false, Instant.now(), 1000L, 1, 1000L);
    final var p2 =
        new SessionParticipant(
            UuidV7.randomUuid(), "Bob", 100, "ptk_bob", false, Instant.now(), 2000L, 1, 2000L);
    createStoredSession(
        sessionId,
        5555L,
        "Active Session",
        1,
        Instant.now().minusSeconds(10),
        Instant.now().minusSeconds(10),
        null,
        null,
        false,
        0,
        List.of(slide),
        List.of(p1, p2),
        List.of(
            new SessionAnswerRecord(
                UuidV7.randomUuid(), "Alice", 1, 0, null, 100, true, Instant.now())));

    final var settings =
        sessionController.updateSettings(
            sessionId, new SessionSettingsRequestDto("AUTOMATIC", 20, true));
    assertThat(settings.progressionMode()).isEqualTo("AUTOMATIC");
    assertThat(settings.explanationDurationSeconds()).isEqualTo(20);
    assertThat(settings.mobileCorrectionEnabled()).isTrue();

    sessionController.kickParticipant(sessionId, "Bob");
    verify(sessionSseService).broadcast(eq(sessionId), eq("PARTICIPANT_KICKED"), eq(5555L), any());

    sessionController.leaveSession(sessionId, "ptk_alice");
    assertThat(sessionStore.findSessionById(sessionId).orElseThrow().getSessionParticipants())
        .hasSize(1);
    assertThat(sessionStore.findSessionById(sessionId).orElseThrow().getSessionAnswers()).isEmpty();
  }

  @Test
  void shouldIncludeGroupMetricsTop5AndDeterministicTieBreakWhenSessionEnds() {
    final var sessionId = UuidV7.randomUuid();
    final var slide =
        quizzSlide(1, "Q1", 30, 100, List.of(new SessionSlideOption(0, "A", null, true)));
    final var now = Instant.now();
    final var participants = new ArrayList<SessionParticipant>();
    final var answers = new ArrayList<SessionAnswerRecord>();
    for (var i = 1; i <= 6; i++) {
      participants.add(
          new SessionParticipant(
              UuidV7.randomUuid(),
              "Student" + i,
              i * 100,
              "token" + i,
              false,
              now.plusSeconds(i),
              i * 500L,
              1,
              i * 500L));
      answers.add(
          new SessionAnswerRecord(
              UuidV7.randomUuid(),
              "Student" + i,
              1,
              0,
              null,
              i % 2 == 0 ? 100 : 0,
              i % 2 == 0,
              now));
    }
    createStoredSession(
        sessionId,
        1234L,
        "Session Ending Test",
        1,
        now,
        now,
        null,
        null,
        false,
        0,
        List.of(slide),
        participants,
        answers);

    sessionController.nextQuestion(sessionId);

    final var captor = ArgumentCaptor.forClass(SessionEndedPayload.class);
    verify(sessionSseService)
        .broadcast(eq(sessionId), eq("SESSION_ENDED"), eq(1234L), captor.capture());
    final var payload = captor.getValue();
    assertThat(payload.finalLeaderboard()).hasSize(6);
    assertThat(Objects.requireNonNull(payload.top5())).hasSize(5);
    assertThat(Objects.requireNonNull(payload.metrics()).averageScore()).isEqualTo(350);
    assertThat(Objects.requireNonNull(payload.metrics()).globalSuccessRate()).isEqualTo(50.0);
  }

  @Test
  void shouldHandleSessionWithDeletedQuezapAndExportCsvWithBom() {
    final var sessionId = UuidV7.randomUuid();
    final var now = Instant.now();
    createStoredSession(
        sessionId,
        8888L,
        "Orphaned Quezap Session",
        1,
        now.minusSeconds(60),
        now.minusSeconds(60),
        null,
        now,
        false,
        1,
        List.of(),
        List.of(
            new SessionParticipant(
                UuidV7.randomUuid(), "=cmd|calc", 100, "ptk_hack", false, now, 1000L, 1, 1000L)),
        List.of());

    final var detail = sessionController.getById(sessionId);
    assertThat(detail.totalSlides()).isZero();

    final var csv = sessionController.exportCsv(sessionId);
    assertThat(csv.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(csv.getHeaders().getContentType())
        .isEqualTo(new MediaType("text", "csv", StandardCharsets.UTF_8));
    assertThat(csv.getBody()).startsWith("\uFEFF");
    assertThat(csv.getBody()).contains("\"'=cmd|calc\"");
  }

  @Test
  void shouldMaintainStrictSequenceOrderUnderConcurrencyAndPurgeExpiredSseSessions()
      throws Exception {
    final var baseTime = Instant.parse("2026-11-18T12:00:00Z");
    final var sseService = new SessionSseService(Clock.fixed(baseTime, ZoneOffset.UTC));
    final var sessionId = UuidV7.randomUuid();

    final var threads = 8;
    final var perThread = 20;
    final var pool = Executors.newFixedThreadPool(threads);
    final var latch = new CountDownLatch(threads);
    for (var t = 0; t < threads; t++) {
      final var threadIdx = t;
      pool.execute(
          () -> {
            try {
              for (var i = 0; i < perThread; i++) {
                sseService.broadcast(
                    sessionId, "ANSWER_RECEIVED", 1234L, Map.of("t", threadIdx, "i", i));
              }
            } finally {
              latch.countDown();
            }
          });
    }
    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    pool.shutdownNow();

    final var events = sseService.getEventsSince(sessionId, 0L);
    assertThat(events).hasSize(50);
    for (var i = 1; i < events.size(); i++) {
      assertThat(events.get(i).sequence()).isEqualTo(events.get(i - 1).sequence() + 1);
    }
    assertThat(events.get(events.size() - 1).sequence()).isEqualTo((long) threads * perThread);

    // End session and verify TTL purge removes state
    sseService.broadcast(sessionId, "SESSION_ENDED", 1234L, Map.of("ended", true));
    assertThat(sseService.hasSessionState(sessionId)).isTrue();
    sseService.purgeExpiredSessions(baseTime.plusSeconds(600));
    assertThat(sseService.hasSessionState(sessionId)).isFalse();
    assertThat(sseService.getEventsSince(sessionId, 0L)).isEmpty();
  }

  @Test
  void shouldReplayMissedEventsOrInitialStateOnStreamEvents() {
    final var sessionId = UuidV7.randomUuid();
    final var slide = booleanSlide(1, "The sky is green", false, 20, 1000);
    createStoredSession(
        sessionId,
        1234L,
        "Stream Replay",
        1,
        Instant.now().minusSeconds(5),
        Instant.now().minusSeconds(5),
        null,
        null,
        false,
        1,
        List.of(slide),
        List.of(),
        List.of());
    when(sessionSseService.registerEmitter(sessionId)).thenReturn(new SseEmitter());
    when(sessionSseService.consumeTicket(sessionId, "sset_valid")).thenReturn(true);

    final var storedEvent =
        new SessionSseService.StoredEvent(5L, "QUESTION_START", 1234L, Map.of("count", 1));
    when(sessionSseService.getEventsSince(sessionId, 4L)).thenReturn(List.of(storedEvent));

    sessionController.streamEvents(sessionId, "sset_valid", "4");
    verify(sessionSseService).sendStoredEventToEmitter(any(), eq(sessionId), eq(storedEvent));

    sessionController.streamEvents(sessionId, "sset_valid", null);
    verify(sessionSseService)
        .sendToEmitter(any(), eq("QUESTION_REVEAL"), eq(1234L), eq(sessionId), any());
    verify(sessionSseService)
        .sendToEmitter(
            any(),
            eq("INTERMEDIATE_LEADERBOARD"),
            eq(1234L),
            eq(sessionId),
            any(IntermediateLeaderboardPayload.class));
  }

  @Test
  void shouldRateLimitGetByCodeAndJoinPerIp() {
    final var quezapId = UuidV7.randomUuid();
    sessionStore.registerQuezap(
        new SessionQuezapSnapshot(
            quezapId,
            "Quiz Rate Limit",
            authorId,
            true,
            List.of(
                new SessionSlideSpec(
                    1,
                    UuidV7.randomUuid(),
                    "QUIZZ",
                    "Q1",
                    null,
                    20,
                    1000,
                    List.of(
                        new SessionSlideOption(0, "Yes", null, true),
                        new SessionSlideOption(1, "No", null, false))))));
    final var created =
        sessionController.create(new CreateSessionRequest("Rate Limit Session", quezapId));

    final var ip1Request = new MockHttpServletRequest();
    ip1Request.setRemoteAddr("198.51.100.10");

    for (var i = 0; i < SessionEndpointRateLimiter.DEFAULT_MAX_REQUESTS; i++) {
      assertThat(sessionController.getByCode(created.code(), ip1Request).id())
          .isEqualTo(created.id());
    }

    assertThatThrownBy(() -> sessionController.getByCode(created.code(), ip1Request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

    final var ip2Request = new MockHttpServletRequest();
    ip2Request.setRemoteAddr("198.51.100.11");
    assertThat(sessionController.getByCode(created.code(), ip2Request).id())
        .isEqualTo(created.id());

    for (var i = 0; i < SessionEndpointRateLimiter.DEFAULT_MAX_REQUESTS; i++) {
      sessionController.join(new JoinSessionRequest(created.code(), "User" + i), null, ip1Request);
    }

    assertThatThrownBy(
            () ->
                sessionController.join(
                    new JoinSessionRequest(created.code(), "UserOverflow"), null, ip1Request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
  }

  @Test
  void shouldMaskUnrevealedAnswerScoresOnGetScoresUntilRevealed() {
    final var quezapId = UuidV7.randomUuid();
    sessionStore.registerQuezap(
        new SessionQuezapSnapshot(
            quezapId,
            "Quiz Oracle",
            authorId,
            true,
            List.of(
                new SessionSlideSpec(
                    1,
                    UuidV7.randomUuid(),
                    "QUIZZ",
                    "Q1",
                    null,
                    20,
                    1000,
                    List.of(
                        new SessionSlideOption(0, "Correct", null, true),
                        new SessionSlideOption(1, "Wrong", null, false))))));
    final var created = sessionController.create(new CreateSessionRequest("Oracle", quezapId));
    final var joinRes = sessionController.join(new JoinSessionRequest(created.code(), "Cheater"));
    sessionController.start(created.id());

    sessionController.submitAnswer(
        created.id(), joinRes.participationToken(), new SubmitAnswerRequest(1, 0, null));

    final var beforeReveal = sessionController.getScores(created.id());
    assertThat(beforeReveal.leaderboard()).hasSize(1);
    assertThat(beforeReveal.leaderboard().get(0).score()).isZero();
    assertThat(beforeReveal.leaderboard().get(0).delta()).isZero();

    sessionController.reveal(created.id());
    final var afterReveal = sessionController.getScores(created.id());
    assertThat(afterReveal.leaderboard()).hasSize(1);
    assertThat(afterReveal.leaderboard().get(0).score()).isPositive();
    assertThat(afterReveal.leaderboard().get(0).delta()).isPositive();
  }

  @Test
  void shouldRequireAuthForGetByIdGetScoresAndSseTicketAndStripNamesFromByCode() {
    final var quezapId = UuidV7.randomUuid();
    sessionStore.registerQuezap(
        new SessionQuezapSnapshot(
            quezapId,
            "Quiz Auth",
            authorId,
            true,
            List.of(
                new SessionSlideSpec(
                    1,
                    UuidV7.randomUuid(),
                    "QUIZZ",
                    "Q1",
                    null,
                    20,
                    1000,
                    List.of(
                        new SessionSlideOption(0, "Correct", null, true),
                        new SessionSlideOption(1, "Wrong", null, false))))));
    final var created =
        sessionController.create(new CreateSessionRequest("Auth Session", quezapId));
    final var joinRes =
        sessionController.join(new JoinSessionRequest(created.code(), "HiddenStudent"));

    // Unauthenticated by-code strips participant names but preserves count
    SecurityContextHolder.clearContext();
    final var publicByCode = sessionController.getByCode(created.code());
    assertThat(publicByCode.participantsCount()).isEqualTo(1);
    assertThat(publicByCode.participants()).isEmpty();

    // Unauthenticated without token -> 401 on getById, getScores, createEventsTicket, streamEvents
    assertThatThrownBy(() -> sessionController.getById(created.id(), null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED));
    assertThatThrownBy(() -> sessionController.getScores(created.id(), null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED));
    assertThatThrownBy(() -> sessionController.createEventsTicket(created.id(), null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED));
    assertThatThrownBy(() -> sessionController.streamEvents(created.id(), null, null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED));

    // Valid participant token -> authorized
    when(sessionSseService.issueTicket(created.id())).thenReturn("sset_student_ticket");
    assertThat(sessionController.getById(created.id(), joinRes.participationToken()).participants())
        .hasSize(1);
    assertThat(sessionController.getScores(created.id(), joinRes.participationToken()).sessionId())
        .isEqualTo(created.id());
    final var ticketDto =
        sessionController.createEventsTicket(created.id(), joinRes.participationToken());
    assertThat(ticketDto.ticket()).isEqualTo("sset_student_ticket");
    assertThat(ticketDto.expiresInSeconds()).isEqualTo(60L);

    // Another authenticated user (not host, no participant token) -> 403 Forbidden
    final var otherUserId = UuidV7.randomUuid();
    sessionStore.registerUser(otherUserId, "OtherUser");
    authenticateAs(otherUserId);
    assertThatThrownBy(() -> sessionController.getById(created.id(), null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN));
    assertThatThrownBy(() -> sessionController.createEventsTicket(created.id(), null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN));
  }

  private static final class DirectUsecaseExecutor implements UsecaseExecutor {
    @Override
    public <I extends UsecaseInput, O extends UsecaseOutput> O execute(
        UsecaseHandler<I, O> handler, I input) {
      final var onCommitHooks = new ArrayList<Runnable>();
      final var onRollbackHooks = new ArrayList<Runnable>();
      final var uow =
          new UnitOfWorkEvents() {
            @Override
            public void onRollback(Runnable action) {
              onRollbackHooks.add(action);
            }

            @Override
            public void onCommit(Runnable action) {
              onCommitHooks.add(action);
            }
          };
      try {
        final var output = handler.handle(input, uow);
        onCommitHooks.forEach(Runnable::run);
        return output;
      } catch (RuntimeException ex) {
        onRollbackHooks.forEach(Runnable::run);
        throw ex;
      }
    }
  }

  private static final class InMemorySessionStore implements SessionStorePort {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, SessionQuezapSnapshot> quezaps = new HashMap<>();
    private final Map<UUID, String> usersById = new HashMap<>();

    void registerQuezap(SessionQuezapSnapshot quezap) {
      quezaps.put(quezap.id(), quezap);
    }

    void registerUser(UUID id, String name) {
      usersById.put(id, name);
    }

    @Override
    public Optional<Session> findSessionById(UUID sessionId) {
      return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<Session> findSessionByNumber(long sessionNumber) {
      return sessions.values().stream()
          .filter(s -> s.getNumber().value() == sessionNumber)
          .findFirst();
    }

    @Override
    public boolean existsSessionByNumber(long sessionNumber) {
      return findSessionByNumber(sessionNumber).isPresent();
    }

    @Override
    public PageOf<SessionSummary> findSessionsByAuthorId(UUID authorId, Pagination pagination) {
      final var matching =
          sessions.values().stream()
              .filter(s -> s.getAuthor().value().equals(authorId))
              .map(
                  s ->
                      new SessionSummary(
                          s.getId().value(),
                          s.getNumber().value(),
                          s.getRawName(),
                          s.getStatus(),
                          s.getCurrentSlideIndex(),
                          s.getTotalSlides(),
                          (int)
                              s.getSessionParticipants().stream()
                                  .filter(p -> !p.isKicked())
                                  .count(),
                          s.getAuthor().value(),
                          s.getStartedAt() != null ? s.getStartedAt().position() : null,
                          s.getEndedAt() != null ? s.getEndedAt().position() : null,
                          s.getProgressionMode().name(),
                          s.isPaused(),
                          s.getExplanationDurationSeconds(),
                          s.isMobileCorrectionEnabled()))
              .toList();
      return PageOf.of(pagination, matching, (long) matching.size());
    }

    @Override
    public Optional<SessionQuezapSnapshot> findQuezapById(UUID quezapId) {
      return Optional.ofNullable(quezaps.get(quezapId));
    }

    @Override
    public Optional<UUID> findUserIdByPrincipal(String principal) {
      try {
        final var uuid = UUID.fromString(principal);
        return usersById.containsKey(uuid) ? Optional.of(uuid) : Optional.empty();
      } catch (IllegalArgumentException _) {
        return usersById.entrySet().stream()
            .filter(e -> e.getValue().equals(principal))
            .map(Map.Entry::getKey)
            .findFirst();
      }
    }

    @Override
    public Session saveSession(Session session) {
      sessions.put(session.getId().value(), session);
      return session;
    }

    @Override
    public Session saveAndFlushSession(Session session) {
      return saveSession(session);
    }

    @Override
    public Optional<Session> findSessionHeaderAndSlidesById(UUID sessionId) {
      return findSessionById(sessionId);
    }

    @Override
    public Optional<SessionParticipant> findParticipantByToken(
        UUID sessionId, String participationToken) {
      final var session = sessions.get(sessionId);
      if (session == null) {
        return Optional.empty();
      }
      return session.getSessionParticipants().stream()
          .filter(p -> p.participationToken().equals(participationToken))
          .findFirst();
    }

    @Override
    public Optional<SessionAnswerRecord> findAnswerForParticipantAndSlide(
        UUID sessionId, String participantName, int slideIndex) {
      final var session = sessions.get(sessionId);
      if (session == null) {
        return Optional.empty();
      }
      return session.getSessionAnswers().stream()
          .filter(a -> a.participantName().equals(participantName) && a.slideIndex() == slideIndex)
          .findFirst();
    }

    @Override
    public void saveParticipantAndAnswer(
        UUID sessionId,
        SessionParticipant participant,
        SessionAnswerRecord answerRecord,
        boolean isNewAnswer) {
      final var session = Objects.requireNonNull(sessions.get(sessionId));
      final var participants = new ArrayList<>(session.getSessionParticipants());
      participants.replaceAll(p -> p.id().equals(participant.id()) ? participant : p);
      final var answers = new ArrayList<>(session.getSessionAnswers());
      if (isNewAnswer) {
        answers.add(answerRecord);
      } else {
        answers.replaceAll(a -> a.id().equals(answerRecord.id()) ? answerRecord : a);
      }
      sessions.put(
          sessionId,
          Session.hydrateLive(
              session.getId(),
              session.getRawName(),
              session.getNumber(),
              session.getCurrentSlideIndex(),
              session.getAuthor(),
              session.getQuezapId(),
              session.getQuezapTitle(),
              session.getProgressionMode(),
              session.isPaused(),
              session.getExplanationDurationSeconds(),
              session.isMobileCorrectionEnabled(),
              session.getStartedAt(),
              session.getCurrentSlideStartedAt(),
              session.getPausedAt(),
              session.getEndedAt(),
              session.getLastRevealedSlideIndex(),
              session.getCreatedAt(),
              session.getSlideSpecs(),
              participants,
              answers));
    }

    @Override
    public int countActiveParticipants(UUID sessionId) {
      final var session = sessions.get(sessionId);
      if (session == null) {
        return 0;
      }
      return (int) session.getSessionParticipants().stream().filter(p -> !p.isKicked()).count();
    }

    @Override
    public int countActiveAnswersForSlide(UUID sessionId, int slideIndex) {
      final var session = sessions.get(sessionId);
      if (session == null) {
        return 0;
      }
      final var activeNames =
          session.getSessionParticipants().stream()
              .filter(p -> !p.isKicked())
              .map(SessionParticipant::name)
              .collect(java.util.stream.Collectors.toSet());
      return (int)
          session.getSessionAnswers().stream()
              .filter(
                  a -> a.slideIndex() == slideIndex && activeNames.contains(a.participantName()))
              .count();
    }
  }
}
