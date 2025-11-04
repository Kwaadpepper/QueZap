package com.quezap.domain.models.valueobjects;

import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.UuidV7;

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
    var hash = new Sha256Hash(new byte[32]);
    var answerPicture = new Picture(UuidV7.randomUuid() + ".jpg", PictureType.JPG, hash);
    var isCorrect = false;

    // WHEN
    new Answer(answerText, answerPicture, isCorrect);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateAnswerWithBothTextAndPicture() {
    // GIVEN
    var answerText = "This is an answer";
    var hash = new Sha256Hash(new byte[32]);
    var answerPicture = new Picture(UuidV7.randomUuid() + ".png", PictureType.PNG, hash);
    var isCorrect = true;

    // WHEN
    new Answer(answerText, answerPicture, isCorrect);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
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
