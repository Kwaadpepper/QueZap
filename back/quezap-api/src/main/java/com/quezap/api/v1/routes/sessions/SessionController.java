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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.quezap.api.v1.dto.response.sessions.QuestionAnswerStatDto;
import com.quezap.api.v1.dto.response.sessions.QuestionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionCreatedDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.api.v1.dto.response.sessions.SessionParticipantReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionStatusDto;
import com.quezap.api.v1.mappers.PaginationMapper;
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
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.utils.UuidV7;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/sessions")
public class SessionController {

  private final SessionJpaRepository sessionJpaRepository;
  private final QuezapJpaRepository quezapJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final SessionSseService sessionSseService;
  private final PaginationMapper paginationMapper;
  private final UserNameSanitizer userNameSanitizer;

  public SessionController(
      SessionJpaRepository sessionJpaRepository,
      QuezapJpaRepository quezapJpaRepository,
      UserJpaRepository userJpaRepository,
      SessionSseService sessionSseService,
      PaginationMapper paginationMapper,
      UserNameSanitizer userNameSanitizer) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.quezapJpaRepository = quezapJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.sessionSseService = sessionSseService;
    this.paginationMapper = paginationMapper;
    this.userNameSanitizer = userNameSanitizer;
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
      page = sessionJpaRepository.findByAuthorId(authorId, pageRequest);
    } else {
      final var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() != null) {
        try {
          final var userUuid = UUID.fromString(auth.getPrincipal().toString());
          page = sessionJpaRepository.findByAuthorId(userUuid, pageRequest);
        } catch (IllegalArgumentException _) {
          page = sessionJpaRepository.findAll(pageRequest);
        }
      } else {
        page = sessionJpaRepository.findAll(pageRequest);
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
                      sessionJpa.getAuthorId());
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
        quezapJpaRepository
            .findById(request.quezapId())
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

    sessionJpaRepository.save(sessionJpa);

    return new SessionCreatedDto(
        sessionJpa.getId(), sessionNumber, formatSessionCode(sessionNumber));
  }

  @GetMapping("{id}")
  @Transactional(readOnly = true)
  public SessionDetailDto getById(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
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
        sessionJpa.getAuthorId());
  }

  @GetMapping("by-code/{code}")
  @Transactional(readOnly = true)
  public SessionDetailDto getByCode(@PathVariable String code) {
    final var sessionNumber = parseSessionCode(code);
    final var sessionJpa =
        sessionJpaRepository
            .findByNumber(sessionNumber)
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
        sessionJpa.getAuthorId());
  }

  @PostMapping("join")
  @Transactional
  public JoinSessionResponseDto join(@Valid @RequestBody JoinSessionRequest request) {
    final var sessionNumber = parseSessionCode(request.code());
    final var sessionJpa =
        sessionJpaRepository
            .findByNumber(sessionNumber)
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
    sessionJpaRepository.saveAndFlush(sessionJpa);

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
        sessionJpaRepository
            .findById(id)
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
    sessionJpaRepository.saveAndFlush(sessionJpa);

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

      for (final var a : sortedAnswers) {
        safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
      }

      final var safeQuestion =
          new SafeQuestionDto(
              question.getId(),
              question.getType(),
              question.getLabel(),
              question.getPictureFilename(),
              safeAnswers);

      final var startPayload =
          new QuestionStartPayload(
              1, totalSlides, currentQq.getTimerSeconds(), currentQq.getPoints(), safeQuestion);

      sessionSseService.broadcast(
          sessionJpa.getId(), "QUESTION_START", sessionJpa.getNumber(), startPayload);
    }

    return new SessionStatusDto("STARTED");
  }

  @PostMapping("{id}/next-question")
  @Transactional
  public NextQuestionResponseDto nextQuestion(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
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
      sessionJpaRepository.saveAndFlush(sessionJpa);

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

      final var endedPayload = new SessionEndedPayload(endedAt, finalLeaderboard, podium);
      sessionSseService.broadcast(
          sessionJpa.getId(), "SESSION_ENDED", sessionJpa.getNumber(), endedPayload);

      return new NextQuestionResponseDto(sessionJpa.getCurrentSlideIndex(), totalSlides);
    }

    sessionJpa.setCurrentSlideIndex(nextIndex);
    sessionJpa.setCurrentSlideStartedAt(Instant.now());
    sessionJpaRepository.saveAndFlush(sessionJpa);

    // Get current slide question details for SSE (Safe without isCorrect)
    final var currentQq = questionsList.get(Math.min(nextIndex - 1, totalSlides - 1));
    final var question = currentQq.getQuestion();

    var answerIndex = 0;
    final var safeAnswers = new ArrayList<SafeAnswerDto>();
    final var sortedAnswers =
        question.getAnswers().stream()
            .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
            .toList();

    for (final var a : sortedAnswers) {
      safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
    }

    final var safeQuestion =
        new SafeQuestionDto(
            question.getId(),
            question.getType(),
            question.getLabel(),
            question.getPictureFilename(),
            safeAnswers);

    final var startPayload =
        new QuestionStartPayload(
            nextIndex,
            totalSlides,
            currentQq.getTimerSeconds(),
            currentQq.getPoints(),
            safeQuestion);

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
        sessionJpaRepository
            .findById(id)
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
        final var answers =
            qq.getQuestion().getAnswers().stream()
                .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
                .toList();
        if (request.answerIndex() >= 0 && request.answerIndex() < answers.size()) {
          final var answerEntity = answers.get(request.answerIndex());
          isCorrect = Boolean.TRUE.equals(answerEntity.getIsCorrect());
          if (isCorrect) {
            final var slideStartedAt =
                sessionJpa.getCurrentSlideStartedAt() != null
                    ? sessionJpa.getCurrentSlideStartedAt()
                    : sessionJpa.getStartedAt();
            final var elapsedMillis =
                slideStartedAt != null
                    ? Math.max(0, Duration.between(slideStartedAt, Instant.now()).toMillis())
                    : 0L;
            final var timerSeconds = qq.getTimerSeconds() != null ? qq.getTimerSeconds() : 30;
            final var basePoints = qq.getPoints() != null ? qq.getPoints() : 1000;
            pointsAwarded = Session.calculateScore(basePoints, timerSeconds, elapsedMillis, true);
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
    sessionJpaRepository.save(sessionJpa);

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
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);
    final var currentSlide = Math.max(sessionJpa.getCurrentSlideIndex(), 1);
    broadcastRevealAndLeaderboard(sessionJpa, currentSlide);
  }

  @GetMapping(value = "{id}/export/csv", produces = "text/csv")
  @Transactional(readOnly = true)
  public ResponseEntity<String> exportCsv(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);

    final var participants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    final var totalQuestions =
        sessionJpa.getQuezap() != null ? sessionJpa.getQuezap().getQuestions().size() : 0;

    final var sb = new StringBuilder();
    sb.append("Rang,Pseudo,Score Total,Bonnes Réponses,Total Questions,Taux de Réussite (%)\n");

    var rank = 1;
    for (final var p : participants) {
      final var correctCount =
          (int)
              sessionJpa.getAnswers().stream()
                  .filter(
                      a ->
                          a.getParticipantName().equals(p.getName())
                              && Boolean.TRUE.equals(a.getIsCorrect()))
                  .count();
      final var accuracy =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      sb.append(
          String.format(
              Locale.US,
              "%d,%s,%d,%d,%d,%.1f%%\n",
              rank++,
              p.getName(),
              p.getScore(),
              correctCount,
              totalQuestions,
              accuracy));
    }

    final var filename = "session-" + sessionJpa.getNumber() + "-resultats.csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(sb.toString());
  }

  @GetMapping("{id}/scores")
  @Transactional(readOnly = true)
  public LeaderboardResponseDto getScores(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    final var participants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    var rank = 1;
    final var leaderboard = new ArrayList<LeaderboardEntryDto>();
    for (final var p : participants) {
      leaderboard.add(new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1));
    }

    return new LeaderboardResponseDto(
        sessionJpa.getId(), sessionJpa.getCurrentSlideIndex(), leaderboard);
  }

  @GetMapping("{id}/report")
  @Transactional(readOnly = true)
  public SessionReportDto getReport(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    assertSessionHost(sessionJpa);

    final var quezap = sessionJpa.getQuezap();
    final var questionsList =
        quezap != null
            ? quezap.getQuestions().stream()
                .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
                .toList()
            : List.<QuezapQuestionJpaEntity>of();

    final var totalQuestions = questionsList.size();

    final var activeParticipants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    final var totalParticipants = activeParticipants.size();

    var rank = 1;
    final var participantReports = new ArrayList<SessionParticipantReportDto>();
    var totalScoreSum = 0L;
    var totalAccuracySum = 0.0;

    for (final var p : activeParticipants) {
      final var correctCount =
          (int)
              sessionJpa.getAnswers().stream()
                  .filter(
                      a ->
                          a.getParticipantName().equals(p.getName())
                              && Boolean.TRUE.equals(a.getIsCorrect()))
                  .count();
      final var accuracyRate =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      totalScoreSum += p.getScore();
      totalAccuracySum += accuracyRate;

      participantReports.add(
          new SessionParticipantReportDto(
              rank++,
              p.getName(),
              p.getScore(),
              correctCount,
              totalQuestions,
              Math.round(accuracyRate * 10.0) / 10.0));
    }

    final var averageScore =
        totalParticipants > 0 ? (double) totalScoreSum / totalParticipants : 0.0;
    final var averageAccuracyRate =
        totalParticipants > 0 ? totalAccuracySum / totalParticipants : 0.0;

    final var questionReports = new ArrayList<QuestionReportDto>();
    for (var idx = 0; idx < questionsList.size(); idx++) {
      final var qq = questionsList.get(idx);
      final var slideIndex = idx + 1;
      final var q = qq.getQuestion();
      final var answersList =
          q.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .toList();

      final var slideAnswers =
          sessionJpa.getAnswers().stream()
              .filter(a -> a.getSlideIndex().equals(slideIndex))
              .toList();

      final var slideTotalAnswers = slideAnswers.size();
      final var slideCorrectAnswers =
          (int) slideAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
      final var successRate =
          slideTotalAnswers > 0 ? (double) slideCorrectAnswers / slideTotalAnswers * 100.0 : 0.0;

      final var answerStats = new ArrayList<QuestionAnswerStatDto>();
      for (var answerIndex = 0; answerIndex < answersList.size(); answerIndex++) {
        final var ans = answersList.get(answerIndex);
        final var targetIndex = answerIndex;
        final var count =
            (int)
                slideAnswers.stream().filter(sa -> sa.getAnswerIndex().equals(targetIndex)).count();
        final var percentage =
            slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
        answerStats.add(
            new QuestionAnswerStatDto(
                answerIndex,
                ans.getLabel() != null ? ans.getLabel() : "",
                Boolean.TRUE.equals(ans.getIsCorrect()),
                count,
                Math.round(percentage * 10.0) / 10.0));
      }

      questionReports.add(
          new QuestionReportDto(
              slideIndex,
              q.getLabel() != null ? q.getLabel() : "",
              q.getType() != null ? q.getType() : "QUIZZ",
              slideTotalAnswers,
              slideCorrectAnswers,
              Math.round(successRate * 10.0) / 10.0,
              answerStats));
    }

    return new SessionReportDto(
        sessionJpa.getId(),
        sessionJpa.getNumber(),
        formatSessionCode(sessionJpa.getNumber()),
        sessionJpa.getName(),
        sessionJpa.getStartedAt(),
        sessionJpa.getEndedAt(),
        totalParticipants,
        totalQuestions,
        Math.round(averageScore * 10.0) / 10.0,
        Math.round(averageAccuracyRate * 10.0) / 10.0,
        participantReports,
        questionReports);
  }

  @PostMapping("{id}/participants/{name}/kick")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void kickParticipant(@PathVariable UUID id, @PathVariable String name) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
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
    sessionJpaRepository.saveAndFlush(sessionJpa);

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
      @Nullable QuestionStartPayload startPayload) {}

  @Transactional(readOnly = true)
  protected InitialEventsData getInitialStreamData(UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
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

        var answerIndex = 0;
        final var safeAnswers = new ArrayList<SafeAnswerDto>();
        final var sortedAnswers =
            question.getAnswers().stream()
                .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
                .toList();

        for (final var a : sortedAnswers) {
          safeAnswers.add(new SafeAnswerDto(answerIndex++, a.getLabel(), a.getPictureFilename()));
        }

        final var safeQuestion =
            new SafeQuestionDto(
                question.getId(),
                question.getType(),
                question.getLabel(),
                question.getPictureFilename(),
                safeAnswers);

        startPayload =
            new QuestionStartPayload(
                nextIndex,
                questionsList.size(),
                currentQq.getTimerSeconds(),
                currentQq.getPoints(),
                safeQuestion);
      }
    }

    return new InitialEventsData(sessionJpa.getNumber(), lobbyPayload, startPayload);
  }

  @GetMapping(value = "{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(@PathVariable UUID id) {
    final var initialData = getInitialStreamData(id);
    final var emitter = sessionSseService.registerEmitter(id);

    sessionSseService.sendToEmitter(
        emitter, "LOBBY_STATE", initialData.sessionNumber(), id, initialData.lobbyPayload());

    if (initialData.startPayload() != null) {
      sessionSseService.sendToEmitter(
          emitter, "QUESTION_START", initialData.sessionNumber(), id, initialData.startPayload());
    }

    return emitter;
  }

  private void broadcastRevealAndLeaderboard(SessionJpaEntity sessionJpa, int slideIndex) {
    final var quezap = sessionJpa.getQuezap();
    if (quezap == null) {
      return;
    }

    final var questions =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    final var slideIdx = slideIndex - 1;
    if (slideIdx < 0 || slideIdx >= questions.size()) {
      return;
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

    for (var i = 0; i < answers.size(); i++) {
      final var a = answers.get(i);
      if (Boolean.TRUE.equals(a.getIsCorrect())) {
        correctIndices.add(i);
      }
      final var answerIndex = i;
      final var count =
          (int) slideAnswers.stream().filter(sa -> sa.getAnswerIndex().equals(answerIndex)).count();
      final var percentage = (double) count / totalCount * 100.0;
      stats.add(new AnswerStatDto(i, count, percentage));
    }

    final var revealPayload = new QuestionRevealPayload(slideIndex, correctIndices, null, stats);
    sessionSseService.broadcast(
        sessionJpa.getId(), "QUESTION_REVEAL", sessionJpa.getNumber(), revealPayload);

    // Intermediate leaderboard
    final var sortedParticipants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    var rank = 1;
    final var leaderboard = new ArrayList<LeaderboardEntryDto>();
    for (final var p : sortedParticipants) {
      leaderboard.add(new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1));
    }

    final var leaderboardPayload = new IntermediateLeaderboardPayload(slideIndex, leaderboard);
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
      if (!sessionJpaRepository.existsByNumber(number)) {
        return number;
      }
    }
    return ThreadLocalRandom.current().nextLong(100000, 999999);
  }

  private String formatSessionCode(Long number) {
    return "QZ-" + number;
  }

  private Long parseSessionCode(String code) {
    var cleaned = code.trim();
    if (cleaned.toUpperCase(Locale.ROOT).startsWith("QZ-")) {
      cleaned = cleaned.substring(3);
    }
    try {
      return Long.parseLong(cleaned);
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
