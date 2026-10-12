package com.quezap.api.v1.routes.sessions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

import com.quezap.api.v1.dto.events.AnswerCountPayload;
import com.quezap.api.v1.dto.events.AnswerStatDto;
import com.quezap.api.v1.dto.events.FinalLeaderboardEntryDto;
import com.quezap.api.v1.dto.events.IntermediateLeaderboardPayload;
import com.quezap.api.v1.dto.events.LobbyStatePayload;
import com.quezap.api.v1.dto.events.ParticipantDto;
import com.quezap.api.v1.dto.events.ParticipantKickedPayload;
import com.quezap.api.v1.dto.events.PodiumDto;
import com.quezap.api.v1.dto.events.QuestionClosedPayload;
import com.quezap.api.v1.dto.events.QuestionRevealPayload;
import com.quezap.api.v1.dto.events.QuestionStartPayload;
import com.quezap.api.v1.dto.events.SafeAnswerDto;
import com.quezap.api.v1.dto.events.SafeQuestionDto;
import com.quezap.api.v1.dto.events.SessionEndedPayload;
import com.quezap.api.v1.dto.events.SessionGroupMetricsDto;
import com.quezap.api.v1.dto.events.SessionPausePayload;
import com.quezap.api.v1.dto.events.SessionStartedPayload;
import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.sessions.CreateSessionRequest;
import com.quezap.api.v1.dto.request.sessions.JoinSessionRequest;
import com.quezap.api.v1.dto.request.sessions.SubmitAnswerRequest;
import com.quezap.api.v1.dto.response.PageOfDto;
import com.quezap.api.v1.dto.response.sessions.AnswerSubmittedDto;
import com.quezap.api.v1.dto.response.sessions.JoinSessionResponseDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.NextQuestionResponseDto;
import com.quezap.api.v1.dto.response.sessions.SessionCreatedDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionStatusDto;
import com.quezap.api.v1.dto.sessions.SessionPauseRequestDto;
import com.quezap.api.v1.dto.sessions.SessionPauseResponseDto;
import com.quezap.api.v1.dto.sessions.SessionSettingsRequestDto;
import com.quezap.api.v1.dto.sessions.SessionSettingsResponseDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.ports.SessionDataPort;
import com.quezap.api.v1.services.SessionReportService;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionAnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.lib.utils.UuidV7;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/sessions")
public class SessionController {

  private final SessionDataPort sessionDataPort;
  private final SessionSseService sessionSseService;
  private final SessionReportService sessionReportService;
  private final PaginationMapper paginationMapper;
  private final UserNameSanitizer userNameSanitizer;
  private final TransactionTemplate transactionTemplate;

  public SessionController(
      SessionDataPort sessionDataPort,
      SessionSseService sessionSseService,
      SessionReportService sessionReportService,
      PaginationMapper paginationMapper,
      UserNameSanitizer userNameSanitizer,
      PlatformTransactionManager transactionManager) {
    this.sessionDataPort = sessionDataPort;
    this.sessionSseService = sessionSseService;
    this.sessionReportService = sessionReportService;
    this.paginationMapper = paginationMapper;
    this.userNameSanitizer = userNameSanitizer;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setReadOnly(true);
  }

  @GetMapping
  @Transactional(readOnly = true)
  public PageOfDto<SessionDetailDto> list(
      @RequestParam(required = false) @Nullable UUID authorId, PaginationDto paginationDto) {

    final var pagination = paginationMapper.toDomain(paginationDto);
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

    Page<SessionJpaEntity> page;
    if (authorId != null) {
      page = sessionDataPort.findSessionsByAuthorId(authorId, pageRequest);
    } else {
      final var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() != null) {
        try {
          final var userUuid = UUID.fromString(auth.getPrincipal().toString());
          page = sessionDataPort.findSessionsByAuthorId(userUuid, pageRequest);
        } catch (IllegalArgumentException _) {
          page = sessionDataPort.findAllSessions(pageRequest);
        }
      } else {
        page = sessionDataPort.findAllSessions(pageRequest);
      }
    }

    final var items =
        page.getContent().stream()
            .map(
                sessionJpa -> {
                  final var status = computeStatus(sessionJpa);
                  final var quezap = sessionJpa.getQuezap();
                  final var totalSlides = quezap != null ? quezap.getQuestions().size() : 0;
                  return new SessionDetailDto(
                      sessionJpa.getId(),
                      sessionJpa.getNumber(),
                      formatSessionCode(sessionJpa.getNumber()),
                      sessionJpa.getName(),
                      status,
                      sessionJpa.getCurrentSlideIndex(),
                      totalSlides,
                      sessionJpa.getParticipants().size(),
                      List.of(),
                      sessionJpa.getAuthorId(),
                      sessionJpa.getStartedAt(),
                      sessionJpa.getEndedAt(),
                      sessionJpa.getProgressionMode(),
                      Boolean.TRUE.equals(sessionJpa.getIsPaused()),
                      sessionJpa.getExplanationDurationSeconds(),
                      Boolean.TRUE.equals(sessionJpa.getMobileCorrectionEnabled()));
                })
            .toList();

