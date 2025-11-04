package com.quezap.application.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.quezap.lib.ddd.usecases.UsecaseHandler;

import com.fasterxml.jackson.databind.JsonDeserializer;
import jakarta.validation.ConstraintValidator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ImportRuntimeHints(QuezapRuntimeHints.class)
public class QuezapRuntimeHints implements RuntimeHintsRegistrar {
  private static final String BASE_PACKAGE = "com.quezap";
  private final Logger logger = LoggerFactory.getLogger(QuezapRuntimeHints.class);

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {

    if (hints == null) {
      logger.warn("RuntimeHints is null, skipping hint registration.");
      return;
    }

    logger.info("Starting registration of custom application AOT hints...");

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        ConstraintValidator.class,
        "Hint reflection for PaginationValidator:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        Converter.class,
        "Hint reflection for Converter:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        JsonDeserializer.class,
        "Hint reflection for JsonDeserializer:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        UsecaseHandler.class,
        "Hint reflection for UsecaseHandler:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        Record.class,
        "Hint reflection for Record:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.DECLARED_FIELDS);

    logger.info("Finished registration of custom AOT hints.");
  }

  private void findAndRegisterForReflection(
      RuntimeHints hints,
      String basePackage,
      Class<?> superType,
      String logMessage,
      MemberCategory... memberCategories) {

    final var typeFilter = new AssignableTypeFilter(superType);
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(typeFilter);
    scanner
        .findCandidateComponents(basePackage)
        .forEach(
            beanDef -> {
              try {
                logger.info("{} {}", logMessage, beanDef.getBeanClassName());
                hints
                    .reflection()
                    .registerType(Class.forName(beanDef.getBeanClassName()), memberCategories);
              } catch (ClassNotFoundException e) {
                throw new AotHintException(e);
              }
            });
  }

  private static class AotHintException extends RuntimeException {
    AotHintException(Exception e) {
      super(e);
    }
  }
}
