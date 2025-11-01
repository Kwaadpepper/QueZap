package com.quezap.interfaces.api.v1.validation;

import com.quezap.interfaces.api.v1.dto.request.PaginationDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PaginationValidator implements ConstraintValidator<PaginationRange, PaginationDto> {
  @Override
  public boolean isValid(PaginationDto dto, ConstraintValidatorContext context) {
    final var hasPageRange = dto.page() != null && dto.perPage() != null;
    final var hasIndexRange = dto.from() != null && dto.to() != null;

    context.disableDefaultConstraintViolation();

    if (hasPageRange && hasIndexRange) {
      context
          .buildConstraintViolationWithTemplate("Provide either page/perPage or from/to, not both")
          .addConstraintViolation();
      return false;
    }

    context
        .buildConstraintViolationWithTemplate(
            "Provide any of page/perPage or from/to for pagination")
        .addConstraintViolation();

    return hasPageRange || hasIndexRange || (!hasPageRange && !hasIndexRange);
  }
}