    return new PageOfDto<>(
        items,
        page.getTotalElements(),
        page.getTotalPages(),
        pagination.pageNumber(),
        page.getSize(),
        page.hasNext(),
        page.hasPrevious());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public SessionCreatedDto create(@Valid @RequestBody CreateSessionRequest request) {
    final var author = getCurrentUser();
    final var sessionNumber = generateUniqueSessionNumber();

    if (request.quezapId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Un Quezap doit être sélectionné pour créer une session");
    }

    final var quezapEntity =
        sessionDataPort
            .findQuezapById(request.quezapId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Quezap introuvable : " + request.quezapId()));

    validateQuezapCompleteness(quezapEntity);

    final var sessionJpa =
        new SessionJpaEntity(
            UuidV7.randomUuid(),
            sessionNumber,
            request.name(),
            0,
            author.getId(),
            quezapEntity,
            null,
            null,
            Instant.now());

    sessionDataPort.saveSession(sessionJpa);

    return new SessionCreatedDto(
        sessionJpa.getId(), sessionNumber, formatSessionCode(sessionNumber));
  }

  @GetMapping("{id}")
  @Transactional(readOnly = true)
  public SessionDetailDto getById(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    final var status = computeStatus(sessionJpa);
    final var quezap = sessionJpa.getQuezap();
    final var totalSlides = quezap != null ? quezap.getQuestions().size() : 0;
    final var participantList =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .map(p -> new ParticipantDto(p.getName(), p.getJoinedAt()))
            .toList();

    return new SessionDetailDto(
        sessionJpa.getId(),
        sessionJpa.getNumber(),
        formatSessionCode(sessionJpa.getNumber()),
        sessionJpa.getName(),
        status,
        sessionJpa.getCurrentSlideIndex(),
        totalSlides,
        participantList.size(),
        participantList,
        sessionJpa.getAuthorId(),
        sessionJpa.getStartedAt(),
        sessionJpa.getEndedAt(),
        sessionJpa.getProgressionMode(),
        Boolean.TRUE.equals(sessionJpa.getIsPaused()),
        sessionJpa.getExplanationDurationSeconds(),
        Boolean.TRUE.equals(sessionJpa.getMobileCorrectionEnabled()));
  }

  @GetMapping("by-code/{code}")
  @Transactional(readOnly = true)
  public SessionDetailDto getByCode(@PathVariable String code) {
    final var sessionNumber = parseSessionCode(code);
    final var sessionJpa =
        sessionDataPort
            .findSessionByNumber(sessionNumber)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid session code: " + code));

    final var status = computeStatus(sessionJpa);
    final var quezap = sessionJpa.getQuezap();
    final var totalSlides = quezap != null ? quezap.getQuestions().size() : 0;
    final var participantList =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .map(p -> new ParticipantDto(p.getName(), p.getJoinedAt()))
            .toList();

    return new SessionDetailDto(
        sessionJpa.getId(),
        sessionJpa.getNumber(),
        formatSessionCode(sessionJpa.getNumber()),
        sessionJpa.getName(),
        status,
        sessionJpa.getCurrentSlideIndex(),
        totalSlides,
        participantList.size(),
        participantList,
        sessionJpa.getAuthorId(),
        sessionJpa.getStartedAt(),
        sessionJpa.getEndedAt(),
        sessionJpa.getProgressionMode(),
        Boolean.TRUE.equals(sessionJpa.getIsPaused()),
        sessionJpa.getExplanationDurationSeconds(),
        Boolean.TRUE.equals(sessionJpa.getMobileCorrectionEnabled()));
  }

  @PostMapping("join")
  @Transactional
  public JoinSessionResponseDto join(@Valid @RequestBody JoinSessionRequest request) {
    if (request.participantName().contains("<") || request.participantName().contains(">")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Participant name must not contain HTML tags");
    }

    final var sessionNumber = parseSessionCode(request.code());
    final var sessionJpa =
        sessionDataPort
            .findSessionByNumber(sessionNumber)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid session code: " + request.code()));

    final var sanitizedName = userNameSanitizer.sanitize(request.participantName());
    if (sanitizedName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid participant name");
    }

    final var existingParticipant =
        sessionJpa.getParticipants().stream()
            .filter(p -> p.getName().equalsIgnoreCase(sanitizedName))
            .findFirst();

    if (existingParticipant.isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Participant name is already taken in this session");
    }

    final var token = "ptk_" + UUID.randomUUID().toString();
    final var participant =
        new SessionParticipantJpaEntity(
            UuidV7.randomUuid(), sessionJpa, sanitizedName, 0, token, false, Instant.now());

    sessionJpa.addParticipant(participant);
    sessionDataPort.saveAndFlushSession(sessionJpa);

    // Broadcast LOBBY_STATE to all SSE subscribers
    final var participantList =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .map(p -> new ParticipantDto(p.getName(), p.getJoinedAt()))
            .toList();

    final var quezap = sessionJpa.getQuezap();
    final var totalQuestions = quezap != null ? quezap.getQuestions().size() : 0;

    final var lobbyPayload =
        new LobbyStatePayload(
            sessionJpa.getName(), participantList.size(), participantList, totalQuestions);

    sessionSseService.broadcast(
        sessionJpa.getId(), "LOBBY_STATE", sessionJpa.getNumber(), lobbyPayload);

    return new JoinSessionResponseDto(sessionJpa.getId(), token, sanitizedName);
  }

  @PostMapping("{id}/start")
  @Transactional
  public SessionStatusDto start(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    assertSessionHost(sessionJpa);

    if (sessionJpa.getStartedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session has already started");
    }

    final var quezap = sessionJpa.getQuezap();
    validateQuezapCompleteness(quezap);

    final var questionsList =
        quezap != null
            ? quezap.getQuestions().stream()
                .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
                .toList()
            : List.<QuezapQuestionJpaEntity>of();
    final var totalSlides = questionsList.size();

    final var now = Instant.now();
    sessionJpa.setStartedAt(now);
    sessionJpa.setCurrentSlideIndex(1);
    sessionJpa.setCurrentSlideStartedAt(now);
    sessionDataPort.saveAndFlushSession(sessionJpa);

    final var startedAt = sessionJpa.getStartedAt() != null ? sessionJpa.getStartedAt() : now;
    final var payload = new SessionStartedPayload(startedAt, totalSlides, 3);
    sessionSseService.broadcast(
        sessionJpa.getId(), "SESSION_STARTED", sessionJpa.getNumber(), payload);

    if (!questionsList.isEmpty()) {
      final var currentQq = questionsList.get(0);
      final var question = currentQq.getQuestion();

      var answerIndex = 0;
      final var safeAnswers = new ArrayList<SafeAnswerDto>();
      final var sortedAnswers =
          question.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .toList();

      if ("BOOLEAN".equalsIgnoreCase(question.getType())) {
        safeAnswers.add(new SafeAnswerDto(0, "Vrai", null));
        safeAnswers.add(new SafeAnswerDto(1, "Faux", null));
      } else {
        for (final var a : sortedAnswers) {
          safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
        }
      }

      final var safeQuestion =
          new SafeQuestionDto(
              question.getId(),
              question.getType(),
              question.getLabel(),
              question.getPictureFilename(),
              safeAnswers);

      final var timerSeconds = currentQq.getTimerSeconds();
      final var deadline = now.plusSeconds(timerSeconds);
      final var startPayload =
          new QuestionStartPayload(
              1,
              totalSlides,
              timerSeconds,
              currentQq.getPoints(),
              safeQuestion,
              deadline,
              0,
              timerSeconds);

      sessionSseService.broadcast(
          sessionJpa.getId(), "QUESTION_START", sessionJpa.getNumber(), startPayload);
    }

    return new SessionStatusDto("STARTED");
  }

  @PostMapping("{id}/next-question")
  @Transactional
  public NextQuestionResponseDto nextQuestion(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    assertSessionHost(sessionJpa);

    final var quezap = sessionJpa.getQuezap();
    if (quezap == null || quezap.getQuestions().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Session does not contain questions");
    }

    final var questionsList =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    final var nextIndex = sessionJpa.getCurrentSlideIndex() + 1;
    final var totalSlides = questionsList.size();

    if (nextIndex > totalSlides) {
      final var endedAt = Instant.now();
      sessionJpa.setEndedAt(endedAt);
      sessionDataPort.saveAndFlushSession(sessionJpa);

      final var sortedParticipants =
          sessionJpa.getParticipants().stream()
              .filter(p -> !p.getIsKicked())
              .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
              .toList();

      var rank = 1;
      final var finalLeaderboard = new ArrayList<FinalLeaderboardEntryDto>();
      for (final var p : sortedParticipants) {
        final var correctCount =
            (int)
                sessionJpa.getAnswers().stream()
                    .filter(
                        a ->
                            a.getParticipantName().equals(p.getName())
                                && Boolean.TRUE.equals(a.getIsCorrect()))
                    .count();
        finalLeaderboard.add(
            new FinalLeaderboardEntryDto(rank++, p.getName(), p.getScore(), correctCount));
      }

      final var podium =
          new PodiumDto(
              !sortedParticipants.isEmpty() ? sortedParticipants.get(0).getName() : "",
              sortedParticipants.size() > 1 ? sortedParticipants.get(1).getName() : null,
              sortedParticipants.size() > 2 ? sortedParticipants.get(2).getName() : null);

      final var totalParticipants = finalLeaderboard.size();
      final var averageScore =
          totalParticipants > 0
              ? (int)
                  Math.round(
                      finalLeaderboard.stream()
                          .mapToInt(FinalLeaderboardEntryDto::totalScore)
                          .average()
                          .orElse(0.0))
              : 0;

      final var totalAnswers = sessionJpa.getAnswers().size();
      final var correctAnswers =
          sessionJpa.getAnswers().stream()
              .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
              .count();
      final var globalSuccessRate =
          totalAnswers > 0
              ? Math.round((double) correctAnswers / totalAnswers * 1000.0) / 10.0
              : 0.0;

      final var groupMetrics =
          new SessionGroupMetricsDto(totalParticipants, averageScore, globalSuccessRate);
      final var top5 = finalLeaderboard.subList(0, Math.min(5, finalLeaderboard.size()));

      final var endedPayload =
          new SessionEndedPayload(endedAt, finalLeaderboard, podium, groupMetrics, top5);
      sessionSseService.broadcast(
          sessionJpa.getId(), "SESSION_ENDED", sessionJpa.getNumber(), endedPayload);

      return new NextQuestionResponseDto(sessionJpa.getCurrentSlideIndex(), totalSlides);
    }

    sessionJpa.setCurrentSlideIndex(nextIndex);
    sessionJpa.setCurrentSlideStartedAt(Instant.now());
    sessionDataPort.saveAndFlushSession(sessionJpa);

    // Get current slide question details for SSE (Safe without isCorrect)
    final var currentQq = questionsList.get(Math.min(nextIndex - 1, totalSlides - 1));
    final var question = currentQq.getQuestion();

    var answerIndex = 0;
    final var safeAnswers = new ArrayList<SafeAnswerDto>();
    final var sortedAnswers =
        question.getAnswers().stream()
            .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
            .toList();

    if ("BOOLEAN".equalsIgnoreCase(question.getType())) {
      safeAnswers.add(new SafeAnswerDto(0, "Vrai", null));
      safeAnswers.add(new SafeAnswerDto(1, "Faux", null));
    } else {
      for (final var a : sortedAnswers) {
        safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
      }
    }

    final var safeQuestion =
        new SafeQuestionDto(
            question.getId(),
            question.getType(),
            question.getLabel(),
            question.getPictureFilename(),
            safeAnswers);

    final var timerSeconds = currentQq.getTimerSeconds();
    final var deadline = Instant.now().plusSeconds(timerSeconds);
    final var startPayload =
        new QuestionStartPayload(
            nextIndex,
            totalSlides,
            timerSeconds,
            currentQq.getPoints(),
            safeQuestion,
            deadline,
            0,
            timerSeconds);

    sessionSseService.broadcast(
        sessionJpa.getId(), "QUESTION_START", sessionJpa.getNumber(), startPayload);

    return new NextQuestionResponseDto(nextIndex, totalSlides);
  }

  @PostMapping("{id}/answers")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public AnswerSubmittedDto submitAnswer(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false) @Nullable String headerToken,
      @Valid @RequestBody SubmitAnswerRequest request) {

    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    if (sessionJpa.getStartedAt() == null || sessionJpa.getEndedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not currently active");
    }

    if (request.slideIndex() != null
        && !request.slideIndex().equals(sessionJpa.getCurrentSlideIndex())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid slide index for current question");
    }

    final var participant =
        sessionJpa.getParticipants().stream()
            .filter(p -> p.getParticipationToken().equals(headerToken))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid participant token"));

    if (participant.getIsKicked()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Participant was kicked from the session");
    }

    final var effectiveSlideIndex =
        request.slideIndex() != null
            ? request.slideIndex()
            : Math.max(sessionJpa.getCurrentSlideIndex(), 1);

    // Rejet strict des réponses post-reveal (P0-3)
    if (sessionJpa.getLastRevealedSlideIndex() != null
        && sessionJpa.getLastRevealedSlideIndex() >= effectiveSlideIndex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Les réponses sont verrouillées pour cette question");
    }

    // Check duplicate answer for this slide
    final var existingAnswer =
        sessionJpa.getAnswers().stream()
            .filter(
                a ->
                    a.getParticipantName().equals(participant.getName())
                        && a.getSlideIndex().equals(effectiveSlideIndex))
            .findFirst();

    if (existingAnswer.isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Answer already submitted for this question slide");
    }

    // Compute answer correctness and score
    var isCorrect = false;
    var pointsAwarded = 0;
    final var quezap = sessionJpa.getQuezap();
    if (quezap != null) {
      final var questions =
          quezap.getQuestions().stream()
              .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
              .toList();
      final var slideIdx = effectiveSlideIndex - 1;
      if (slideIdx >= 0 && slideIdx < questions.size()) {
        final var qq = questions.get(slideIdx);
        final var timerSeconds = qq.getTimerSeconds() != null ? qq.getTimerSeconds() : 30;
        final var slideStartedAt =
            sessionJpa.getCurrentSlideStartedAt() != null
                ? sessionJpa.getCurrentSlideStartedAt()
                : sessionJpa.getStartedAt();
        if (slideStartedAt != null) {
          final var elapsedSeconds = Duration.between(slideStartedAt, Instant.now()).toSeconds();
          if (elapsedSeconds > timerSeconds + 2) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Le temps imparti est écoulé");
          }
        }

        final var answers =
            qq.getQuestion().getAnswers().stream()
                .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
                .toList();

        final var questionType = qq.getQuestion().getType();
        if ("BOOLEAN".equalsIgnoreCase(questionType)) {
          if (request.answerIndex() == 0 || request.answerIndex() == 1) {
            final var answerEntity = answers.isEmpty() ? null : answers.get(0);
            final var statementIsTrue =
                answerEntity != null && Boolean.TRUE.equals(answerEntity.getIsCorrect());
            isCorrect =
                (request.answerIndex() == 0 && statementIsTrue)
                    || (request.answerIndex() == 1 && !statementIsTrue);
            if (isCorrect) {
              final var elapsedMillis =
                  slideStartedAt != null
                      ? Math.max(0, Duration.between(slideStartedAt, Instant.now()).toMillis())
                      : 0L;
              final var basePoints = qq.getPoints() != null ? qq.getPoints() : 1000;
              pointsAwarded = Session.calculateScore(basePoints, timerSeconds, elapsedMillis, true);
            }
          }
        } else {
          if (request.answerIndex() >= 0 && request.answerIndex() < answers.size()) {
            final var answerEntity = answers.get(request.answerIndex());
            isCorrect = Boolean.TRUE.equals(answerEntity.getIsCorrect());
            if (isCorrect) {
              final var elapsedMillis =
                  slideStartedAt != null
                      ? Math.max(0, Duration.between(slideStartedAt, Instant.now()).toMillis())
                      : 0L;
              final var basePoints = qq.getPoints() != null ? qq.getPoints() : 1000;
              pointsAwarded = Session.calculateScore(basePoints, timerSeconds, elapsedMillis, true);
            }
          }
        }
      }
    }

