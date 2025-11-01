package com.quezap.aot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.cglib.proxy.Factory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.quezap.infrastructure.adapter.spi.DataSource;
import com.quezap.lib.ddd.repositories.Repository;
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

    logger.info("Hints for Apache Tika resources");
    hints
        .resources()
        .registerPattern("org/apache/tika/mime/tika-mimetypes.xml")
        .registerPattern("org/apache/tika/parser/external/tika-external-parsers.xml");

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
        UseCaseHandler.class,
        "Hint reflection for UseCaseHandler:",
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

    registerJdkProxyWithSpringAop(hints, BASE_PACKAGE, DataSource.class, Repository.class);

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

  private void registerJdkProxyWithSpringAop(
      RuntimeHints hints, String basePackage, Class<?> withInterface, Class<?>... withInterfaces) {
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    final var interfaceSet = new HashSet<Class<?>>();
    interfaceSet.add(withInterface);
    interfaceSet.addAll(List.of(withInterfaces));

    // * Add filters for all interfaces
    interfaceSet.forEach(iClass -> scanner.addIncludeFilter(new AssignableTypeFilter(iClass)));

    scanner
        .findCandidateComponents(basePackage)
        .forEach(
            beanDef -> {
              try {
                final var implementClass = Class.forName(beanDef.getBeanClassName());

                // * Skip if it's one of the base interfaces
                if (interfaceSet.stream().anyMatch(implementClass::equals)) {
                  return;
                }

                // * Extract only the interfaces from the implementation class
                final var implementedInterfaces =
                    extractImplementedInterfaces(implementClass, interfaceSet);

                if (implementedInterfaces.isEmpty()) {
                  return;
                }

                final var classNames =
                    implementedInterfaces.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(" + "));

                logger.info("Hint proxy for {}: {}", implementClass.getSimpleName(), classNames);
                registerProxy(hints, implementedInterfaces);
              } catch (ClassNotFoundException e) {
                throw new AotHintException(e);
              }
            });
  }

  private List<Class<?>> extractImplementedInterfaces(
      Class<?> clazz, Set<Class<?>> targetInterfaces) {
    final var result = new ArrayList<Class<?>>();

    // * Get all interfaces implemented by the class (including inherited ones)
    for (final var implemented : clazz.getInterfaces()) {
      // * Check if the implemented interface matches any of the target interfaces
      if (targetInterfaces.stream().anyMatch(target -> target.isAssignableFrom(implemented))) {
        result.add(implemented);
      }
    }

    return result;
  }

  private void registerProxy(RuntimeHints hints, List<Class<?>> classes) {
    final var proxyInterfaces = new ArrayList<Class<?>>();

    proxyInterfaces.add(SpringProxy.class);
    proxyInterfaces.add(Advised.class);
    proxyInterfaces.add(Factory.class);
    proxyInterfaces.addAll(classes);
    proxyInterfaces.add(DecoratingProxy.class);

    hints.proxies().registerJdkProxy(proxyInterfaces.toArray(new Class<?>[0]));
  }

  private static class AotHintException extends RuntimeException {
    public AotHintException(Exception e) {
      super(e);
    }
  }
}
