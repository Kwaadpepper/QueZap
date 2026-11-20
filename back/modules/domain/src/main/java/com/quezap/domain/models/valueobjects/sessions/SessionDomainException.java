package com.quezap.domain.models.valueobjects.sessions;

public final class SessionDomainException extends RuntimeException {

  public enum ErrorCategory {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT
  }

  private final ErrorCategory category;

  public SessionDomainException(ErrorCategory category, String message) {
    super(message);
    this.category = category;
  }

  public ErrorCategory category() {
    return category;
  }

  public static SessionDomainException badRequest(String message) {
    return new SessionDomainException(ErrorCategory.BAD_REQUEST, message);
  }

  public static SessionDomainException unauthorized(String message) {
    return new SessionDomainException(ErrorCategory.UNAUTHORIZED, message);
  }

  public static SessionDomainException forbidden(String message) {
    return new SessionDomainException(ErrorCategory.FORBIDDEN, message);
  }

  public static SessionDomainException notFound(String message) {
    return new SessionDomainException(ErrorCategory.NOT_FOUND, message);
  }

  public static SessionDomainException conflict(String message) {
    return new SessionDomainException(ErrorCategory.CONFLICT, message);
  }
}
