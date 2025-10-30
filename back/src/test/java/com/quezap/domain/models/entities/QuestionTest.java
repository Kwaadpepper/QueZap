package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestionTest {

  @Test
  void canInstantiateBoolean() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var questionType = QuestionType.BOOLEAN;
    var value = "Is Paris the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers = Set.of(new Answer("Paris", null, true));
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN
    new Question(id, questionType, value, picture, themeId, answers, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateBinary() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var questionType = QuestionType.BINARY;
    var value = "What is the capital of France?";
    var hash = new Sha256Hash(new byte[32]);
    var picture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6b-0000-000000000000");
    var answers = Set.of(new Answer("Paris", null, true), new Answer("London", null, false));
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN
    new Question(id, questionType, value, picture, themeId, answers, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateQuizz() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN
    new Question(id, questionType, value, picture.get(), themeId, answers, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankQuestion() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }

  @Test
  void cannotInstantiateWithNoCorrectAnswer() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }

  @Test
  void cannotInstantiateWithTooManyAnswersOnBoolean() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }

  @Test
  void cannotInstantiateWithTooManyAnswersOnQuizz() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }

  @Test
  void cannotInstantiateWithDuplicateAnswers() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }

  @Test
  void cannotInstantiateWithMixedAnswerTypes() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
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
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Question(id, questionType, value, picture, themeId, answers, updatedAt);
        });
  }
}
