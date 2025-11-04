package com.quezap.infrastructure.aot;

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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cglib.proxy.Factory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.quezap.infrastructure.adapter.spi.DataSource;
import com.quezap.lib.ddd.repositories.Repository;

import io.jsonwebtoken.impl.security.KeysBridge;
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

    logger.info("Starting registration of custom infrastructure AOT hints...");

    findAndRegisterForSpringAopProxy(hints, BASE_PACKAGE, DataSource.class, Repository.class);

    // Ensure JJWT internal SPI class loaded via Class.forName is reachable in native image
    hints
        .reflection()
        .registerType(
            KeysBridge.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS);
    logger.info("Hint reflection for JJWT: {}", KeysBridge.class.getName());

    logger.info("Hint resources for profanity-filter dictionaries registered (dictionary.*)");
    hints.resources().registerPattern("com/modernmt/text/profanity/dictionary.*");

    logger.info("Hints for Apache Tika resources");
    hints
        .resources()
        .registerPattern("org/apache/tika/mime/tika-mimetypes.xml")
        .registerPattern("org/apache/tika/parser/external/tika-external-parsers.xml");

    logger.info("Finished registration of custom AOT hints.");
  }

  // (kept for future generic scanning if needed)

  private void findAndRegisterForSpringAopProxy(
      RuntimeHints hints, String basePackage, Class<?> withInterface, Class<?>... withInterfaces) {

    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    final var withInterfaceSet = new HashSet<Class<?>>();

    withInterfaceSet.add(withInterface);
    withInterfaceSet.addAll(List.of(withInterfaces));

    // * Add filters for all interfaces
    withInterfaceSet.forEach(
        interfaceClass -> scanner.addIncludeFilter(new AssignableTypeFilter(interfaceClass)));

    scanner
        .findCandidateComponents(basePackage)
        .forEach(beanDef -> registerProxy(hints, beanDef, withInterfaceSet));
  }

  private void registerProxy(
      RuntimeHints hints, BeanDefinition beanDef, Set<Class<?>> withInterfaceSet) {
    try {
      final var implementClass = Class.forName(beanDef.getBeanClassName());

      // * Skip if it's one of the base interfaces
      if (withInterfaceSet.stream().anyMatch(implementClass::equals)) {
        return;
      }

      // * Extract only the interfaces from the implementation class
      final var implementedInterfaces =
          extractImplementedInterfaces(implementClass, withInterfaceSet);

      if (implementedInterfaces.isEmpty()) {
        return;
      }

      final var classNames =
          implementedInterfaces.stream()
              .map(Class::getSimpleName)
              .collect(Collectors.joining(" + "));

      logger.info("Hint proxy for {}: {}", implementClass.getSimpleName(), classNames);
      registerSpringAopProxy(hints, implementedInterfaces);
    } catch (ClassNotFoundException e) {
      throw new AotHintException(e);
    }
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

  private void registerSpringAopProxy(RuntimeHints hints, List<Class<?>> classes) {
    final var proxyInterfaces = new ArrayList<Class<?>>();

    proxyInterfaces.add(SpringProxy.class);
    proxyInterfaces.add(Advised.class);
    proxyInterfaces.add(Factory.class);
    proxyInterfaces.addAll(classes);
    proxyInterfaces.add(DecoratingProxy.class);

    hints.proxies().registerJdkProxy(proxyInterfaces.toArray(new Class<?>[0]));
  }

  private static class AotHintException extends RuntimeException {
    AotHintException(Exception e) {
      super(e);
    }
  }
}
