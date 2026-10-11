package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionAnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SessionControllerTest {

  private SessionJpaRepository sessionJpaRepository;
  private QuezapJpaRepository quezapJpaRepository;
  private UserJpaRepository userJpaRepository;
  private SessionSseService sessionSseService;
  private com.quezap.domain.ports.services.UserNameSanitizer userNameSanitizer;
  private SessionController sessionController;
  private final UUID authorId = UUID.randomUUID();
  private UserJpaEntity author;

  @BeforeEach
  void setUp() {
    sessionJpaRepository = Mockito.mock(SessionJpaRepository.class);
    quezapJpaRepository = Mockito.mock(QuezapJpaRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    sessionSseService = Mockito.mock(SessionSseService.class);
    userNameSanitizer = Mockito.mock(com.quezap.domain.ports.services.UserNameSanitizer.class);
    sessionController =
        new SessionController(
            sessionJpaRepository,
            quezapJpaRepository,
            userJpaRepository,
            sessionSseService,
            new com.quezap.api.v1.mappers.PaginationMapper(),
            userNameSanitizer);

    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    author = new UserJpaEntity(authorId, "HostUser", cred, Instant.now(), Instant.now());
    when(userJpaRepository.findById(authorId)).thenReturn(Optional.of(author));
    when(userNameSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

    final var auth = new UsernamePasswordAuthenticationToken(authorId.toString(), null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateSession() {
    final var quezapId = UUID.randomUUID();
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
    question.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), question, "150,000 km/s", null, false, 1));
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "Physics Finals", null, author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 100, 1));
    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));

    final var request = new CreateSessionRequest("Physics Finals", quezapId);

    final var response = sessionController.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.code()).startsWith("QZ-");
    verify(sessionJpaRepository).save(any(SessionJpaEntity.class));
  }

  @Test
  void shouldJoinSessionAndBroadcastLobbyState() {
    final var sessionId = UUID.randomUUID();
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
    question.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), question, "150,000 km/s", null, false, 1));
    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(), "Physics Finals", null, author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 100, 1));

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId, 1042L, "Physics Finals", 0, authorId, quezap, null, null, Instant.now());

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response = sessionController.start(sessionId);

    assertThat(response.status()).isEqualTo("STARTED");
    verify(sessionSseService).broadcast(eq(sessionId), eq("SESSION_STARTED"), eq(1042L), any());
  }

  @Test
  void shouldAdvanceToNextQuestionAndBroadcastQuestionStart() {
    final var sessionId = UUID.randomUUID();

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

    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(), "Physics", null, author, true, Instant.now(), Instant.now());
    final var qq = new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 100, 1);
    quezap.addQuestion(qq);

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId,
            1042L,
            "Physics Finals",
            1,
            authorId,
            quezap,
            Instant.now(),
            null,
            Instant.now());

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
  void shouldAwardPointsWithSpeedBonus() {
    final var sessionId = UUID.randomUUID();
    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(), "Math", "Desc", author, true, Instant.now(), Instant.now());
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Math", Instant.now());
    final var question =
        new QuestionJpaEntity(
            UUID.randomUUID(), "QUIZZ", "What is 5x5?", null, theme, Instant.now(), Instant.now());
    final var ansCorrect =
        new com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity(
            UUID.randomUUID(), question, "25", null, true, 0);
    final var ansWrong =
        new com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity(
            UUID.randomUUID(), question, "10", null, false, 1);
    question.addAnswer(ansCorrect);
    question.addAnswer(ansWrong);

    final var qq = new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 1000, 1);
    quezap.addQuestion(qq);

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId,
            1042L,
            "Speed Quiz",
            1,
            authorId,
            quezap,
            Instant.now().minusSeconds(10),
            null,
            Instant.now());
    sessionEntity.setCurrentSlideStartedAt(Instant.now().minusSeconds(10));

    final var participant =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Speedy", 0, "ptk_speed", false, Instant.now());
    sessionEntity.addParticipant(participant);

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var response =
        sessionController.submitAnswer(sessionId, "ptk_speed", new SubmitAnswerRequest(1, 0));

    assertThat(response.accepted()).isTrue();
    // 20s timer, answered after 10s -> remaining = 10s (50%), points = 1000 * (0.5 + 0.5 * 0.5) =
    // 750
    assertThat(participant.getScore()).isBetween(740, 760);
  }

  @Test
  void shouldKickParticipantAndBroadcastKicked() {
    final var sessionId = UUID.randomUUID();
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

  @Test
  void shouldGetSessionReport() {
    final var sessionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Physics", Instant.now());
    final var q1 =
        new QuestionJpaEntity(
            UUID.randomUUID(),
            "QUIZZ",
            "Speed of light?",
            null,
            theme,
            Instant.now(),
            Instant.now());
    q1.addAnswer(new AnswerJpaEntity(UUID.randomUUID(), q1, "300,000 km/s", null, true, 0));
    q1.addAnswer(new AnswerJpaEntity(UUID.randomUUID(), q1, "150,000 km/s", null, false, 1));

    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(),
            "Physics Quiz",
            "Description",
            author,
            true,
            Instant.now(),
            Instant.now());
    quezap.addQuestion(new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, q1, 20, 1000, 1));

    final var sessionEntity =
        new SessionJpaEntity(
            sessionId,
            1042L,
            "Physics Finals",
            1,
            authorId,
            quezap,
            Instant.now(),
            Instant.now(),
            Instant.now());

    final var p1 =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Alice", 1000, "ptk_1", false, Instant.now());
    final var p2 =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, "Bob", 0, "ptk_2", false, Instant.now());
    sessionEntity.addParticipant(p1);
    sessionEntity.addParticipant(p2);

    sessionEntity.addAnswer(
        new SessionAnswerJpaEntity(
            UUID.randomUUID(), sessionEntity, "Alice", 1, 0, 1000, true, Instant.now()));
    sessionEntity.addAnswer(
        new SessionAnswerJpaEntity(
            UUID.randomUUID(), sessionEntity, "Bob", 1, 1, 0, false, Instant.now()));

    when(sessionJpaRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));

    final var report = sessionController.getReport(sessionId);

    assertThat(report.sessionId()).isEqualTo(sessionId);
    assertThat(report.sessionCode()).isEqualTo("QZ-1042");
    assertThat(report.totalParticipants()).isEqualTo(2);
    assertThat(report.totalQuestions()).isEqualTo(1);
    assertThat(report.averageScore()).isEqualTo(500.0);
    assertThat(report.averageAccuracyRate()).isEqualTo(50.0);
    assertThat(report.participants()).hasSize(2);
    assertThat(report.participants().get(0).participantName()).isEqualTo("Alice");
    assertThat(report.participants().get(0).correctCount()).isEqualTo(1);
    assertThat(report.participants().get(0).accuracyRate()).isEqualTo(100.0);
    assertThat(report.questions()).hasSize(1);
    assertThat(report.questions().get(0).questionLabel()).isEqualTo("Speed of light?");
    assertThat(report.questions().get(0).successRate()).isEqualTo(50.0);
    assertThat(report.questions().get(0).answers()).hasSize(2);
    assertThat(report.questions().get(0).answers().get(0).count()).isEqualTo(1);
  }
}
