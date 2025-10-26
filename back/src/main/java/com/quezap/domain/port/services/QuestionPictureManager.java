package com.quezap.domain.port.services;

import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;

public interface QuestionPictureManager {
  Picture store(PictureUploadData uploadData);

  boolean exists(Picture picture);

  Picture copy(Picture picture);

  void remove(Picture picture);
}
