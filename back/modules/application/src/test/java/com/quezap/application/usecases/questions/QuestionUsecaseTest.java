package com.quezap.application.usecases.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.quezap.application.ports.questions.AddQuestion;
import com.quezap.application.ports.questions.ListQuestions;
import com.quezap.application.ports.questions.RemoveQuestion;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionForbiddenException;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionInUseException;
import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.domain.ports.directories.QuestionDirectory;
import com.quezap.domain.ports.directories.views.QuestionView;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuestionUsecaseTest {

  private QuestionRepository questionRepository;
  private QuestionDirectory questionDirectory;
  private ThemeRepository themeRepository;
  private QuestionPictureManager pictureManager;
  private QuezapDataPort quezapDataPort;
  private UnitOfWorkEvents unitOfWork;

  private AddQuestionHandler addHandler;
  private ListQuestionsHandler listHandler;
  private RemoveQuestionHandler removeHandler;

  @BeforeEach
  void setUp() {
    questionRepository = mock(QuestionRepository.class);
    questionDirectory = mock(QuestionDirectory.class);
    themeRepository = mock(ThemeRepository.class);
    pictureManager = mock(QuestionPictureManager.class);
    quezapDataPort = mock(QuezapDataPort.class);
    unitOfWork = mock(UnitOfWorkEvents.class);

    addHandler = new AddQuestionHandler(questionRepository, themeRepository, pictureManager);
    listHandler = new ListQuestionsHandler(questionDirectory);
    removeHandler = new RemoveQuestionHandler(questionRepository, quezapDataPort);
  }

  @Test
  void addQuestionShouldPersistAuthorId() {
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var authorId = new UserId(UuidV7.randomUuid());
    when(themeRepository.find(themeId))
        .thenReturn(Optional.of(Theme.hydrate(themeId, new ThemeName("Science"))));

    final var input =
        new AddQuestion.Input.Affirmation(
            "Is Earth the third planet from the Sun?", true, null, themeId, authorId);

    addHandler.handle(input, unitOfWork);

    final var captor = ArgumentCaptor.forClass(Question.class);
    verify(questionRepository).persist(captor.capture());
    assertThat(captor.getValue().getAuthorId()).isEqualTo(authorId);
  }

  @Test
  void listQuestionsShouldScopeDirectoryQueriesToRequesterId() {
    final var userId = new UserId(UuidV7.randomUuid());
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var page = Pagination.firstPage(10);
    final var view =
        new QuestionView(
            new QuestionId(UuidV7.randomUuid()),
            "My owned question label?",
            themeId,
            TimelinePoint.now());
    final var pageOf = PageOf.of(page, List.of(view), 1L);

    when(questionDirectory.paginateForUser(page, userId)).thenReturn(pageOf);
    when(questionDirectory.paginateWithThemesForUser(page, Set.of(themeId), userId))
        .thenReturn(pageOf);
    when(questionDirectory.paginateSearchingForUser(page, new SearchQuery("owned"), userId))
        .thenReturn(pageOf);
    when(questionDirectory.paginateSearchingWithThemesForUser(
            page, new SearchQuery("owned"), Set.of(themeId), userId))
        .thenReturn(pageOf);

    assertThat(
            listHandler
                .handle(ListQuestions.Input.perPage(page, userId), unitOfWork)
                .value()
                .items())
        .hasSize(1);
    assertThat(
            listHandler
                .handle(ListQuestions.Input.withThemes(page, Set.of(themeId), userId), unitOfWork)
                .value()
                .items())
        .hasSize(1);
    assertThat(
            listHandler
                .handle(
                    ListQuestions.Input.searching(page, new SearchQuery("owned"), userId),
                    unitOfWork)
                .value()
                .items())
        .hasSize(1);
    assertThat(
            listHandler
                .handle(
                    ListQuestions.Input.searchingWithThemes(
                        page, new SearchQuery("owned"), Set.of(themeId), userId),
                    unitOfWork)
                .value()
                .items())
        .hasSize(1);

    verify(questionDirectory, never()).paginate(any());
  }

  @Test
  void removeQuestionShouldRejectNonOwnerAndNullAuthorEvenIfInUse() {
    final var ownerId = new UserId(UuidV7.randomUuid());
    final var attackerId = new UserId(UuidV7.randomUuid());
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var answers = Set.of(new Answer("True", null, true));

    final var ownedQuestion =
        new Question(
            QuestionType.BOOLEAN, "Owned question label?", null, themeId, answers, ownerId);
    final var nullAuthorQuestion =
        new Question(QuestionType.BOOLEAN, "Null author question?", null, themeId, answers, null);

    when(questionRepository.find(ownedQuestion.getId())).thenReturn(Optional.of(ownedQuestion));
    when(questionRepository.find(nullAuthorQuestion.getId()))
        .thenReturn(Optional.of(nullAuthorQuestion));
    when(quezapDataPort.countQuezapsReferencingQuestion(ownedQuestion.getId().value()))
        .thenReturn(1L);

    assertThatThrownBy(
            () ->
                removeHandler.handle(
                    new RemoveQuestion.Input(ownedQuestion.getId(), attackerId, false), unitOfWork))
        .isInstanceOf(QuestionForbiddenException.class);

    assertThatThrownBy(
            () ->
                removeHandler.handle(
                    new RemoveQuestion.Input(nullAuthorQuestion.getId(), attackerId, false),
                    unitOfWork))
        .isInstanceOf(QuestionForbiddenException.class);

    verify(questionRepository, never()).delete(any());
  }

  @Test
  void removeQuestionShouldRejectOwnerWithConflictWhenStillReferencedByQuezap() {
    final var ownerId = new UserId(UuidV7.randomUuid());
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var question =
        new Question(
            QuestionType.BOOLEAN,
            "Owned question in use?",
            null,
            themeId,
            Set.of(new Answer("True", null, true)),
            ownerId);

    when(questionRepository.find(question.getId())).thenReturn(Optional.of(question));
    when(quezapDataPort.countQuezapsReferencingQuestion(question.getId().value())).thenReturn(2L);

    assertThatThrownBy(
            () ->
                removeHandler.handle(
                    new RemoveQuestion.Input(question.getId(), ownerId, false), unitOfWork))
        .isInstanceOf(QuestionInUseException.class);

    verify(questionRepository, never()).delete(any());
  }

  @Test
  void removeQuestionShouldAllowOwnerAndAdminWhenUnused() {
    final var ownerId = new UserId(UuidV7.randomUuid());
    final var adminId = new UserId(UuidV7.randomUuid());
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var answers = Set.of(new Answer("True", null, true));
    final var ownedQuestion =
        new Question(
            QuestionType.BOOLEAN, "Owned unused question?", null, themeId, answers, ownerId);
    final var nullAuthorQuestion =
        new Question(QuestionType.BOOLEAN, "Legacy unused question?", null, themeId, answers, null);

    when(questionRepository.find(ownedQuestion.getId())).thenReturn(Optional.of(ownedQuestion));
    when(questionRepository.find(nullAuthorQuestion.getId()))
        .thenReturn(Optional.of(nullAuthorQuestion));
    when(quezapDataPort.countQuezapsReferencingQuestion(any())).thenReturn(0L);

    removeHandler.handle(
        new RemoveQuestion.Input(ownedQuestion.getId(), ownerId, false), unitOfWork);
    removeHandler.handle(
        new RemoveQuestion.Input(nullAuthorQuestion.getId(), adminId, true), unitOfWork);

    verify(questionRepository).delete(ownedQuestion);
    verify(questionRepository).delete(nullAuthorQuestion);
  }
}
