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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.quezaps.CreateQuezapRequest;
import com.quezap.api.v1.dto.request.quezaps.InlineAnswerRequest;
import com.quezap.api.v1.dto.request.quezaps.InlineQuestionRequest;
import com.quezap.api.v1.dto.request.quezaps.QuezapSlideRequest;
import com.quezap.api.v1.dto.request.quezaps.UpdateQuezapRequest;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.quezaps.QuezapController;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuezapControllerTest {

  private QuezapJpaRepository quezapJpaRepository;
  private QuestionJpaRepository questionJpaRepository;
  private ThemeJpaRepository themeJpaRepository;
  private UserJpaRepository userJpaRepository;
  private PaginationMapper paginationMapper;
  private QuezapController quezapController;
  private final UUID authorId = UUID.randomUUID();
  private UserJpaEntity author;

  @BeforeEach
  void setUp() {
    quezapJpaRepository = Mockito.mock(QuezapJpaRepository.class);
    questionJpaRepository = Mockito.mock(QuestionJpaRepository.class);
    themeJpaRepository = Mockito.mock(ThemeJpaRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    paginationMapper = new PaginationMapper();
    quezapController =
        new QuezapController(
            quezapJpaRepository,
            questionJpaRepository,
            themeJpaRepository,
            userJpaRepository,
            paginationMapper);

    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    author = new UserJpaEntity(authorId, "Teacher", cred, Instant.now(), Instant.now());
    when(userJpaRepository.findById(authorId)).thenReturn(Optional.of(author));

    final var auth = new UsernamePasswordAuthenticationToken(authorId.toString(), null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateQuezap() {
    final var qId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "History", Instant.now());
    final var question =
        new QuestionJpaEntity(
            qId, "QUIZZ", "What year is it?", null, theme, Instant.now(), Instant.now());
    question.addAnswer(
        new com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity(
            UUID.randomUUID(), question, "1789", null, true, 0));
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
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "History Quiz", "Description", author, true, Instant.now(), Instant.now());

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));

    final var response = quezapController.getById(quezapId);

    assertThat(response.title()).isEqualTo("History Quiz");
    assertThat(response.author().name()).isEqualTo("Teacher");
  }

  @Test
  void shouldListQuezapsForAuthenticatedAuthor() {
    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(),
            "History Quiz",
            "Description",
            author,
            false,
            Instant.now(),
            Instant.now());

    when(quezapJpaRepository.findByAuthorId(any(UUID.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(quezap)));

    final var response =
        quezapController.list(null, null, new PaginationDto(1L, 10L, null, null, null, null));

    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).title()).isEqualTo("History Quiz");
  }

  @Test
  void shouldListPublicQuezapsWhenUnauthenticated() {
    SecurityContextHolder.clearContext();
    final var quezap =
        new QuezapJpaEntity(
            UUID.randomUUID(),
            "Public Quiz",
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
    assertThat(response.data().get(0).title()).isEqualTo("Public Quiz");
  }

  @Test
  void shouldUpdateQuezap() {
    final var quezapId = UUID.randomUUID();
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
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "History Quiz", "Description", author, true, Instant.now(), Instant.now());
    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));

    quezapController.delete(quezapId);

    verify(quezapJpaRepository).delete(quezap);
  }

  @Test
  void shouldCreateQuezapWithInlineQuestions() {
    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(null, "What is 2+2?", "QUIZZ", null, null, "Math", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);

    final var request = new CreateQuezapRequest("Math Quiz", "Inline quiz", true, List.of(slide));

    final var response = quezapController.create(request);

    assertThat(response.id()).isNotNull();
    verify(questionJpaRepository).save(any(QuestionJpaEntity.class));
    verify(quezapJpaRepository).save(any(QuezapJpaEntity.class));
  }

  @Test
  void shouldUpdateQuezapWithInlineQuestions() {
    final var quezapId = UUID.randomUUID();
    final var quezap =
        new QuezapJpaEntity(
            quezapId, "Old Title", "Old Desc", author, false, Instant.now(), Instant.now());

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            null, "Updated question?", "QUIZZ", null, null, "Science", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 15, 500, 1, inlineQuestion);

    final var updateReq =
        new UpdateQuezapRequest("Updated Title", "Updated Desc", true, List.of(slide));

    quezapController.update(quezapId, updateReq);

    assertThat(quezap.getTitle()).isEqualTo("Updated Title");
    assertThat(quezap.getQuestions()).hasSize(1);
    verify(questionJpaRepository).save(any(QuestionJpaEntity.class));
    verify(quezapJpaRepository).save(quezap);
  }

  @Test
  void shouldUploadDataUrlPictureViaQuestionPictureManagerAndReturnNormalizedPictureUrl() {
    final var pictureManager =
        Mockito.mock(com.quezap.domain.ports.services.QuestionPictureManager.class);
    final var storedKey = "pictures/questions/" + com.quezap.lib.utils.UuidV7.randomUuid() + ".png";
    final var storedPicture =
        new com.quezap.domain.models.valueobjects.pictures.Picture(
            storedKey,
            com.quezap.domain.models.valueobjects.pictures.PictureType.PNG,
            new com.quezap.domain.models.valueobjects.Sha256Hash(new byte[32]));
    when(pictureManager.store(any())).thenReturn(storedPicture);

    final var controllerWithS3 =
        new QuezapController(
            quezapJpaRepository,
            questionJpaRepository,
            themeJpaRepository,
            userJpaRepository,
            paginationMapper,
            pictureManager);

    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var dataUrl =
        "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            null, "Question with image?", "QUIZZ", dataUrl, null, "Science", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);

    final var createReq =
        new CreateQuezapRequest("Visual Quiz", "With image", true, List.of(slide));
    controllerWithS3.create(createReq);

    verify(pictureManager).store(any());

    final var captor = org.mockito.ArgumentCaptor.forClass(QuestionJpaEntity.class);
    verify(questionJpaRepository).save(captor.capture());
    assertThat(captor.getValue().getPictureFilename())
        .isEqualTo("/quezap-public-images/" + storedKey);
  }

  @Test
  void shouldClearPictureWhenUpdatedWithNullPictureFilename() {
    final var quezapId = UUID.randomUUID();
    final var existingQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var existingQuestion =
        new QuestionJpaEntity(
            existingQuestionId,
            "QUIZZ",
            "Question with picture",
            "/quezap-public-images/pictures/questions/old.png",
            theme,
            Instant.now(),
            Instant.now());
    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.findById(existingQuestionId))
        .thenReturn(Optional.of(existingQuestion));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            existingQuestionId,
            "Question without picture",
            "QUIZZ",
            null,
            null,
            "Science",
            inlineAnswers);
    final var slide = new QuezapSlideRequest(existingQuestionId, 20, 1000, 1, inlineQuestion);

    quezapController.update(
        quezapId, new UpdateQuezapRequest("Quiz", "Desc", true, List.of(slide)));

    assertThat(existingQuestion.getPictureFilename()).isNull();
  }

  @Test
  void shouldDeepCloneQuestionsWhenDuplicatingQuezap() {
    final var originalId = UUID.randomUUID();
    final var originalQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var originalQuestion =
        new QuestionJpaEntity(
            originalQuestionId,
            "QUIZZ",
            "Original Question",
            null,
            theme,
            Instant.now(),
            Instant.now());
    originalQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), originalQuestion, "Ans 1", null, true, 1));
    originalQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), originalQuestion, "Ans 2", null, false, 2));

    final var original =
        new QuezapJpaEntity(
            originalId, "Original Quiz", "Desc", author, true, Instant.now(), Instant.now());
    original.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), original, originalQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(originalId)).thenReturn(Optional.of(original));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    final var savedCopyCaptor = org.mockito.ArgumentCaptor.forClass(QuezapJpaEntity.class);
    when(quezapJpaRepository.save(savedCopyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

    quezapController.duplicate(originalId);

    final var duplicated = savedCopyCaptor.getValue();
    assertThat(duplicated.getTitle()).isEqualTo("Original Quiz (Copie)");
    assertThat(duplicated.getQuestions()).hasSize(1);
    final var duplicatedQuestion = duplicated.getQuestions().iterator().next().getQuestion();
    assertThat(duplicatedQuestion.getId()).isNotEqualTo(originalQuestionId);
    assertThat(duplicatedQuestion.getLabel()).isEqualTo("Original Question");
    assertThat(duplicatedQuestion.getAnswers()).hasSize(2);
  }
}
