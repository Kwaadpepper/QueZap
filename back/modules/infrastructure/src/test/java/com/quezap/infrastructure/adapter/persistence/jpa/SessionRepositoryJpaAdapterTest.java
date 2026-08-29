package com.quezap.infrastructure.adapter.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.SessionRepositoryJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.SessionJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SessionRepositoryJpaAdapterTest {

  private SessionJpaRepository sessionJpaRepository;
  private UserJpaRepository userJpaRepository;
  private QuestionJpaRepository questionJpaRepository;
  private SessionJpaMapper sessionJpaMapper;
  private SessionRepositoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    sessionJpaRepository = Mockito.mock(SessionJpaRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    questionJpaRepository = Mockito.mock(QuestionJpaRepository.class);
    sessionJpaMapper = new SessionJpaMapper();
    adapter =
        new SessionRepositoryJpaAdapter(
            sessionJpaRepository, userJpaRepository, questionJpaRepository, sessionJpaMapper);
  }

  private QuezapJpaEntity createSampleQuezap() {
    final var cred =
        new CredentialJpaEntity(
            UuidV7.randomUuid(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author =
        new UserJpaEntity(UuidV7.randomUuid(), "Teacher", cred, Instant.now(), Instant.now());
    final var theme = new ThemeJpaEntity(UuidV7.randomUuid(), "Physics", Instant.now());
    final var question =
        new QuestionJpaEntity(
            UuidV7.randomUuid(),
            "QUIZZ",
            "Speed of light?",
            null,
            theme,
            Instant.now(),
            Instant.now());
    question.addAnswer(
        new AnswerJpaEntity(UuidV7.randomUuid(), question, "300,000 km/s", null, true, 0));

    final var quezap =
        new QuezapJpaEntity(
            UuidV7.randomUuid(), "Physics", null, author, true, Instant.now(), Instant.now());
    final var qq = new QuezapQuestionJpaEntity(UuidV7.randomUuid(), quezap, question, 20, 100, 1);
    quezap.addQuestion(qq);
    return quezap;
  }

  @Test
  void shouldPersistSession() {
    final var authorId = UuidV7.randomUuid();
    final var questionId = UuidV7.randomUuid();

    final var session =
        new Session(
            new SessionName("Session Test"),
            new SessionNumber(1042L),
            0,
            Set.of(new QuestionSlide(30, 100, new QuestionId(questionId))),
            new HashSet<>(),
            new HashSet<>(),
            new UserId(authorId));

    adapter.persist(session);

    verify(sessionJpaRepository).save(any(SessionJpaEntity.class));
  }

  @Test
  void shouldFindSessionById() {
    final var id = UuidV7.randomUuid();
    final var authorId = UuidV7.randomUuid();
    final var quezap = createSampleQuezap();
    final var entity =
        new SessionJpaEntity(
            id, 1042L, "Session Test", 0, authorId, quezap, null, null, Instant.now());

    when(sessionJpaRepository.findById(id)).thenReturn(Optional.of(entity));

    final var result = adapter.find(new SessionId(id));

    assertThat(result).isPresent();
    assertThat(result.get().getNumber().value()).isEqualTo(1042L);
    assertThat(result.get().getName().value()).isEqualTo("Session Test");
  }

  @Test
  void shouldFindSessionByNumber() {
    final var id = UuidV7.randomUuid();
    final var authorId = UuidV7.randomUuid();
    final var quezap = createSampleQuezap();
    final var entity =
        new SessionJpaEntity(
            id, 1042L, "Session Test", 0, authorId, quezap, null, null, Instant.now());

    when(sessionJpaRepository.findByNumber(1042L)).thenReturn(Optional.of(entity));

    final var result = adapter.findByNumber(new SessionNumber(1042L));

    assertThat(result).isPresent();
    assertThat(result.get().getId().value()).isEqualTo(id);
  }
}
