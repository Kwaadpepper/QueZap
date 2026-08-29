package com.quezap.infrastructure.adapter.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.QuestionRepositoryJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.QuestionJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuestionRepositoryJpaAdapterTest {

  private QuestionJpaRepository questionJpaRepository;
  private ThemeJpaRepository themeJpaRepository;
  private QuestionJpaMapper questionJpaMapper;
  private QuestionRepositoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    questionJpaRepository = Mockito.mock(QuestionJpaRepository.class);
    themeJpaRepository = Mockito.mock(ThemeJpaRepository.class);
    questionJpaMapper = new QuestionJpaMapper();
    adapter =
        new QuestionRepositoryJpaAdapter(
            questionJpaRepository, themeJpaRepository, questionJpaMapper);
  }

  @Test
  void shouldPersistQuestion() {
    final var themeId = UuidV7.randomUuid();
    final var themeEntity = new ThemeJpaEntity(themeId, "History", Instant.now());
    when(themeJpaRepository.findById(themeId)).thenReturn(Optional.of(themeEntity));

    final var question =
        new Question(
            QuestionType.QUIZZ,
            "What year did World War II end?",
            null,
            new ThemeId(themeId),
            Set.of(
                new Answer("1945", null, true),
                new Answer("1944", null, false),
                new Answer("1939", null, false)));

    adapter.persist(question);

    verify(questionJpaRepository).save(any(QuestionJpaEntity.class));
  }

  @Test
  void shouldFindQuestionById() {
    final var qId = UuidV7.randomUuid();
    final var themeEntity = new ThemeJpaEntity(UuidV7.randomUuid(), "History", Instant.now());
    final var entity =
        new QuestionJpaEntity(
            qId,
            "QUIZZ",
            "What year did World War II end?",
            null,
            themeEntity,
            Instant.now(),
            Instant.now());

    entity.addAnswer(new AnswerJpaEntity(UuidV7.randomUuid(), entity, "1945", null, true, 0));
    entity.addAnswer(new AnswerJpaEntity(UuidV7.randomUuid(), entity, "1944", null, false, 1));
    entity.addAnswer(new AnswerJpaEntity(UuidV7.randomUuid(), entity, "1939", null, false, 2));

    when(questionJpaRepository.findById(qId)).thenReturn(Optional.of(entity));

    final var result = adapter.find(new QuestionId(qId));

    assertThat(result).isPresent();
    assertThat(result.get().getValue()).isEqualTo("What year did World War II end?");
    assertThat(result.get().getAnswers()).hasSize(3);
  }

  @Test
  void shouldCountWithThemes() {
    final var themeId = UuidV7.randomUuid();
    when(questionJpaRepository.countByThemeIdIn(any())).thenReturn(5L);

    final var count = adapter.countWithThemes(Set.of(new ThemeId(themeId)));

    assertThat(count).isEqualTo(5L);
  }

  @Test
  void shouldPaginateSearching() {
    final var themeEntity = new ThemeJpaEntity(UuidV7.randomUuid(), "History", Instant.now());
    final var entity =
        new QuestionJpaEntity(
            UuidV7.randomUuid(),
            "QUIZZ",
            "What year did World War II end?",
            null,
            themeEntity,
            Instant.now(),
            Instant.now());

    when(questionJpaRepository.findByLabelContainingIgnoreCase(
            eq("World War"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    final var result =
        adapter.paginateSearching(Pagination.firstPage(10), new SearchQuery("World War"));

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).question()).contains("World War");
  }
}
