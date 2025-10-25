package com.quezap.domain.port.services;

import java.util.stream.Stream;

import com.quezap.domain.models.valueobjects.Picture;

public interface QuestionPictureManager {
  Picture store(Stream<Byte> pictureData);

  boolean exists(Picture picture);

  Picture copy(Picture picture);

  void remove(Picture picture);
}
