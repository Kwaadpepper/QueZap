package com.quezap.domain.models.valueobjects;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PictureTest {

  @Test
  void canInstantiatePicture() {
    // GIVEN
    var uri = java.net.URI.create("path/to/picture.jpg");
    var pictureType = PictureType.JPG;

    // WHEN
    new Picture(uri, pictureType);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiatePictureWithInvalidUri() {
    // GIVEN
    var uri = java.net.URI.create("invalid_uri");
    var pictureType = PictureType.PNG;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Picture(uri, pictureType);
        });
  }

  @Test
  void cannotInstantiatePictureWithHttpUri() {
    // GIVEN
    var uri = java.net.URI.create("https://example.com/picture.png");
    var pictureType = PictureType.PNG;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Picture(uri, pictureType);
        });
  }
}
