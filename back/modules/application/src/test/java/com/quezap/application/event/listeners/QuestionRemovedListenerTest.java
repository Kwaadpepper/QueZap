package com.quezap.application.event.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.domain.events.questions.QuestionDeleted;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionRemovedListenerTest {

  private QuestionPictureManager pictureManager;
  private QuezapDataPort quezapDataPort;
  private QuestionRemovedListener listener;

  @BeforeEach
  void setUp() {
    pictureManager = mock(QuestionPictureManager.class);
    quezapDataPort = mock(QuezapDataPort.class);
    listener = new QuestionRemovedListener(pictureManager, quezapDataPort);
  }

  @Test
  void shouldNotDeletePictureFromS3WhenStillReferencedByAnotherQuestionOrAnswer() {
    final var picture =
        new Picture(
            "pictures/questions/" + UuidV7.randomUuid() + ".png",
            PictureType.PNG,
            new Sha256Hash(new byte[32]));
    when(quezapDataPort.isPictureReferenced(anyCollection())).thenReturn(true);

    listener.onEvent(new QuestionDeleted(Set.of(picture)));

    verify(pictureManager, never()).remove(any());
  }

  @Test
  void shouldDeletePictureFromS3WhenNoReferencesRemain() {
    final var picture =
        new Picture(
            "pictures/questions/" + UuidV7.randomUuid() + ".png",
            PictureType.PNG,
            new Sha256Hash(new byte[32]));
    when(quezapDataPort.isPictureReferenced(anyCollection())).thenReturn(false);

    listener.onEvent(new QuestionDeleted(Set.of(picture)));

    verify(pictureManager).remove(picture);
  }
}
