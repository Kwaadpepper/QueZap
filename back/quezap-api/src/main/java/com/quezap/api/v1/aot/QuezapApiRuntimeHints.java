package com.quezap.api.v1.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import com.quezap.api.v1.services.SessionSseService;

import org.jspecify.annotations.Nullable;

public class QuezapApiRuntimeHints implements RuntimeHintsRegistrar {
  private static final String DTO_PACKAGE = "com.quezap.api.v1.dto";
  private static final String ERRORS_PACKAGE = "com.quezap.api.errors";

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (hints == null) {
      return;
    }

    registerPackageTypes(hints, DTO_PACKAGE, classLoader);
    registerPackageTypes(hints, ERRORS_PACKAGE, classLoader);
    registerReflection(hints, SessionSseService.StoredEvent.class);
  }

  private void registerPackageTypes(
      RuntimeHints hints, String basePackage, @Nullable ClassLoader classLoader) {
    final var scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isIndependent();
          }
        };
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

    final var effectiveClassLoader =
        classLoader != null ? classLoader : QuezapApiRuntimeHints.class.getClassLoader();

    for (final var beanDef : scanner.findCandidateComponents(basePackage)) {
      final var className = beanDef.getBeanClassName();
      if (className == null || className.endsWith(".package-info")) {
        continue;
      }
      try {
        final var clazz = Class.forName(className, false, effectiveClassLoader);
        registerReflection(hints, clazz);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Failed to register AOT hint for " + className, e);
      }
    }
  }

  private void registerReflection(RuntimeHints hints, Class<?> clazz) {
    hints
        .reflection()
        .registerType(
            clazz,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS);
  }
}
