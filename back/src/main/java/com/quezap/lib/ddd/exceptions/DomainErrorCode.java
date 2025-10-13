package com.quezap.lib.ddd.exceptions;

import java.io.Serializable;

public interface DomainErrorCode extends Serializable {
  int getCode();

  String getMessage();
}
