package com.quezap.application.exceptions;

import java.io.Serializable;

public interface ApplicationErrorCode extends Serializable {
  int getCode();

  String getMessage();
}
