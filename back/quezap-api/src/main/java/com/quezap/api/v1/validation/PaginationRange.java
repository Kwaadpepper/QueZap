package com.quezap.api.v1.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PaginationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PaginationRange {
  String message() default "Either page/perPage or from/to must be provided";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
