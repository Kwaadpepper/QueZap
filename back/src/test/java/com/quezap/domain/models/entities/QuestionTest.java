package com.quezap.domain.models.entities;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.Answer;
import com.quezap.domain.models.valueobjects.Picture;
import com.quezap.domain.models.valueobjects.PictureType;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.questions.QuestionType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestionTest {

  @Test
  void canInstantiateBoolean() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var questionType = QuestionType.BOOLEAN;
    var value = "What is the capital of France?";
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
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
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
    var answers =
        Set.of(
            new Answer("Paris", null, true),
            new Answer("London", null, false),
            new Answer("Berlin", null, false),
            new Answer("Madrid", null, false));
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN
    new Question(id, questionType, value, picture, themeId, answers, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankQuestion() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var questionType = QuestionType.QUIZZ;
    var value = "   ";
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
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
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
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
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
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
    var picture = new Picture(URI.create("picture.jpg"), PictureType.JPG);
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6b-0000-000000000000"));
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
}
