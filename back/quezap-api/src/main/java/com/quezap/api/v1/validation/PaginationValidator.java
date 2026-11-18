package com.quezap.api.v1.validation;

import com.quezap.api.v1.dto.request.PaginationDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PaginationValidator implements ConstraintValidator<PaginationRange, PaginationDto> {
  @Override
  public boolean isValid(PaginationDto dto, ConstraintValidatorContext context) {
    final var hasAnyPage = dto.page() != null || dto.perPage() != null;
    final var hasCompletePage = dto.page() != null && dto.perPage() != null;

    final var hasAnyIndex = dto.from() != null || dto.to() != null;
    final var hasCompleteIndex = dto.from() != null && dto.to() != null;

    final var hasAnyOffset = dto.offset() != null || dto.limit() != null;
    final var hasCompleteOffset = dto.offset() != null && dto.limit() != null;

    context.disableDefaultConstraintViolation();

    if ((hasAnyPage && !hasCompletePage)
        || (hasAnyIndex && !hasCompleteIndex)
        || (hasAnyOffset && !hasCompleteOffset)) {
      context
          .buildConstraintViolationWithTemplate(
              "Provide complete pairs: page/perPage, from/to, or offset/limit")
          .addConstraintViolation();
      return false;
    }

    final int modesCount =
        (hasCompletePage ? 1 : 0) + (hasCompleteIndex ? 1 : 0) + (hasCompleteOffset ? 1 : 0);

    if (modesCount > 1) {
      context
          .buildConstraintViolationWithTemplate(
              "Provide only one pagination mode: page/perPage, from/to, or offset/limit")
          .addConstraintViolation();
      return false;
    }

    return true;
  }
}
