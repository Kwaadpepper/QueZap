package com.quezap.domain.models.valueobjects;

import java.util.UUID;

import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PictureTest {

  @Test
  void canInstantiatePicture() {
    // GIVEN
    var uri = UuidV7.randomUuid() + ".jpg";
    var pictureType = PictureType.JPG;
    var hash = new Sha256Hash(new byte[32]);

    // WHEN
    new Picture(uri, pictureType, hash);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiatePictureWithAnotherType() {
    // GIVEN
    var uri = UuidV7.randomUuid() + ".png";
    var pictureType = PictureType.PNG;
    var hash = new Sha256Hash(new byte[32]);

    // WHEN
    new Picture(uri, pictureType, hash);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiatePictureWithPathPrefix() {
    // GIVEN
    var uri = "picture/" + UuidV7.randomUuid() + ".png";
    var pictureType = PictureType.PNG;
    var hash = new Sha256Hash(new byte[32]);

    // WHEN
    new Picture(uri, pictureType, hash);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiatePictureWithInvalidUuid() {
    // GIVEN
    var uri = "invalid-uuid.jpg";
    var pictureType = PictureType.JPG;
    var hash = new Sha256Hash(new byte[32]);
    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Picture(uri, pictureType, hash);
        });
  }

  @Test
  void cannotInstantiatePictureWithUuidNotV7() {
    // GIVEN
    var uri = "picture/" + UUID.randomUUID().toString() + ".png";
    var pictureType = PictureType.JPG;
    var hash = new Sha256Hash(new byte[32]);

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Picture(uri, pictureType, hash);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {".", "", ".test", ".webp"})
  void cannotInstanciateWithWrongPictureExtension(String ext) {
    // GIVEN
    var uri = "picture/" + UuidV7.randomUuid() + ext;
    var pictureType = PictureType.PNG;
    var hash = new Sha256Hash(new byte[32]);

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Picture(uri, pictureType, hash);
        });
  }
}
