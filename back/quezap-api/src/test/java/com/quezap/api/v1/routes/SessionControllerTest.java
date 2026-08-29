package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quezap.api.v1.dto.request.sessions.CreateSessionRequest;
import com.quezap.api.v1.dto.request.sessions.JoinSessionRequest;
import com.quezap.api.v1.dto.request.sessions.SubmitAnswerRequest;
import com.quezap.api.v1.routes.sessions.SessionController;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SessionControllerTest {

  private SessionJpaRepository sessionJpaRepository;
  private QuezapJpaRepository quezapJpaRepository;
  private UserJpaRepository userJpaRepository;
  private SessionSseService sessionSseService;
  private SessionController sessionController;

  @BeforeEach
  void setUp() {
    sessionJpaRepository = Mockito.mock(SessionJpaRepository.class);
    quezapJpaRepository = Mockito.mock(QuezapJpaRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    sessionSseService = Mockito.mock(SessionSseService.class);
    sessionController =
        new SessionController(
            sessionJpaRepository,
            quezapJpaRepository,
            userJpaRepository,
            sessionSseService,
            new com.quezap.api.v1.mappers.PaginationMapper());
  }

  @Test
  void shouldCreateSession() {
    final var authorId = UUID.randomUUID();
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author = new UserJpaEntity(authorId, "HostUser", cred, Instant.now(), Instant.now());
    when(userJpaRepository.findAll()).thenReturn(List.of(author));

    final var request = new CreateSessionRequest("Physics Finals", null);

    final var response = sessionController.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.code()).startsWith("QZ-");
    verify(sessionJpaRepository).save(any(SessionJpaEntity.class));
  }

  @Test
  void shouldJoinSessionAndBroadcastLobbyState() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();
    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 0, authorId, null, null, null, Instant.now());

    when(sessionJpaRepository.findByNumber(1042L)).thenReturn(Optional.of(sessionEntity));

    final var response = sessionController.join(new JoinSessionRequest("QZ-1042", "Arthur"));

    assertThat(response.sessionId()).isEqualTo(sessionId);
    assertThat(response.participantName()).isEqualTo("Arthur");
    assertThat(response.participationToken()).startsWith("ptk_");

    verify(sessionSseService).broadcast(eq(sessionId), eq("LOBBY_STATE"), eq(1042L), any());
  }

  @Test
  void shouldStartSessionAndBroadcastSessionStarted() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();
    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 0, authorId, null, null, null, Instant.now());

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response = sessionController.start(sessionId);

    assertThat(response.status()).isEqualTo("STARTED");
    verify(sessionSseService).broadcast(eq(sessionId), eq("SESSION_STARTED"), eq(1042L), any());
  }

  @Test
  void shouldAdvanceToNextQuestionAndBroadcastQuestionStart() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();

    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Physics", Instant.now());
    final var question =
        new QuestionJpaEntity(
            UUID.randomUUID(),
            "QUIZZ",
            "Speed of light?",
            null,
            theme,
            Instant.now(),
            Instant.now());
    question.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), question, "300,000 km/s", null, true, 0));

    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author = new UserJpaEntity(authorId, "Teacher", cred, Instant.now(), Instant.now());

    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(), "Physics", null, author, true, Instant.now(), Instant.now());
    final var qq = new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 100, 1);
    quezap.addQuestion(qq);

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 0, authorId, quezap, null, null, Instant.now());

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response = sessionController.nextQuestion(sessionId);

    assertThat(response.currentSlideIndex()).isEqualTo(1);
    assertThat(response.totalSlides()).isEqualTo(1);

    verify(sessionSseService).broadcast(eq(sessionId), eq("QUESTION_START"), eq(1042L), any());
  }

  @Test
  void shouldSubmitAnswerAndCalculateScore() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();

    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Physics", Instant.now());
    final var question =
        new QuestionJpaEntity(
            UUID.randomUUID(),
            "QUIZZ",
            "Speed of light?",
            null,
            theme,
            Instant.now(),
            Instant.now());
    question.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), question, "300,000 km/s", null, true, 0));

    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author = new UserJpaEntity(authorId, "Teacher", cred, Instant.now(), Instant.now());

    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(), "Physics", null, author, true, Instant.now(), Instant.now());
    final var qq = new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 100, 1);
    quezap.addQuestion(qq);

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 1, authorId, quezap, null, null, Instant.now());

    final var participant =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Arthur", 0, "ptk_123", false, Instant.now());
    sessionEntity.addParticipant(participant);

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response =
        sessionController.submitAnswer(sessionId, "ptk_123", new SubmitAnswerRequest(1, 0));

    assertThat(response.accepted()).isTrue();
    assertThat(participant.getScore()).isEqualTo(100);
  }

  @Test
  void shouldKickParticipantAndBroadcastKicked() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();
    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 0, authorId, null, null, null, Instant.now());

    final var participant =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "BadPlayer", 0, "ptk_bad", false, Instant.now());
    sessionEntity.addParticipant(participant);

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    sessionController.kickParticipant(sessionId, "BadPlayer");

    assertThat(participant.getIsKicked()).isTrue();
    verify(sessionSseService).broadcast(eq(sessionId), eq("PARTICIPANT_KICKED"), eq(1042L), any());
  }

  @Test
  void shouldGetScoresLeaderboard() {
    final var sessionId = UUID.randomUUID();
    final var authorId = UUID.randomUUID();
    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 1, authorId, null, null, null, Instant.now());

    final var p1 =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Alice", 250, "ptk_1", false, Instant.now());
    final var p2 =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Bob", 150, "ptk_2", false, Instant.now());
    sessionEntity.addParticipant(p1);
    sessionEntity.addParticipant(p2);

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response = sessionController.getScores(sessionId);

    assertThat(response.leaderboard()).hasSize(2);
    assertThat(response.leaderboard().get(0).participantName()).isEqualTo("Alice");
    assertThat(response.leaderboard().get(0).score()).isEqualTo(250);
  }
}
