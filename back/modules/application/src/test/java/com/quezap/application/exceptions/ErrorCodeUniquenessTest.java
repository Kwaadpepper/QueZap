package com.quezap.application.exceptions;

import java.util.HashSet;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ErrorCodeUniquenessTest {
  private static final String BASE_PACKAGE = "com.quezap";

  @Test
  void checkUniqueness() throws ClassNotFoundException {
    final var uniqueCodes = new HashSet<Integer>();
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new AssignableTypeFilter(ApplicationErrorCode.class));

    final var candidates = scanner.findCandidateComponents(BASE_PACKAGE);

    Assertions.assertThat(candidates).isNotEmpty();

    for (BeanDefinition beanDef : candidates) {
      final var enumClass = Class.forName(beanDef.getBeanClassName());

      if (!enumClass.isEnum()) {
        continue;
      }

      Assertions.assertThat(ApplicationErrorCode.class).isAssignableFrom(enumClass);
      Assertions.assertThat(enumClass.getEnumConstants()).isNotEmpty();

      for (Object enumValue : enumClass.getEnumConstants()) {
        final var error = (ApplicationErrorCode) enumValue;
        final var code = error.getCode();

        Assertions.assertThat(uniqueCodes)
            .withFailMessage("Duplicate error code found: %d in %s", code, enumClass.getName())
            .doesNotContain(code);

        uniqueCodes.add(code);
      }
    }
  }
}
