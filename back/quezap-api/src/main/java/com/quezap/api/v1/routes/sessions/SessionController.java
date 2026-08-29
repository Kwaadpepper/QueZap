package com.quezap.api.v1.routes.sessions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.AnswerStatDto;
import com.quezap.api.v1.dto.events.IntermediateLeaderboardPayload;
import com.quezap.api.v1.dto.events.LobbyStatePayload;
import com.quezap.api.v1.dto.events.ParticipantDto;
import com.quezap.api.v1.dto.events.ParticipantKickedPayload;
import com.quezap.api.v1.dto.events.QuestionClosedPayload;
import com.quezap.api.v1.dto.events.QuestionRevealPayload;
import com.quezap.api.v1.dto.events.QuestionStartPayload;
import com.quezap.api.v1.dto.events.SafeAnswerDto;
import com.quezap.api.v1.dto.events.SafeQuestionDto;
import com.quezap.api.v1.dto.events.SessionStartedPayload;
import com.quezap.api.v1.dto.request.sessions.CreateSessionRequest;
import com.quezap.api.v1.dto.request.sessions.JoinSessionRequest;
import com.quezap.api.v1.dto.request.sessions.SubmitAnswerRequest;
import com.quezap.api.v1.dto.response.sessions.AnswerSubmittedDto;
import com.quezap.api.v1.dto.response.sessions.JoinSessionResponseDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.NextQuestionResponseDto;
import com.quezap.api.v1.dto.response.sessions.SessionCreatedDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.api.v1.dto.response.sessions.SessionStatusDto;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
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

  public SessionController(
      SessionJpaRepository sessionJpaRepository,
      QuezapJpaRepository quezapJpaRepository,
      UserJpaRepository userJpaRepository,
      SessionSseService sessionSseService) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.quezapJpaRepository = quezapJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.sessionSseService = sessionSseService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SessionCreatedDto create(@Valid @RequestBody CreateSessionRequest request) {
    final var author = getCurrentUser();
    final var sessionNumber = generateUniqueSessionNumber();

    QuezapJpaEntity quezapEntity = null;
    if (request.quezapId() != null) {
      quezapEntity =
          quezapJpaRepository
              .findById(request.quezapId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "Quezap not found: " + request.quezapId()));
    }

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
  public SessionDetailDto getById(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

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
        sessionJpa.getAuthorId());
  }

  @PostMapping("join")
  public JoinSessionResponseDto join(@Valid @RequestBody JoinSessionRequest request) {
    final var sessionNumber = parseSessionCode(request.code());
    final var sessionJpa =
        sessionJpaRepository
            .findByNumber(sessionNumber)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid session code: " + request.code()));

    final var existingParticipant =
        sessionJpa.getParticipants().stream()
            .filter(p -> p.getName().equalsIgnoreCase(request.participantName()))
            .findFirst();

    if (existingParticipant.isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Participant name is already taken in this session");
    }

    final var token = "ptk_" + UUID.randomUUID().toString();
    final var participant =
        new SessionParticipantJpaEntity(
            UuidV7.randomUuid(),
            sessionJpa,
            request.participantName(),
            0,
            token,
            false,
            Instant.now());

    sessionJpa.addParticipant(participant);
    sessionJpaRepository.save(sessionJpa);

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

    return new JoinSessionResponseDto(sessionJpa.getId(), token, request.participantName());
  }

  @PostMapping("{id}/start")
  public SessionStatusDto start(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    if (sessionJpa.getStartedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session has already started");
    }

    sessionJpa.setStartedAt(Instant.now());
    sessionJpaRepository.save(sessionJpa);

    final var quezap = sessionJpa.getQuezap();
    final var totalSlides = quezap != null ? quezap.getQuestions().size() : 0;

    final var startedAt =
        sessionJpa.getStartedAt() != null ? sessionJpa.getStartedAt() : Instant.now();
    final var payload = new SessionStartedPayload(startedAt, totalSlides, 3);
    sessionSseService.broadcast(
        sessionJpa.getId(), "SESSION_STARTED", sessionJpa.getNumber(), payload);

    return new SessionStatusDto("STARTED");
  }

  @PostMapping("{id}/next-question")
  public NextQuestionResponseDto nextQuestion(@PathVariable UUID id) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    final var quezap = sessionJpa.getQuezap();
    if (quezap == null || quezap.getQuestions().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Session does not contain questions");
    }

    final var questionsList = quezap.getQuestions().stream().toList();
    final var nextIndex = sessionJpa.getCurrentSlideIndex() + 1;
    final var totalSlides = questionsList.size();

    if (nextIndex > totalSlides) {
      sessionJpa.setEndedAt(Instant.now());
      sessionJpaRepository.save(sessionJpa);
      return new NextQuestionResponseDto(sessionJpa.getCurrentSlideIndex(), totalSlides);
    }

    sessionJpa.setCurrentSlideIndex(nextIndex);
    sessionJpaRepository.save(sessionJpa);

    // Get current slide question details for SSE (Safe without isCorrect)
    final var currentQq = questionsList.get(Math.min(nextIndex - 1, totalSlides - 1));
    final var question = currentQq.getQuestion();

    var answerIndex = 0;
    final var safeAnswers = new ArrayList<SafeAnswerDto>();
    for (final var a : question.getAnswers()) {
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
  public AnswerSubmittedDto submitAnswer(
      @PathVariable UUID id,
      @RequestHeader(value = "X-Participant-Token", required = false) @Nullable String headerToken,
      @Valid @RequestBody SubmitAnswerRequest request) {

    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

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

    // Check duplicate answer for this slide
    final var existingAnswer =
        sessionJpa.getAnswers().stream()
            .filter(
                a ->
                    a.getParticipantName().equals(participant.getName())
                        && a.getSlideIndex().equals(request.slideIndex()))
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
      final var questions = quezap.getQuestions().stream().toList();
      final var slideIdx = request.slideIndex() - 1;
      if (slideIdx >= 0 && slideIdx < questions.size()) {
        final var qq = questions.get(slideIdx);
        final var answers = qq.getQuestion().getAnswers().stream().toList();
        if (request.answerIndex() >= 0 && request.answerIndex() < answers.size()) {
          final var answerEntity = answers.get(request.answerIndex());
          isCorrect = Boolean.TRUE.equals(answerEntity.getIsCorrect());
          if (isCorrect) {
            pointsAwarded = qq.getPoints();
          }
        }
      }
    }

    final var answerEntity =
        new SessionAnswerJpaEntity(
            UuidV7.randomUuid(),
            sessionJpa,
            participant.getName(),
            request.slideIndex(),
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
                .filter(a -> a.getSlideIndex().equals(request.slideIndex()))
                .count();

    if (totalAnswered >= totalActive && totalActive > 0) {
      // Broadcast QUESTION_CLOSED
      final var closedPayload =
          new QuestionClosedPayload(
              request.slideIndex(), totalAnswered, totalActive, "ALL_PARTICIPANTS_ANSWERED");
      sessionSseService.broadcast(
          sessionJpa.getId(), "QUESTION_CLOSED", sessionJpa.getNumber(), closedPayload);

      // Broadcast QUESTION_REVEAL
      broadcastRevealAndLeaderboard(sessionJpa, request.slideIndex());
    }

    return new AnswerSubmittedDto(true, Instant.now());
  }

  @GetMapping("{id}/scores")
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

  @PostMapping("{id}/participants/{name}/kick")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void kickParticipant(@PathVariable UUID id, @PathVariable String name) {
    final var sessionJpa =
        sessionJpaRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

    final var participant =
        sessionJpa.getParticipants().stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));

    participant.setIsKicked(true);
    sessionJpaRepository.save(sessionJpa);

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

  @GetMapping(value = "{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(@PathVariable UUID id) {
    if (!sessionJpaRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
    return sessionSseService.registerEmitter(id);
  }

  private void broadcastRevealAndLeaderboard(SessionJpaEntity sessionJpa, int slideIndex) {
    final var quezap = sessionJpa.getQuezap();
    if (quezap == null) {
      return;
    }

    final var questions = quezap.getQuestions().stream().toList();
    final var slideIdx = slideIndex - 1;
    if (slideIdx < 0 || slideIdx >= questions.size()) {
      return;
    }

    final var qq = questions.get(slideIdx);
    final var answers = qq.getQuestion().getAnswers().stream().toList();
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
    return ThreadLocalRandom.current().nextLong(1000, 99999);
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

    return userJpaRepository.findAll().stream()
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No authenticated user available"));
  }
}
