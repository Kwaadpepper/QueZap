package com.quezap.domain.usecases.questions;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.quezap.domain.errors.questions.AddQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.OnFailure;
import com.quezap.lib.ddd.usecases.TransactionRegistrar;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public sealed interface AddQuestion {

  sealed interface Input extends UseCaseInput {
    record Affirmation(
        String question, boolean isTrue, @Nullable PictureUploadData picture, ThemeId theme)
        implements Input {}

    record Binary(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme)
        implements Input {}

    record Quizz(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme)
        implements Input {}

    public record AnswerData(
        String answerText, @Nullable PictureUploadData picture, boolean isCorrect) {}
  }

  record Output(QuestionId id) implements UseCaseOutput {}

  final class Handler implements UseCaseHandler<Input, Output>, AddQuestion, OnFailure {
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private final QuestionRepository questionRepository;
    private final ThemeRepository themeRepository;
    private final QuestionPictureManager questionPictureManager;
    private final TransactionRegistrar cleanupCoordinator;

    public Handler(
        QuestionRepository questionRepository,
        ThemeRepository themeRepository,
        QuestionPictureManager questionPictureManager,
        TransactionRegistrar cleanupCoordinator) {
      this.questionRepository = questionRepository;
      this.themeRepository = themeRepository;
      this.questionPictureManager = questionPictureManager;
      this.cleanupCoordinator = cleanupCoordinator;
    }

    @Override
    public Output handle(Input usecaseInput) {
      return switch (usecaseInput) {
        case Input.Affirmation affirmation -> handleAffirmation(affirmation);
        case Input.Binary binaryQuestion -> handleBinary(binaryQuestion);
        case Input.Quizz quiz -> handleQuiz(quiz);
      };
    }

    private Output handleAffirmation(Input.Affirmation input) {
      final var questionValue = input.question();
      final var isTrue = input.isTrue();
      final var pictureBytes = input.picture();
      final var themeId = input.theme();
      final var theme = themeRepository.find(themeId);

      if (theme == null) {
        throw new DomainConstraintException(AddQuestionError.THEME_DOES_NOT_EXISTS);
      }

      final var picture = storePicture(pictureBytes);
      final var answer = new Answer(isTrue ? "True" : "False", null, isTrue);
      final var question =
          createQuestion(QuestionType.BOOLEAN, questionValue, picture, themeId, Set.of(answer));

      questionRepository.save(question);

      return new Output(question.getId());
    }

    private Output handleBinary(Input.Binary input) {
      final var questionValue = input.question();
      final var pictureBytes = input.picture();
      final var themeId = input.theme();
      final var theme = themeRepository.find(themeId);
      final var answers =
          input.answers().stream().map(this::answerDataMapper).collect(Collectors.toSet());

      if (theme == null) {
        throw new DomainConstraintException(AddQuestionError.THEME_DOES_NOT_EXISTS);
      }

      final var picture = storePicture(pictureBytes);
      final var question =
          createQuestion(QuestionType.BINARY, questionValue, picture, themeId, answers);

      questionRepository.save(question);

      return new Output(question.getId());
    }

    private Output handleQuiz(Input.Quizz input) {
      final var questionValue = input.question();
      final var pictureBytes = input.picture();
      final var themeId = input.theme();
      final var theme = themeRepository.find(themeId);
      final var answers =
          input.answers().stream().map(this::answerDataMapper).collect(Collectors.toSet());

      if (theme == null) {
        throw new DomainConstraintException(AddQuestionError.THEME_DOES_NOT_EXISTS);
      }

      final var picture = storePicture(pictureBytes);
      final var question =
          createQuestion(QuestionType.QUIZZ, questionValue, picture, themeId, answers);

      questionRepository.save(question);

      return new Output(question.getId());
    }

    private Question createQuestion(
        QuestionType type,
        String value,
        @Nullable Picture picture,
        ThemeId theme,
        Set<Answer> answers) {
      try {
        final var updatedAt = ZonedDateTime.now(java.time.ZoneId.of("UTC"));

        return new Question(type, value, picture, theme, answers, updatedAt);
      } catch (IllegalDomainStateException e) {
        // * Convert to a more specific domain exception with relevant error message
        throw new DomainConstraintException(AddQuestionError.INVALID_QUESTION_DATA, e.getMessage());
      }
    }

    private @Nullable Picture storePicture(@Nullable PictureUploadData pictureData) {
      if (pictureData == null) {
        return null;
      }

      final var picture = questionPictureManager.store(pictureData);

      cleanupCoordinator.register(picture);

      return picture;
    }

    private Answer answerDataMapper(Input.AnswerData answerData) {
      final var answerText = answerData.answerText();
      final var pictureBytes = answerData.picture();
      final var isCorrect = answerData.isCorrect();
      final var picture = storePicture(pictureBytes);

      return new Answer(answerText, picture, isCorrect);
    }

    @Override
    public void onFailure(Throwable error) {
      final Set<Picture> picturesToClean = cleanupCoordinator.retrieveAndUnbind(Picture.class);

      if (!picturesToClean.isEmpty()) {

        for (Picture picture : picturesToClean) {
          try {
            questionPictureManager.remove(picture);
          } catch (Exception e) {
            logger.error(
                "Critical error during cleanup: Unable to delete picture {}.",
                picture.objectKey(),
                e);
          }
        }
      }
    }
  }
}
