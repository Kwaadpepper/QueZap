package com.quezap.api.v1.routes.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.questions.AddQuestionDto.AffirmationDto;
import com.quezap.api.v1.dto.request.questions.FindQuestionsDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.mappers.QuestionMapper;
import com.quezap.api.v1.services.PictureMetadataValidatorService;
import com.quezap.application.ports.questions.AddQuestion;
import com.quezap.application.ports.questions.AddQuestion.AddQuestionUsecase;
import com.quezap.application.ports.questions.ListQuestions;
import com.quezap.application.ports.questions.ListQuestions.ListQuestionsUsecase;
import com.quezap.application.ports.questions.RemoveQuestion;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionForbiddenException;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionInUseException;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuestionControllerTest {

  private UsecaseExecutor executor;
  private AddQuestionUsecase addUsecase;
  private ListQuestionsUsecase listUsecase;
  private RemoveQuestionUsecase removeUsecase;
  private UserRepository userRepository;

  private AddQuestionController addController;
  private FindQuestionsController findController;
  private DeleteQuestionController deleteController;

  private final UUID userId = UuidV7.randomUuid();

  @BeforeEach
  void setUp() {
    executor = mock(UsecaseExecutor.class);
    addUsecase = mock(AddQuestionUsecase.class);
    listUsecase = mock(ListQuestionsUsecase.class);
    removeUsecase = mock(RemoveQuestionUsecase.class);
    userRepository = mock(UserRepository.class);

    final var questionMapper = new QuestionMapper(mock(PictureMetadataValidatorService.class));
    final var paginationMapper = new PaginationMapper();

    addController = new AddQuestionController(executor, addUsecase, questionMapper, userRepository);
    findController =
        new FindQuestionsController(executor, listUsecase, paginationMapper, userRepository);
    deleteController = new DeleteQuestionController(executor, removeUsecase, userRepository);

    final var user =
        User.hydrate(
            new UserId(userId),
            "Teacher",
            new CredentialId(UuidV7.randomUuid()),
            TimelinePoint.now());
    when(userRepository.find(new UserId(userId))).thenReturn(Optional.of(user));

    final var auth = new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void addAffirmationShouldAttachAuthenticatedAuthorId() {
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var qId = new QuestionId(UuidV7.randomUuid());
    when(executor.execute(any(), any(AddQuestion.Input.class)))
        .thenReturn(new AddQuestion.Output(qId));

    addController.addAffirmation(new AffirmationDto("Is Earth round?", true, null, themeId));

    final var captor = ArgumentCaptor.forClass(AddQuestion.Input.class);
    verify(executor).execute(any(), captor.capture());
    assertThat(((AddQuestion.Input.Affirmation) captor.getValue()).authorId())
        .isEqualTo(new UserId(userId));
  }

  @Test
  void findQuestionsShouldScopeQueryToAuthenticatedUser() {
    when(executor.execute(any(), any(ListQuestions.Input.class)))
        .thenReturn(new ListQuestions.Output(PageOf.empty(Pagination.firstPage(10))));

    findController.find(
        new PaginationDto(1L, 10L, null, null, null, null), new FindQuestionsDto(null, null));

    final var captor = ArgumentCaptor.forClass(ListQuestions.Input.class);
    verify(executor).execute(any(), captor.capture());
    assertThat(((ListQuestions.Input.PerPage) captor.getValue()).requesterId())
        .isEqualTo(new UserId(userId));
  }

  @Test
  void findQuestionsShouldRejectUnauthenticatedRequest() {
    SecurityContextHolder.clearContext();
    final var pagination = new PaginationDto(1L, 10L, null, null, null, null);
    final var query = new FindQuestionsDto(null, null);

    assertThatThrownBy(() -> findController.find(pagination, query))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
  }

  @Test
  void deleteQuestionShouldMapForbiddenAndConflictAndPassAdminFlag() {
    final var qId = new QuestionId(UuidV7.randomUuid());

    when(executor.execute(any(), any(RemoveQuestion.Input.class)))
        .thenThrow(new QuestionForbiddenException("Forbidden"))
        .thenThrow(new QuestionInUseException("In use"))
        .thenReturn(new RemoveQuestion.Output.QuestionDeleted());

    assertThatThrownBy(() -> deleteController.delete(qId, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

    assertThatThrownBy(() -> deleteController.delete(qId, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    final var adminAuth =
        new UsernamePasswordAuthenticationToken(
            userId.toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    deleteController.delete(qId, adminAuth);

    final var captor = ArgumentCaptor.forClass(RemoveQuestion.Input.class);
    verify(executor, org.mockito.Mockito.times(3)).execute(any(), captor.capture());
    assertThat(captor.getAllValues().get(2).isAdmin()).isTrue();
    assertThat(captor.getAllValues().get(2).requesterId()).isEqualTo(new UserId(userId));
  }
}
