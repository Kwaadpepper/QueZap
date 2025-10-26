package com.quezap.domain.models.valueobjects;

import java.net.URI;

import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AnswerTest {
  @Test
  void canInstantiateTextAnswer() {
    // GIVEN
    var answerText = "This is an answer";
    Picture answerPicture = null;
    var isCorrect = true;

    // WHEN
    new Answer(answerText, answerPicture, isCorrect);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiatePictureAnswer() {
    // GIVEN
    String answerText = null;
    var answerPicture = new Picture(URI.create("path/to/picture.jpg"), PictureType.JPG);
    var isCorrect = false;

    // WHEN
    new Answer(answerText, answerPicture, isCorrect);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateAnswerWithBothTextAndPicture() {
    // GIVEN
    var answerText = "This is an answer";
    var answerPicture = new Picture(URI.create("path/to/picture.png"), PictureType.PNG);
    var isCorrect = true;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Answer(answerText, answerPicture, isCorrect);
        });
  }

  @Test
  void cannotInstantiateAnswerWithNeitherTextNorPicture() {
    // GIVEN
    String answerText = null;
    Picture answerPicture = null;
    var isCorrect = false;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Answer(answerText, answerPicture, isCorrect);
        });
  }

  @Test
  void cannotInstantiateAnswerWithBlankText() {
    // GIVEN
    var answerText = "   ";
    Picture answerPicture = null;
    var isCorrect = true;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Answer(answerText, answerPicture, isCorrect);
        });
  }
}