    final var answerEntity =
        new SessionAnswerJpaEntity(
            UuidV7.randomUuid(),
            sessionJpa,
            participant.getName(),
            effectiveSlideIndex,
            request.answerIndex(),
            pointsAwarded,
            isCorrect,
            Instant.now());

    sessionJpa.addAnswer(answerEntity);
    participant.setScore(participant.getScore() + pointsAwarded);
    sessionDataPort.saveSession(sessionJpa);

    // Check if all active participants have answered this slide
    final var totalActive =
        (int) sessionJpa.getParticipants().stream().filter(p -> !p.getIsKicked()).count();
    final var totalAnswered =
        (int)
            sessionJpa.getAnswers().stream()
                .filter(a -> a.getSlideIndex().equals(effectiveSlideIndex))
                .count();

    // Broadcast live answer progress
    final var answerCountPayload =
        new AnswerCountPayload(effectiveSlideIndex, totalAnswered, totalActive);
    sessionSseService.broadcast(
        sessionJpa.getId(), "ANSWER_RECEIVED", sessionJpa.getNumber(), answerCountPayload);

    if (totalAnswered >= totalActive && totalActive > 0) {
      // Broadcast QUESTION_CLOSED
      final var closedPayload =
          new QuestionClosedPayload(
              effectiveSlideIndex, totalAnswered, totalActive, "ALL_PARTICIPANTS_ANSWERED");
      sessionSseService.broadcast(
          sessionJpa.getId(), "QUESTION_CLOSED", sessionJpa.getNumber(), closedPayload);

      // Broadcast QUESTION_REVEAL
      broadcastRevealAndLeaderboard(sessionJpa, effectiveSlideIndex);
    }

