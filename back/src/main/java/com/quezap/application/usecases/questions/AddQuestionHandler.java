package com.quezap.application.usecases.questions;

import java.util.Set;
import java.util.stream.Collectors;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.questions.AddQuestion.AddQuestionUseCase;
import com.quezap.application.ports.questions.AddQuestion.Input;
import com.quezap.application.ports.questions.AddQuestion.Output;
import com.quezap.domain.errors.questions.AddQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.EmptyConsumer;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Usecase
final class AddQuestionHandler implements AddQuestionUseCase {
  private static final Logger logger = LoggerFactory.getLogger(AddQuestionHandler.class);
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager questionPictureManager;

  public AddQuestionHandler(
      QuestionRepository questionRepository,
      ThemeRepository themeRepository,
      QuestionPictureManager questionPictureManager) {
    this.questionRepository = questionRepository;
    this.themeRepository = themeRepository;
    this.questionPictureManager = questionPictureManager;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    return switch (usecaseInput) {
      case Input.Affirmation affirmation -> handleAffirmation(affirmation, unitOfWork);
      case Input.Binary binaryQuestion -> handleBinary(binaryQuestion, unitOfWork);
      case Input.Quizz quiz -> handleQuiz(quiz, unitOfWork);
    };
  }

  private Output handleAffirmation(Input.Affirmation input, UnitOfWorkEvents unitOfWork) {
    final var questionValue = input.question();
    final var isTrue = input.isTrue();
    final var pictureBytes = input.picture();
    final var themeId = input.theme();

    themeRepository
        .find(themeId)
        .ifPresentOrElse(
            EmptyConsumer.accept(),
            DomainConstraintException.throwWith(AddQuestionError.THEME_DOES_NOT_EXISTS));

    final var picture = storePicture(pictureBytes, unitOfWork);
    final var answer = new Answer(isTrue ? "True" : "False", null, isTrue);
    final var question =
        createQuestion(
            QuestionType.BOOLEAN, questionValue, picture, themeId, Set.<Answer>of(answer));

    questionRepository.persist(question);

    return new Output(question.getId());
  }

  private Output handleBinary(Input.Binary input, UnitOfWorkEvents unitOfWork) {
    final var questionValue = input.question();
    final var pictureBytes = input.picture();
    final var themeId = input.theme();
    final var answers =
        input.answers().stream()
            .<Answer>map(a -> answerDataMapper(a, unitOfWork))
            .collect(Collectors.toSet());

    themeRepository
        .find(themeId)
        .ifPresentOrElse(
            EmptyConsumer.accept(),
            DomainConstraintException.throwWith(AddQuestionError.THEME_DOES_NOT_EXISTS));

    final var picture = storePicture(pictureBytes, unitOfWork);
    final var question =
        createQuestion(QuestionType.BINARY, questionValue, picture, themeId, answers);

    questionRepository.persist(question);

    return new Output(question.getId());
  }

  private Output handleQuiz(Input.Quizz input, UnitOfWorkEvents unitOfWork) {
    final var questionValue = input.question();
    final var pictureBytes = input.picture();
    final var themeId = input.theme();
    final var answers =
        input.answers().stream()
            .<Answer>map(a -> answerDataMapper(a, unitOfWork))
            .collect(Collectors.toSet());

    themeRepository
        .find(themeId)
        .ifPresentOrElse(
            EmptyConsumer.accept(),
            DomainConstraintException.throwWith(AddQuestionError.THEME_DOES_NOT_EXISTS));

    final var picture = storePicture(pictureBytes, unitOfWork);
    final var question =
        createQuestion(QuestionType.QUIZZ, questionValue, picture, themeId, answers);

    questionRepository.persist(question);

    return new Output(question.getId());
  }

  private Question createQuestion(
      QuestionType type,
      String value,
      @Nullable Picture picture,
      ThemeId theme,
      Set<Answer> answers) {
    try {
      return new Question(type, value, picture, theme, answers);
    } catch (IllegalDomainStateException e) {
      // * Convert to a more specific domain exception with relevant error message
      throw new DomainConstraintException(AddQuestionError.INVALID_QUESTION_DATA, e.getMessage());
    }
  }

  private @Nullable Picture storePicture(
      @Nullable PictureUploadData pictureData, UnitOfWorkEvents unitOfWork) {
    if (pictureData == null) {
      return null;
    }

    final var picture = questionPictureManager.store(pictureData);

    unitOfWork.onRollback(() -> removePicture(picture));

    return picture;
  }

  private void removePicture(Picture picture) {
    try {
      questionPictureManager.remove(picture);
    } catch (Exception e) {
      logger.error(
          "Critical error during cleanup: Unable to delete picture {}.", picture.objectKey(), e);
    }
  }

  private Answer answerDataMapper(Input.AnswerData answerData, UnitOfWorkEvents unitOfWork) {
    final var answerText = answerData.answerText();
    final var pictureBytes = answerData.picture();
    final var isCorrect = answerData.isCorrect();
    final var picture = storePicture(pictureBytes, unitOfWork);

    return new Answer(answerText, picture, isCorrect);
  }
}
