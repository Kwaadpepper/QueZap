package com.quezap.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.quezap.lib.ddd.usecases.UseCaseHandler;

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

    logger.info("Starting registration of custom AOT hints...");

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        ConstraintValidator.class,
        "Registering hint for PaginationValidator:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        Converter.class,
        "Registering hint for Converter:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        JsonDeserializer.class,
        "Registering hint for JsonDeserializer:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        UseCaseHandler.class,
        "Registering hint for UseCaseHandler:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS);

    findAndRegisterForReflection(
        hints,
        BASE_PACKAGE,
        Record.class,
        "Registering hint for Record:",
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.DECLARED_FIELDS);

    logger.info("Finished registration of custom AOT hints.");
  }

  /**
   * Find and register classes for AOT reflection based on a parent type.
   *
   * @param hints The RuntimeHints instance.
   * @param basePackage The base package to scan.
   * @param superType The parent type (class or interface) to look for.
   * @param logMessage The log message to display for each found class.
   * @param memberCategories The member categories to register for reflection.
   */
  private void findAndRegisterForReflection(
      RuntimeHints hints,
      String basePackage,
      Class<?> superType,
      String logMessage,
      MemberCategory... memberCategories) {
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(superType));

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
                logger.error("Could not find class for hint: {}", beanDef.getBeanClassName(), e);
              }
            });
  }
}
