package com.quezap.interfaces.api.v1.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadPaginationException extends RuntimeException {
  public BadPaginationException(String message) {
    super(message);
  }
}