    return new AnswerSubmittedDto(true, Instant.now());
  }

  @PostMapping("{id}/reveal")
  @Transactional
  public void reveal(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);
    final var currentSlide = Math.max(sessionJpa.getCurrentSlideIndex(), 1);
    broadcastRevealAndLeaderboard(sessionJpa, currentSlide);
  }

  @PostMapping("{id}/pause")
  @Transactional
  public SessionPauseResponseDto pauseSession(
      @PathVariable UUID id, @Valid @RequestBody SessionPauseRequestDto request) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);

    sessionJpa.setIsPaused(request.paused());
    sessionDataPort.saveAndFlushSession(sessionJpa);

    final var pausePayload = new SessionPausePayload(request.paused(), Instant.now());
    sessionSseService.broadcast(
        sessionJpa.getId(), "SESSION_PAUSE_STATUS", sessionJpa.getNumber(), pausePayload);

    return new SessionPauseResponseDto(
        sessionJpa.getId(), Boolean.TRUE.equals(sessionJpa.getIsPaused()));
  }

  @PutMapping("{id}/settings")
  @Transactional
  public SessionSettingsResponseDto updateSettings(
      @PathVariable UUID id, @Valid @RequestBody SessionSettingsRequestDto request) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);

    if (request.progressionMode() != null) {
      sessionJpa.setProgressionMode(request.progressionMode());
    }
    if (request.explanationDurationSeconds() != null) {
      sessionJpa.setExplanationDurationSeconds(request.explanationDurationSeconds());
    }
    if (request.mobileCorrectionEnabled() != null) {
      sessionJpa.setMobileCorrectionEnabled(request.mobileCorrectionEnabled());
    }

    sessionDataPort.saveAndFlushSession(sessionJpa);

    return new SessionSettingsResponseDto(
        sessionJpa.getProgressionMode(),
        sessionJpa.getExplanationDurationSeconds(),
        Boolean.TRUE.equals(sessionJpa.getMobileCorrectionEnabled()));
  }

  @GetMapping(value = "{id}/export/csv", produces = "text/csv")
  @Transactional(readOnly = true)
  public ResponseEntity<String> exportCsv(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);
    return sessionReportService.exportCsv(sessionJpa);
  }

  public static String escapeCsvCell(@Nullable String value) {
    return SessionReportService.escapeCsvCell(value);
  }

  @GetMapping("{id}/scores")
  @Transactional(readOnly = true)
  public LeaderboardResponseDto getScores(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    return sessionReportService.buildScores(sessionJpa);
  }

  @GetMapping("{id}/report")
  @Transactional(readOnly = true)
  public SessionReportDto getReport(@PathVariable UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);
    return sessionReportService.buildSessionReport(sessionJpa);
  }

  @PostMapping("{id}/participants/{name}/kick")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void kickParticipant(@PathVariable UUID id, @PathVariable String name) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);

    final var participant =
        sessionJpa.getParticipants().stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));

    participant.setIsKicked(true);
    sessionDataPort.saveAndFlushSession(sessionJpa);

    // Broadcast PARTICIPANT_KICKED
    final var kickedPayload = new ParticipantKickedPayload(name, "HOST_KICK");
    sessionSseService.broadcast(
        sessionJpa.getId(), "PARTICIPANT_KICKED", sessionJpa.getNumber(), kickedPayload);

    // Broadcast updated LOBBY_STATE
    final var participantList =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .map(p -> new ParticipantDto(p.getName(), p.getJoinedAt()))
            .toList();

    final var quezap = sessionJpa.getQuezap();
    final var totalQuestions = quezap != null ? quezap.getQuestions().size() : 0;
    final var lobbyPayload =
        new LobbyStatePayload(
            sessionJpa.getName(), participantList.size(), participantList, totalQuestions);

    sessionSseService.broadcast(
        sessionJpa.getId(), "LOBBY_STATE", sessionJpa.getNumber(), lobbyPayload);
  }

  private record InitialEventsData(
      Long sessionNumber,
      LobbyStatePayload lobbyPayload,
      @Nullable QuestionStartPayload startPayload,
      @Nullable QuestionRevealPayload revealPayload,
      @Nullable IntermediateLeaderboardPayload leaderboardPayload) {}

  @Transactional(readOnly = true)
  protected InitialEventsData getInitialStreamData(UUID id) {
    final var sessionJpa =
        sessionDataPort
            .findSessionById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    final var participantList =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .map(p -> new ParticipantDto(p.getName(), p.getJoinedAt()))
            .toList();

    final var quezap = sessionJpa.getQuezap();
    final var totalQuestions = quezap != null ? quezap.getQuestions().size() : 0;

    final var lobbyPayload =
        new LobbyStatePayload(
            sessionJpa.getName(), participantList.size(), participantList, totalQuestions);

    QuestionStartPayload startPayload = null;
    QuestionRevealPayload revealPayload = null;
    IntermediateLeaderboardPayload leaderboardPayload = null;

    if (sessionJpa.getStartedAt() != null
        && sessionJpa.getEndedAt() == null
        && sessionJpa.getCurrentSlideIndex() > 0) {
      final var nextIndex = sessionJpa.getCurrentSlideIndex();
      final var questionsList =
          quezap != null
              ? quezap.getQuestions().stream()
                  .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
                  .toList()
              : List.<QuezapQuestionJpaEntity>of();
      if (!questionsList.isEmpty() && nextIndex <= questionsList.size()) {
        final var currentQq = questionsList.get(nextIndex - 1);
        final var question = currentQq.getQuestion();

        final var safeAnswers = new ArrayList<SafeAnswerDto>();
        if ("BOOLEAN".equalsIgnoreCase(question.getType())) {
          safeAnswers.add(new SafeAnswerDto(0, "Vrai", null));
          safeAnswers.add(new SafeAnswerDto(1, "Faux", null));
        } else {
          var answerIndex = 0;
          final var sortedAnswers =
              question.getAnswers().stream()
                  .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
                  .toList();
          for (final var a : sortedAnswers) {
            safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
          }
        }

        final var safeQuestion =
            new SafeQuestionDto(
                question.getId(),
                question.getType(),
                question.getLabel(),
                question.getPictureFilename(),
                safeAnswers);

        final var slideStartedAt = sessionJpa.getCurrentSlideStartedAt();
        final var timerSeconds = currentQq.getTimerSeconds();
        final var elapsedSeconds =
            slideStartedAt != null
                ? (int) Math.max(0, Duration.between(slideStartedAt, Instant.now()).toSeconds())
                : 0;
        final var remainingSeconds = Math.max(0, timerSeconds - elapsedSeconds);
        final var deadline =
            slideStartedAt != null
                ? slideStartedAt.plusSeconds(timerSeconds)
                : Instant.now().plusSeconds(remainingSeconds);

        final var answeredCount =
            (int)
                sessionJpa.getAnswers().stream()
                    .filter(a -> a.getSlideIndex().equals(nextIndex))
                    .count();

        startPayload =
            new QuestionStartPayload(
                nextIndex,
                questionsList.size(),
                timerSeconds,
                currentQq.getPoints(),
                safeQuestion,
                deadline,
                answeredCount,
                remainingSeconds);

        if (sessionJpa.getLastRevealedSlideIndex() != null
            && sessionJpa.getLastRevealedSlideIndex().equals(nextIndex)) {
          revealPayload = buildRevealPayload(sessionJpa, questionsList, nextIndex);
          leaderboardPayload = buildIntermediateLeaderboardPayload(sessionJpa, nextIndex);
        }
      }
    }

    return new InitialEventsData(
        sessionJpa.getNumber(), lobbyPayload, startPayload, revealPayload, leaderboardPayload);
  }

  @GetMapping(value = "{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(
      @PathVariable UUID id,
      @RequestHeader(value = "Last-Event-ID", required = false) @Nullable String lastEventId) {
    final var initialData = transactionTemplate.execute(status -> getInitialStreamData(id));
    if (initialData == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
    final var emitter = sessionSseService.registerEmitter(id);

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

    sessionSseService.sendToEmitter(
        emitter, "LOBBY_STATE", initialData.sessionNumber(), id, initialData.lobbyPayload());

    if (initialData.startPayload() != null) {
      sessionSseService.sendToEmitter(
          emitter, "QUESTION_START", initialData.sessionNumber(), id, initialData.startPayload());
    }

    if (initialData.revealPayload() != null) {
      sessionSseService.sendToEmitter(
          emitter, "QUESTION_REVEAL", initialData.sessionNumber(), id, initialData.revealPayload());
    }

    if (initialData.leaderboardPayload() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "INTERMEDIATE_LEADERBOARD",
          initialData.sessionNumber(),
          id,
          initialData.leaderboardPayload());
    }

    return emitter;
  }

  private @Nullable QuestionRevealPayload buildRevealPayload(
      SessionJpaEntity sessionJpa, List<QuezapQuestionJpaEntity> questions, int slideIndex) {
    final var slideIdx = slideIndex - 1;
    if (slideIdx < 0 || slideIdx >= questions.size()) {
      return null;
    }

    final var qq = questions.get(slideIdx);
    final var answers =
        qq.getQuestion().getAnswers().stream()
            .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
            .toList();
    final var correctIndices = new ArrayList<Integer>();
    final var stats = new ArrayList<AnswerStatDto>();

    final var slideAnswers =
        sessionJpa.getAnswers().stream().filter(a -> a.getSlideIndex().equals(slideIndex)).toList();

    final var totalCount = Math.max(slideAnswers.size(), 1);
    final var questionType = qq.getQuestion().getType();

    if ("BOOLEAN".equalsIgnoreCase(questionType)) {
      final var answerEntity = answers.isEmpty() ? null : answers.get(0);
      final var statementIsTrue =
          answerEntity != null && Boolean.TRUE.equals(answerEntity.getIsCorrect());
      if (statementIsTrue) {
        correctIndices.add(0);
      } else {
        correctIndices.add(1);
      }
      for (var i = 0; i < 2; i++) {
        final var answerIndex = i;
        final var count =
            (int)
                slideAnswers.stream().filter(sa -> sa.getAnswerIndex().equals(answerIndex)).count();
        final var percentage = (double) count / totalCount * 100.0;
        stats.add(new AnswerStatDto(i, count, percentage));
      }
    } else {
      for (var i = 0; i < answers.size(); i++) {
        final var a = answers.get(i);
        if (Boolean.TRUE.equals(a.getIsCorrect())) {
          correctIndices.add(i);
        }
        final var answerIndex = i;
        final var count =
            (int)
                slideAnswers.stream().filter(sa -> sa.getAnswerIndex().equals(answerIndex)).count();
        final var percentage = (double) count / totalCount * 100.0;
        stats.add(new AnswerStatDto(i, count, percentage));
      }
    }

    return new QuestionRevealPayload(
        slideIndex,
        correctIndices,
        null,
        stats,
        Boolean.TRUE.equals(sessionJpa.getMobileCorrectionEnabled()));
  }

  private IntermediateLeaderboardPayload buildIntermediateLeaderboardPayload(
      SessionJpaEntity sessionJpa, int slideIndex) {
    final var sortedParticipants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    var rank = 1;
    final var leaderboard = new ArrayList<LeaderboardEntryDto>();
    for (final var p : sortedParticipants) {
      final var currentAnswerOpt =
          sessionJpa.getAnswers().stream()
              .filter(
                  a ->
                      a.getParticipantName().equals(p.getName())
                          && a.getSlideIndex().equals(slideIndex))
              .findFirst();
      final var delta = currentAnswerOpt.map(SessionAnswerJpaEntity::getPointsAwarded).orElse(0);

      var streak = 0;
      for (int s = slideIndex; s >= 1; s--) {
        final int sIdx = s;
        final var ans =
            sessionJpa.getAnswers().stream()
                .filter(
                    a ->
                        a.getParticipantName().equals(p.getName())
                            && a.getSlideIndex().equals(sIdx))
                .findFirst();
        if (ans.isPresent() && Boolean.TRUE.equals(ans.get().getIsCorrect())) {
          streak++;
        } else {
          break;
        }
      }

      leaderboard.add(new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), delta, streak));
    }

    return new IntermediateLeaderboardPayload(slideIndex, leaderboard);
  }

  private void broadcastRevealAndLeaderboard(SessionJpaEntity sessionJpa, int slideIndex) {
    sessionJpa.setLastRevealedSlideIndex(slideIndex);
    sessionDataPort.saveSession(sessionJpa);

    final var quezap = sessionJpa.getQuezap();
    if (quezap == null) {
      return;
    }

    final var questions =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();

    final var revealPayload = buildRevealPayload(sessionJpa, questions, slideIndex);
    if (revealPayload != null) {
      sessionSseService.broadcast(
          sessionJpa.getId(), "QUESTION_REVEAL", sessionJpa.getNumber(), revealPayload);
    }

    final var leaderboardPayload = buildIntermediateLeaderboardPayload(sessionJpa, slideIndex);
    sessionSseService.broadcast(
        sessionJpa.getId(), "INTERMEDIATE_LEADERBOARD", sessionJpa.getNumber(), leaderboardPayload);
  }

  private String computeStatus(SessionJpaEntity session) {
    if (session.getEndedAt() != null) {
      return "ENDED";
    }
    if (session.getStartedAt() != null) {
      return "STARTED";
    }
    return "LOBBY";
  }

  private Long generateUniqueSessionNumber() {
    for (int i = 0; i < 20; i++) {
      final var number = ThreadLocalRandom.current().nextLong(10000, 99999);
      if (!sessionDataPort.existsSessionByNumber(number)) {
        return number;
      }
    }
    return ThreadLocalRandom.current().nextLong(100000, 999999);
  }

  private String formatSessionCode(Long number) {
    return String.valueOf(number);
  }

  private Long parseSessionCode(String code) {
    final var digitsOnly = code.replaceAll("[^0-9]", "");
    if (digitsOnly.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid session code: " + code);
    }
    try {
      return Long.parseLong(digitsOnly);
    } catch (NumberFormatException _) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid session code: " + code);
    }
  }

  private UserJpaEntity getCurrentUser() {
    final var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() != null) {
      final var principal = auth.getPrincipal().toString();
      try {
        final var userId = UUID.fromString(principal);
        final var userOpt = sessionDataPort.findUserById(userId);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      } catch (IllegalArgumentException _) {
        final var userOpt = sessionDataPort.findUserByName(principal);
        if (userOpt.isPresent()) {
          return userOpt.get();
        }
      }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
  }

  private void validateQuezapCompleteness(@Nullable QuezapJpaEntity quezap) {
    if (quezap == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Un Quezap valide est obligatoire pour lancer une session");
    }
    final var questions = quezap.getQuestions();
    if (questions == null || questions.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Le Quezap \"" + quezap.getTitle() + "\" ne contient aucune question");
    }
    for (final var qq : questions) {
      final var q = qq.getQuestion();
      if (q == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Une des questions du Quezap est invalide");
      }
      if (q.getLabel() == null || q.getLabel().isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "La question #" + qq.getQuestionOrder() + " n'a pas d'énoncé");
      }
      final var answers = q.getAnswers();
      if (answers == null || answers.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La question #" + qq.getQuestionOrder() + " ne comporte aucune proposition de réponse");
      }
      final var type = q.getType() != null ? q.getType().toUpperCase(Locale.ROOT) : "QUIZZ";
      if (!"BOOLEAN".equals(type) && !"AFFIRMATION".equals(type)) {
        if (answers.size() < 2) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "La question #"
                  + qq.getQuestionOrder()
                  + " doit comporter au moins 2 propositions de réponse");
        }
        if (answers.stream().noneMatch(a -> Boolean.TRUE.equals(a.getIsCorrect()))) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "La question #"
                  + qq.getQuestionOrder()
                  + " doit comporter au moins une bonne réponse");
        }
      }
      for (final var a : answers) {
        if (a.getLabel() == null || a.getLabel().isBlank()) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Une des propositions de la question #" + qq.getQuestionOrder() + " est vide");
        }
      }
    }
  }

  private void assertSessionHost(SessionJpaEntity session) {
    final var currentUser = getCurrentUser();
    if (!session.getAuthorId().equals(currentUser.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Seul le créateur peut piloter cette session");
    }
  }
}
