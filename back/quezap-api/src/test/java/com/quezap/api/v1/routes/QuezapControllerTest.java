package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.quezaps.CreateQuezapRequest;
import com.quezap.api.v1.dto.request.quezaps.QuezapSlideRequest;
import com.quezap.api.v1.dto.request.quezaps.UpdateQuezapRequest;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.quezaps.QuezapController;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuezapControllerTest {

  private QuezapJpaRepository quezapJpaRepository;
  private QuestionJpaRepository questionJpaRepository;
  private UserJpaRepository userJpaRepository;
  private PaginationMapper paginationMapper;
  private QuezapController quezapController;

  @BeforeEach
  void setUp() {
    quezapJpaRepository = Mockito.mock(QuezapJpaRepository.class);
    questionJpaRepository = Mockito.mock(QuestionJpaRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    paginationMapper = new PaginationMapper();
    quezapController =
        new QuezapController(
            quezapJpaRepository, questionJpaRepository, userJpaRepository, paginationMapper);
  }

  @Test
  void shouldCreateQuezap() {
    final var authorId = UUID.randomUUID();
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author = new UserJpaEntity(authorId, "Teacher", cred, Instant.now(), Instant.now());
    when(userJpaRepository.findAll()).thenReturn(List.of(author));

    final var qId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "History", Instant.now());
    final var question =
        new QuestionJpaEntity(
            qId, "QUIZZ", "What year is it?", null, theme, Instant.now(), Instant.now());
    when(questionJpaRepository.findById(qId)).thenReturn(Optional.of(question));

    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var request =
        new CreateQuezapRequest(
            "Quiz History",
            "Description of quiz",
            true,
            List.of(new QuezapSlideRequest(qId, 30, 100)));

    final var response = quezapController.create(request);

    assertThat(response.id()).isNotNull();
    verify(quezapJpaRepository).save(any(QuezapJpaEntity.class));
  }

  @Test
  void shouldGetQuezapById() {
    final var quezapId = UUID.randomUUID();
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author =
        new UserJpaEntity(UUID.randomUUID(), "Teacher", cred, Instant.now(), Instant.now());
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "History Quiz", "Description", author, true, Instant.now(), Instant.now());

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));

    final var response = quezapController.getById(quezapId);

    assertThat(response.title()).isEqualTo("History Quiz");
    assertThat(response.author().name()).isEqualTo("Teacher");
  }

  @Test
  void shouldListQuezaps() {
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author =
        new UserJpaEntity(UUID.randomUUID(), "Teacher", cred, Instant.now(), Instant.now());
    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(),
            "History Quiz",
            "Description",
            author,
            true,
            Instant.now(),
            Instant.now());

    when(quezapJpaRepository.findByIsPublicTrue(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(quezap)));

    final var response =
        quezapController.list(null, null, new PaginationDto(1L, 10L, null, null, null, null));

    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).title()).isEqualTo("History Quiz");
  }

  @Test
  void shouldUpdateQuezap() {
    final var quezapId = UUID.randomUUID();
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var author =
        new UserJpaEntity(UUID.randomUUID(), "Teacher", cred, Instant.now(), Instant.now());
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "Old Title", "Old Desc", author, false, Instant.now(), Instant.now());

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));

    quezapController.update(quezapId, new UpdateQuezapRequest("New Title", "New Desc", true));

    assertThat(quezap.getTitle()).isEqualTo("New Title");
    assertThat(quezap.getIsPublic()).isTrue();
    verify(quezapJpaRepository).save(quezap);
  }

  @Test
  void shouldDeleteQuezap() {
    final var quezapId = UUID.randomUUID();
    when(quezapJpaRepository.existsById(quezapId)).thenReturn(true);

    quezapController.delete(quezapId);

    verify(quezapJpaRepository).deleteById(quezapId);
  }
}
