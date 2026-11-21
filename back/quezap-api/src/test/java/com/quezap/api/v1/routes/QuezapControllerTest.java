package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.quezaps.CreateQuezapRequest;
import com.quezap.api.v1.dto.request.quezaps.InlineAnswerRequest;
import com.quezap.api.v1.dto.request.quezaps.InlineQuestionRequest;
import com.quezap.api.v1.dto.request.quezaps.QuezapSlideRequest;
import com.quezap.api.v1.dto.request.quezaps.UpdateQuezapRequest;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.quezaps.QuezapController;
import com.quezap.application.usecases.quezaps.QuezapApplicationService;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.QuezapDataJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.StagedPictureJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.infrastructure.adapter.services.StagedPictureService;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuezapControllerTest {

  private QuezapJpaRepository quezapJpaRepository;
  private QuestionJpaRepository questionJpaRepository;
  private ThemeJpaRepository themeJpaRepository;
  private UserJpaRepository userJpaRepository;
  private PaginationMapper paginationMapper;
  private QuezapDataJpaAdapter quezapDataAdapter;
  private QuezapController quezapController;
  private final UUID authorId = UUID.randomUUID();
  private UserJpaEntity author;

  @BeforeEach
  void setUp() {
    quezapJpaRepository = mock(QuezapJpaRepository.class);
    questionJpaRepository = mock(QuestionJpaRepository.class);
    themeJpaRepository = mock(ThemeJpaRepository.class);
    userJpaRepository = mock(UserJpaRepository.class);
    paginationMapper = new PaginationMapper();
    quezapDataAdapter =
        new QuezapDataJpaAdapter(
            quezapJpaRepository, questionJpaRepository, themeJpaRepository, userJpaRepository);
    quezapController = buildController(null, null);

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
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private QuezapController buildController(
      @Nullable StagedPictureManager stagedPictureManager,
      @Nullable QuestionPictureManager questionPictureManager) {
    final var service =
        new QuezapApplicationService(
            quezapDataAdapter, stagedPictureManager, questionPictureManager);
    return new QuezapController(service, paginationMapper);
  }

  @Test
  void shouldCreateQuezap() {
    final var qId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "History", Instant.now());
    final var question =
        new QuestionJpaEntity(
            qId, "QUIZZ", "What year is it?", null, theme, authorId, Instant.now(), Instant.now());
    question.addAnswer(new AnswerJpaEntity(UUID.randomUUID(), question, "1789", null, true, 0));
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
  void shouldRejectDataUrlPictureInCreateQuezap() {
    final var dataUrl =
        "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
            + "+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
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

    assertThatThrownBy(() -> quezapController.create(createReq))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldRejectExternalAndTraversalPictureUrlsInCreateQuezap() {
    final var validUuid = UuidV7.randomUuid();
    final var invalidUrls =
        List.of(
            "http://evil.example.com/tracker.png",
            "https://evil.example.com/pictures/questions/" + validUuid + ".png",
            "/quezap-public-images/pictures/questions/../" + validUuid + ".png",
            "/apiv1/auth/logout",
            "stored.png",
            "/quezap-public-images/pictures/questions/" + validUuid + ".svg");

    for (final var badUrl : invalidUrls) {
      final var inlineAnswers =
          List.of(
              new InlineAnswerRequest(null, "Answer A", true, null, 1),
              new InlineAnswerRequest(null, "Answer B", false, null, 2));
      final var inlineQuestion =
          new InlineQuestionRequest(
              null, "Question with bad image?", "QUIZZ", badUrl, null, "Science", inlineAnswers);
      final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);
      final var createReq =
          new CreateQuezapRequest("Visual Quiz", "Bad image", true, List.of(slide));

      assertThatThrownBy(() -> quezapController.create(createReq))
          .as("Should reject invalid picture URL: %s", badUrl)
          .isInstanceOf(ResponseStatusException.class)
          .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldClaimPicturesWhenCreatingQuezapWithPreUploadedPicture() {
    final var stagedManager = mock(StagedPictureManager.class);
    final var controllerWithStaged = buildController(stagedManager, null);

    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var picUrl = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";
    when(stagedManager.resolveValidPublicPictureUrl(picUrl)).thenReturn(picUrl);

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            null, "Question with image?", "QUIZZ", picUrl, null, "Science", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);

    final var createReq =
        new CreateQuezapRequest("Visual Quiz", "With image", true, List.of(slide));
    controllerWithStaged.create(createReq);

    final var captor = ArgumentCaptor.forClass(Collection.class);
    verify(stagedManager).claimPictures(captor.capture(), org.mockito.Mockito.eq(authorId));
    assertThat(captor.getValue()).contains(picUrl);
  }

  @Test
  void shouldRejectClaimingAnotherUsersStagedPicture() {
    final var stagedRepo = mock(StagedPictureJpaRepository.class);
    final var pictureManager = mock(QuestionPictureManager.class);
    final var realStagedService = new StagedPictureService(stagedRepo, pictureManager);
    final var controllerWithRealStaged = buildController(realStagedService, pictureManager);

    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var otherUserId = UuidV7.randomUuid();
    final var picKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var picUrl = "/quezap-public-images/" + picKey;
    final var now = Instant.now();
    when(stagedRepo.findByPictureUrlIn(List.of(picUrl)))
        .thenReturn(
            List.of(
                new StagedPictureJpaEntity(
                    UuidV7.randomUuid(), picKey, picUrl, otherUserId, now, now.plusSeconds(3600))));

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            null, "Question with stolen image?", "QUIZZ", picUrl, null, "Science", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);

    final var createReq =
        new CreateQuezapRequest("Stolen Image Quiz", "Attack", true, List.of(slide));

    assertThatThrownBy(() -> controllerWithRealStaged.create(createReq))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldRejectAttachingUnstagedForeignPictureUrl() {
    final var stagedRepo = mock(StagedPictureJpaRepository.class);
    final var pictureManager = mock(QuestionPictureManager.class);
    final var realStagedService = new StagedPictureService(stagedRepo, pictureManager);
    final var controllerWithRealStaged = buildController(realStagedService, pictureManager);

    when(quezapJpaRepository.save(any(QuezapJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var picUrl = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";
    when(stagedRepo.findByPictureUrlIn(List.of(picUrl))).thenReturn(List.of());

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            null, "Question with foreign image?", "QUIZZ", picUrl, null, "Science", inlineAnswers);
    final var slide = new QuezapSlideRequest(null, 20, 1000, 1, inlineQuestion);

    final var createReq =
        new CreateQuezapRequest("Foreign Image Quiz", "Attack", true, List.of(slide));

    assertThatThrownBy(() -> controllerWithRealStaged.create(createReq))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldClearPictureAndDeleteFromS3WhenNoReferencesRemain() {
    final var quezapId = UUID.randomUUID();
    final var existingQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var picKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var picUrl = "/quezap-public-images/" + picKey;
    final var existingQuestion =
        new QuestionJpaEntity(
            existingQuestionId,
            "QUIZZ",
            "Question with picture",
            picUrl,
            theme,
            authorId,
            Instant.now(),
            Instant.now());
    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, existingQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.findById(existingQuestionId))
        .thenReturn(Optional.of(existingQuestion));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.existsByPictureFilenameIn(anyCollection())).thenReturn(false);
    when(questionJpaRepository.existsAnswerByPictureFilenameIn(anyCollection())).thenReturn(false);

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

    final var pictureManager = mock(QuestionPictureManager.class);
    final var controllerWithManager = buildController(null, pictureManager);

    controllerWithManager.update(
        quezapId, new UpdateQuezapRequest("Quiz", "Desc", true, List.of(slide)));

    assertThat(existingQuestion.getPictureFilename()).isNull();
    verify(pictureManager).removeByObjectKey(picKey);
  }

  @Test
  void shouldNotDeleteS3PictureWhenUpdatingClonedQuezapIfStillReferencedInDb() {
    final var originalId = UUID.randomUUID();
    final var clonedId = UUID.randomUUID();
    final var clonedQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var sharedKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var sharedUrl = "/quezap-public-images/" + sharedKey;

    final var originalQuestion =
        new QuestionJpaEntity(
            UUID.randomUUID(),
            "QUIZZ",
            "Original Question",
            sharedUrl,
            theme,
            Instant.now(),
            Instant.now());
    final var original =
        new QuezapJpaEntity(
            originalId, "Original Quiz", "Desc", author, true, Instant.now(), Instant.now());
    original.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), original, originalQuestion, 20, 1000, 1));

    final var clonedQuestion =
        new QuestionJpaEntity(
            clonedQuestionId,
            "QUIZZ",
            "Cloned Question",
            sharedUrl,
            theme,
            authorId,
            Instant.now(),
            Instant.now());
    final var cloned =
        new QuezapJpaEntity(
            clonedId, "Original Quiz (Copie)", "Desc", author, false, Instant.now(), Instant.now());
    cloned.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), cloned, clonedQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(clonedId)).thenReturn(Optional.of(cloned));
    when(questionJpaRepository.findById(clonedQuestionId)).thenReturn(Optional.of(clonedQuestion));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(questionJpaRepository.existsByPictureFilenameIn(anyCollection())).thenReturn(true);

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            clonedQuestionId,
            "Cloned Question Without Picture",
            "QUIZZ",
            null,
            null,
            "Science",
            inlineAnswers);
    final var slide = new QuezapSlideRequest(clonedQuestionId, 20, 1000, 1, inlineQuestion);

    final var pictureManager = mock(QuestionPictureManager.class);
    final var controllerWithManager = buildController(null, pictureManager);

    controllerWithManager.update(
        clonedId, new UpdateQuezapRequest("Cloned", "Desc", false, List.of(slide)));

    assertThat(clonedQuestion.getPictureFilename()).isNull();
    verify(pictureManager, never()).removeByObjectKey(any());
  }

  @Test
  void shouldNotDeleteS3PictureWhenDeletingClonedQuezapIfStillReferencedInDb() {
    final var clonedId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var sharedKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var sharedUrl = "/quezap-public-images/" + sharedKey;

    final var clonedQuestion =
        new QuestionJpaEntity(
            UUID.randomUUID(),
            "QUIZZ",
            "Cloned Question",
            sharedUrl,
            theme,
            Instant.now(),
            Instant.now());
    final var cloned =
        new QuezapJpaEntity(
            clonedId, "Cloned Quiz", "Desc", author, false, Instant.now(), Instant.now());
    cloned.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), cloned, clonedQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(clonedId)).thenReturn(Optional.of(cloned));
    when(questionJpaRepository.existsByPictureFilenameIn(anyCollection())).thenReturn(true);

    final var pictureManager = mock(QuestionPictureManager.class);
    final var controllerWithManager = buildController(null, pictureManager);

    controllerWithManager.delete(clonedId);

    verify(quezapJpaRepository).delete(cloned);
    verify(pictureManager, never()).removeByObjectKey(any());
  }

  @Test
  void shouldNotDeleteS3PictureWhenRemovingQuestionIfReferencedByAnswer() {
    final var quezapId = UUID.randomUUID();
    final var questionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var answerPicKey = "pictures/questions/" + UuidV7.randomUuid() + ".webp";
    final var answerPicUrl = "/quezap-public-images/" + answerPicKey;

    final var question =
        new QuestionJpaEntity(
            questionId, "QUIZZ", "Question", null, theme, Instant.now(), Instant.now());
    question.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), question, "Ans 1", answerPicUrl, true, 1));
    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.existsByPictureFilenameIn(anyCollection())).thenReturn(false);
    when(questionJpaRepository.existsAnswerByPictureFilenameIn(anyCollection())).thenReturn(true);

    final var pictureManager = mock(QuestionPictureManager.class);
    final var controllerWithManager = buildController(null, pictureManager);

    controllerWithManager.removeQuestion(quezapId, questionId);

    verify(pictureManager, never()).removeByObjectKey(any());
  }

  @Test
  void shouldDeferS3DeletionUntilTransactionCommit() {
    final var quezapId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var picKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var picUrl = "/quezap-public-images/" + picKey;

    final var question =
        new QuestionJpaEntity(
            UUID.randomUUID(), "QUIZZ", "Question", picUrl, theme, Instant.now(), Instant.now());
    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, question, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.existsByPictureFilenameIn(anyCollection())).thenReturn(false);
    when(questionJpaRepository.existsAnswerByPictureFilenameIn(anyCollection())).thenReturn(false);

    final var pictureManager = mock(QuestionPictureManager.class);
    final var controllerWithManager = buildController(null, pictureManager);

    TransactionSynchronizationManager.initSynchronization();
    controllerWithManager.delete(quezapId);

    // Must NOT delete from S3 before commit
    verify(pictureManager, never()).removeByObjectKey(any());

    final var synchronizations = TransactionSynchronizationManager.getSynchronizations();
    assertThat(synchronizations).hasSize(1);

    // Simulate rollback -> still must NOT delete
    synchronizations.forEach(
        sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verify(pictureManager, never()).removeByObjectKey(any());

    // Simulate commit -> NOW deletes from S3
    synchronizations.forEach(TransactionSynchronization::afterCommit);
    verify(pictureManager).removeByObjectKey(picKey);
  }

  @Test
  void shouldAllowUpdatingQuizKeepingExistingPictureWithoutReclaiming() {
    final var quezapId = UUID.randomUUID();
    final var existingQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var existingPicUrl =
        "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var existingQuestion =
        new QuestionJpaEntity(
            existingQuestionId,
            "QUIZZ",
            "Old label",
            existingPicUrl,
            theme,
            authorId,
            Instant.now(),
            Instant.now());
    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, existingQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.findById(existingQuestionId))
        .thenReturn(Optional.of(existingQuestion));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var stagedRepo = mock(StagedPictureJpaRepository.class);
    final var pictureManager = mock(QuestionPictureManager.class);
    final var realStagedService = new StagedPictureService(stagedRepo, pictureManager);
    final var controllerWithRealStaged = buildController(realStagedService, pictureManager);

    final var inlineAnswers =
        List.of(
            new InlineAnswerRequest(null, "Answer A", true, null, 1),
            new InlineAnswerRequest(null, "Answer B", false, null, 2));
    final var inlineQuestion =
        new InlineQuestionRequest(
            existingQuestionId,
            "New label",
            "QUIZZ",
            existingPicUrl,
            null,
            "Science",
            inlineAnswers);
    final var slide = new QuezapSlideRequest(existingQuestionId, 20, 1000, 1, inlineQuestion);

    controllerWithRealStaged.update(
        quezapId, new UpdateQuezapRequest("Quiz", "Desc", true, List.of(slide)));

    assertThat(existingQuestion.getLabel()).isEqualTo("New label");
    assertThat(existingQuestion.getPictureFilename()).isEqualTo(existingPicUrl);
    verify(stagedRepo, never()).findByPictureUrlIn(any());
    verify(pictureManager, never()).removeByObjectKey(any());
  }

  @Test
  void shouldNotMutateForeignQuestionIdAndShouldUpdateExistingAnswersInPlace() {
    final var quezapId = UUID.randomUUID();
    final var ownQuestionId = UUID.randomUUID();
    final var foreignQuestionId = UUID.randomUUID();
    final var existingAnswerId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());

    final var ownQuestion =
        new QuestionJpaEntity(
            ownQuestionId,
            "QUIZZ",
            "Own Question",
            null,
            theme,
            authorId,
            Instant.now(),
            Instant.now());
    final var existingAnswer =
        new AnswerJpaEntity(existingAnswerId, ownQuestion, "Old Label", null, true, 1);
    ownQuestion.addAnswer(existingAnswer);

    final var foreignQuestion =
        new QuestionJpaEntity(
            foreignQuestionId,
            "QUIZZ",
            "Foreign Question",
            null,
            theme,
            Instant.now(),
            Instant.now());

    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, ownQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.findById(ownQuestionId)).thenReturn(Optional.of(ownQuestion));
    when(questionJpaRepository.findById(foreignQuestionId))
        .thenReturn(Optional.of(foreignQuestion));
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var ownInline =
        new InlineQuestionRequest(
            ownQuestionId,
            "Updated Own Question",
            "QUIZZ",
            null,
            null,
            "Science",
            List.of(new InlineAnswerRequest(existingAnswerId, "Updated Label", false, null, 1)));
    final var foreignInline =
        new InlineQuestionRequest(
            foreignQuestionId,
            "Tampered Foreign Question",
            "QUIZZ",
            null,
            null,
            "Science",
            List.of(new InlineAnswerRequest(null, "Tampered Answer", true, null, 1)));

    quezapController.update(
        quezapId,
        new UpdateQuezapRequest(
            "Quiz",
            "Desc",
            true,
            List.of(
                new QuezapSlideRequest(ownQuestionId, 20, 1000, 1, ownInline),
                new QuezapSlideRequest(foreignQuestionId, 20, 1000, 2, foreignInline))));

    // Own question and its existing answer entity instance were updated in place
    assertThat(ownQuestion.getLabel()).isEqualTo("Updated Own Question");
    assertThat(ownQuestion.getAnswers()).containsExactly(existingAnswer);
    assertThat(existingAnswer.getLabel()).isEqualTo("Updated Label");
    assertThat(existingAnswer.getIsCorrect()).isFalse();

    // Foreign question was NOT mutated in place
    assertThat(foreignQuestion.getLabel()).isEqualTo("Foreign Question");
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
    final var savedCopyCaptor = ArgumentCaptor.forClass(QuezapJpaEntity.class);
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

  @Test
  void shouldRejectDuplicatingOrCloningPrivateQuezapFromAnotherAuthor() {
    final var otherAuthorId = UUID.randomUUID();
    final var otherCred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "other", "pass", null, Instant.now(), Instant.now());
    final var otherAuthor =
        new UserJpaEntity(otherAuthorId, "OtherTeacher", otherCred, Instant.now(), Instant.now());
    final var privateQuezapId = UUID.randomUUID();
    final var privateQuezap =
        new QuezapJpaEntity(
            privateQuezapId,
            "Secret Quiz",
            "Desc",
            otherAuthor,
            false,
            Instant.now(),
            Instant.now());

    when(quezapJpaRepository.findById(privateQuezapId)).thenReturn(Optional.of(privateQuezap));

    assertThatThrownBy(() -> quezapController.duplicate(privateQuezapId))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

    assertThatThrownBy(() -> quezapController.clone(privateQuezapId))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldRejectAttachingAnotherUsersPrivateOrNullAuthorQuestion() {
    final var otherAuthorId = UUID.randomUUID();
    final var privateQuestionId = UUID.randomUUID();
    final var nullAuthorQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "History", Instant.now());

    final var privateQuestion =
        new QuestionJpaEntity(
            privateQuestionId,
            "QUIZZ",
            "Secret Question",
            null,
            theme,
            otherAuthorId,
            Instant.now(),
            Instant.now());
    privateQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), privateQuestion, "Ans", null, true, 1));

    final var nullAuthorQuestion =
        new QuestionJpaEntity(
            nullAuthorQuestionId,
            "QUIZZ",
            "Legacy Orphan Question",
            null,
            theme,
            null,
            Instant.now(),
            Instant.now());
    nullAuthorQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), nullAuthorQuestion, "Ans", null, true, 1));

    final var ownQuezapId = UUID.randomUUID();
    final var ownQuezap =
        new QuezapJpaEntity(
            ownQuezapId, "Own Quiz", "Desc", author, false, Instant.now(), Instant.now());

    when(questionJpaRepository.findById(privateQuestionId))
        .thenReturn(Optional.of(privateQuestion));
    when(questionJpaRepository.findById(nullAuthorQuestionId))
        .thenReturn(Optional.of(nullAuthorQuestion));
    when(questionJpaRepository.isQuestionInPublicQuezap(privateQuestionId)).thenReturn(false);
    when(questionJpaRepository.isQuestionInPublicQuezap(nullAuthorQuestionId)).thenReturn(false);
    when(quezapJpaRepository.findById(ownQuezapId)).thenReturn(Optional.of(ownQuezap));

    final var createWithPrivate =
        new CreateQuezapRequest(
            "Steal Quiz",
            "Desc",
            false,
            List.of(new QuezapSlideRequest(privateQuestionId, 30, 100)));
    assertThatThrownBy(() -> quezapController.create(createWithPrivate))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

    final var createWithNullAuthor =
        new CreateQuezapRequest(
            "Legacy Bypass Quiz",
            "Desc",
            false,
            List.of(new QuezapSlideRequest(nullAuthorQuestionId, 30, 100)));
    assertThatThrownBy(() -> quezapController.create(createWithNullAuthor))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

    final var updateWithPrivate =
        new UpdateQuezapRequest(
            "Steal Quiz",
            "Desc",
            false,
            List.of(new QuezapSlideRequest(privateQuestionId, 30, 100)));
    assertThatThrownBy(() -> quezapController.update(ownQuezapId, updateWithPrivate))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

    assertThatThrownBy(
            () ->
                quezapController.addQuestion(
                    ownQuezapId, new QuezapSlideRequest(privateQuestionId, 30, 100)))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldClonePublicQuestionInsteadOfLinkingWhenAttachedByAnotherUser() {
    final var otherAuthorId = UUID.randomUUID();
    final var publicQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var publicQuestion =
        new QuestionJpaEntity(
            publicQuestionId,
            "QUIZZ",
            "Public Question?",
            null,
            theme,
            otherAuthorId,
            Instant.now(),
            Instant.now());
    publicQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), publicQuestion, "Yes", null, true, 1));
    publicQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), publicQuestion, "No", null, false, 2));

    when(questionJpaRepository.findById(publicQuestionId)).thenReturn(Optional.of(publicQuestion));
    when(questionJpaRepository.isQuestionInPublicQuezap(publicQuestionId)).thenReturn(true);
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    final var savedQuezapCaptor = ArgumentCaptor.forClass(QuezapJpaEntity.class);
    when(quezapJpaRepository.save(savedQuezapCaptor.capture()))
        .thenAnswer(inv -> inv.getArgument(0));

    final var request =
        new CreateQuezapRequest(
            "Remix Quiz",
            "Desc",
            false,
            List.of(new QuezapSlideRequest(publicQuestionId, 30, 100)));

    quezapController.create(request);

    final var savedQuiz = savedQuezapCaptor.getValue();
    final var attachedQuestion = savedQuiz.getQuestions().iterator().next().getQuestion();
    assertThat(attachedQuestion.getId()).isNotEqualTo(publicQuestionId);
    assertThat(attachedQuestion.getAuthorId()).isEqualTo(authorId);
    assertThat(attachedQuestion.getLabel()).isEqualTo("Public Question?");
    assertThat(publicQuestion.getAuthorId()).isEqualTo(otherAuthorId);
  }

  @Test
  void shouldCopyOnWriteWhenUpdatingQuestionSharedAcrossMultipleQuezaps() {
    final var quezapId = UUID.randomUUID();
    final var sharedQuestionId = UUID.randomUUID();
    final var theme = new ThemeJpaEntity(UUID.randomUUID(), "Science", Instant.now());
    final var sharedQuestion =
        new QuestionJpaEntity(
            sharedQuestionId,
            "QUIZZ",
            "Original Shared Label",
            null,
            theme,
            authorId,
            Instant.now(),
            Instant.now());
    sharedQuestion.addAnswer(
        new AnswerJpaEntity(UUID.randomUUID(), sharedQuestion, "Original Ans", null, true, 1));

    final var quezap =
        new QuezapJpaEntity(quezapId, "Quiz 1", "Desc", author, true, Instant.now(), Instant.now());
    quezap.addQuestion(
        new QuezapQuestionJpaEntity(UUID.randomUUID(), quezap, sharedQuestion, 20, 1000, 1));

    when(quezapJpaRepository.findById(quezapId)).thenReturn(Optional.of(quezap));
    when(questionJpaRepository.findById(sharedQuestionId)).thenReturn(Optional.of(sharedQuestion));
    when(questionJpaRepository.countOtherQuezapsReferencingQuestion(sharedQuestionId, quezapId))
        .thenReturn(1L);
    when(questionJpaRepository.save(any(QuestionJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var updatedInline =
        new InlineQuestionRequest(
            sharedQuestionId,
            "Edited Label Only For Quiz 1",
            "QUIZZ",
            null,
            null,
            "Science",
            List.of(new InlineAnswerRequest(null, "New Ans", true, null, 1)));

    quezapController.update(
        quezapId,
        new UpdateQuezapRequest(
            "Quiz 1",
            "Desc",
            true,
            List.of(new QuezapSlideRequest(sharedQuestionId, 20, 1000, 1, updatedInline))));

    // Shared question entity MUST NOT be mutated in place
    assertThat(sharedQuestion.getLabel()).isEqualTo("Original Shared Label");
    final var quizQuestion = quezap.getQuestions().iterator().next().getQuestion();
    assertThat(quizQuestion.getId()).isNotEqualTo(sharedQuestionId);
    assertThat(quizQuestion.getLabel()).isEqualTo("Edited Label Only For Quiz 1");
    assertThat(quizQuestion.getAuthorId()).isEqualTo(authorId);
  }

  @Test
  void shouldRejectOversizedDescriptionAndSlidesInRequestDtos() {
    final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
    final var validator = factory.getValidator();
    final var longDescription = "a".repeat(2001);
    final var tooManySlides =
        java.util.stream.IntStream.range(0, 101)
            .mapToObj(i -> new QuezapSlideRequest(UUID.randomUUID(), 20, 100))
            .toList();

    final var createWithLongDesc =
        new CreateQuezapRequest("Title", longDescription, true, List.of());
    assertThat(validator.validate(createWithLongDesc)).isNotEmpty();

    final var createWithTooManySlides = new CreateQuezapRequest("Title", "ok", true, tooManySlides);
    assertThat(validator.validate(createWithTooManySlides)).isNotEmpty();

    final var updateWithLongDesc =
        new UpdateQuezapRequest("Title", longDescription, true, List.of());
    assertThat(validator.validate(updateWithLongDesc)).isNotEmpty();

    final var updateWithTooManySlides = new UpdateQuezapRequest("Title", "ok", true, tooManySlides);
    assertThat(validator.validate(updateWithTooManySlides)).isNotEmpty();
  }
}
