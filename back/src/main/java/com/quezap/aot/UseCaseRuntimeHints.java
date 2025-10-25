package com.quezap.aot;

import org.springframework.aop.support.AopUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.quezap.lib.ddd.usecases.UseCaseHandler;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ImportRuntimeHints(UseCaseRuntimeHints.class)
public class UseCaseRuntimeHints implements RuntimeHintsRegistrar {
  private static final String BASE_PACKAGE = "com.quezap";
  private final Logger logger = LoggerFactory.getLogger(UseCaseRuntimeHints.class);

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {

    if (hints == null) {
      logger.warn("RuntimeHints is null, skipping UseCaseHandler registration.");
      return;
    }

    // Keep AopUtils for proxying UseCaseHandlers
    hints.reflection().registerType(AopUtils.class);

    findAndRegisterUseCaseHandlers(hints, BASE_PACKAGE);
  }

  private void findAndRegisterUseCaseHandlers(RuntimeHints hints, String basePackage) {
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);

    // Add a filter to include only classes that implement UseCaseHandler
    scanner.addIncludeFilter(new AssignableTypeFilter(UseCaseHandler.class));
    scanner
        .findCandidateComponents(basePackage)
        .forEach(beanDefinition -> registerUseCaseHandlerHint(hints, beanDefinition));
  }

  private void registerUseCaseHandlerHint(RuntimeHints hints, BeanDefinition beanDefinition) {
    final var useCaseHandlerClassName = beanDefinition.getBeanClassName();

    if (useCaseHandlerClassName == null) {
      throw new IllegalArgumentException("Bean class name is null");
    }

    try {

      final var typeRef = TypeReference.of(useCaseHandlerClassName);

      // Register the type for reflection
      hints.reflection().registerType(typeRef);

      logger.info("Registering AOT reflection hint for: {}", useCaseHandlerClassName);
    } catch (Exception e) {
      logger.error("Could not register AOT hint for: {}", useCaseHandlerClassName, e);
    }
  }
}
