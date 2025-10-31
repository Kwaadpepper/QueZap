package com.quezap.domain.models.entities;

import java.util.Set;
import java.util.function.Supplier;

import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestionTest {

  @Test
  void canInstantiateBoolean() {
    // GIVEN
    var questionType = QuestionType.BOOLEAN;
    var value = "Is Paris the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers = Set.of(new Answer("Paris", null, true));

    // WHEN
    new Question(questionType, value, picture, themeId, answers);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateBinary() {
    // GIVEN
    var questionType = QuestionType.BINARY;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers = Set.of(new Answer("Paris", null, true), new Answer("London", null, false));

    // WHEN
    new Question(questionType, value, picture, themeId, answers);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  @SuppressWarnings("DistinctVarargsChecker")
  void canInstantiateQuizz() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    Supplier<Picture> picture =
        () -> {
          return new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
        };
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer(null, picture.get(), true),
            new Answer(null, picture.get(), false),
            new Answer(null, picture.get(), false),
            new Answer(null, picture.get(), false));

    // WHEN
    new Question(questionType, value, picture.get(), themeId, answers);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.<Answer>of(
            new Answer("Paris", null, true),
            new Answer("London", null, false),
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false));
    var id = UuidV7.randomUuid();
    var updatedAt = TimelinePoint.now();

    // WHEN
    Question.hydrate(id, questionType, value, picture, themeId, answers, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankQuestion() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "   ";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", null, true),
            new Answer("London", null, false),
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }

  @Test
  void cannotInstantiateWithNoCorrectAnswer() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", null, false),
            new Answer("London", null, false),
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }

  @Test
  void cannotInstantiateWithTooManyAnswersOnBoolean() {
    // GIVEN
    var questionType = QuestionType.BOOLEAN;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", null, true),
            new Answer("London", null, false),
            new Answer("Berlin", null, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }

  @Test
  void cannotInstantiateWithTooManyAnswersOnQuizz() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", null, true),
            new Answer("London", null, false),
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false),
            new Answer("Rome", null, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }

  @Test
  void cannotInstantiateWithDuplicateAnswers() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var picture2 = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", picture, true),
            new Answer("Paris", picture2, false), // Duplicate answer
            new Answer("Berlin", picture, false),
            new Answer("Madrid", picture, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }

  @Test
  void cannotInstantiateWithMixedAnswerTypes() {
    // GIVEN
    var questionType = QuestionType.QUIZZ;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers =
        Set.of(
            new Answer("Paris", null, true), // Text answer
            new Answer(
                null,
                new Picture(
                    UuidV7.randomUuid() + ".jpg", PictureType.JPG, new Sha256Hash(new byte[32])),
                false), // Picture answer
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(questionType, value, picture, themeId, answers);
        });
  }
}
