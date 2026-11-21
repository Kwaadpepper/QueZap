package com.quezap.infrastructure.aot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cglib.proxy.Factory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.spi.DataSource;
import com.quezap.lib.ddd.repositories.Repository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.usecases.UsecaseHandler;

import jakarta.persistence.Entity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ImportRuntimeHints(QuezapRuntimeHints.class)
public class QuezapRuntimeHints implements RuntimeHintsRegistrar {
  private static final String BASE_PACKAGE = "com.quezap";
  private static final String JPA_ENTITIES_PACKAGE =
      "com.quezap.infrastructure.adapter.persistence.jpa.entities";
  private final Logger logger = LoggerFactory.getLogger(QuezapRuntimeHints.class);

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {

    if (hints == null) {
      logger.warn("RuntimeHints is null, skipping hint registration.");
      return;
    }

    logger.info("Starting registration of custom infrastructure AOT hints...");

    findAndRegisterForSpringAopProxy(
        hints,
        BASE_PACKAGE,
        DataSource.class,
        Repository.class,
        UsecaseExecutor.class,
        UsecaseHandler.class);

    registerJpaEntities(hints);
    registerJjwtHints(hints);

    logger.info("Hint resources for profanity-filter dictionaries registered (dictionary.*)");
    hints.resources().registerPattern("com/modernmt/text/profanity/dictionary.*");

    logger.info("Hints for Apache Tika and ImageIO resources");
    hints
        .resources()
        .registerPattern("org/apache/tika/mime/tika-mimetypes.xml")
        .registerPattern("org/apache/tika/mime/custom-mimetypes.xml")
        .registerPattern("org/apache/tika/parser/external/tika-external-parsers.xml")
        .registerPattern("META-INF/services/org.apache.tika.*")
        .registerPattern("META-INF/services/javax.imageio.*");

    logger.info("Finished registration of custom AOT hints.");
  }

  private void registerJjwtHints(RuntimeHints hints) {
    // JJWT 0.13.x loads all implementation suppliers, registries, and SPI providers via
    // Class.forName / ServiceLoader; register them explicitly for GraalVM Native Image.
    final var jjwtReflectiveClasses =
        List.of(
            "io.jsonwebtoken.impl.DefaultClaimsBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtHeaderBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtParserBuilder$Supplier",
            "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms",
            "io.jsonwebtoken.impl.compression.DeflateCompressionAlgorithm",
            "io.jsonwebtoken.impl.compression.GzipCompressionAlgorithm",
            "io.jsonwebtoken.impl.security.DefaultDynamicJwkBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkParserBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkSetBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkSetParserBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultKeyOperationBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultKeyOperationPolicyBuilder$Supplier",
            "io.jsonwebtoken.impl.security.JwksBridge",
            "io.jsonwebtoken.impl.security.KeysBridge",
            "io.jsonwebtoken.impl.security.StandardCurves",
            "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms",
            "io.jsonwebtoken.impl.security.StandardHashAlgorithms",
            "io.jsonwebtoken.impl.security.StandardKeyAlgorithms",
            "io.jsonwebtoken.impl.security.StandardKeyOperations",
            "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms",
            "io.jsonwebtoken.jackson.io.JacksonDeserializer",
            "io.jsonwebtoken.jackson.io.JacksonSerializer");

    for (final var className : jjwtReflectiveClasses) {
      hints
          .reflection()
          .registerType(
              TypeReference.of(className),
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.INVOKE_DECLARED_METHODS,
              MemberCategory.DECLARED_FIELDS);
    }
    hints.resources().registerPattern("META-INF/services/io.jsonwebtoken.*");
    logger.info(
        "Registered {} JJWT reflection hints and SPI services.", jjwtReflectiveClasses.size());
  }

  private void registerJpaEntities(RuntimeHints hints) {
    registerEntityReflection(hints, StagedPictureJpaEntity.class);

    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
    scanner
        .findCandidateComponents(JPA_ENTITIES_PACKAGE)
        .forEach(
            beanDef -> {
              final var className = beanDef.getBeanClassName();
              if (className == null) {
                return;
              }
              try {
                registerEntityReflection(hints, Class.forName(className));
              } catch (ClassNotFoundException e) {
                throw new AotHintException(e);
              }
            });
  }

  private void registerEntityReflection(RuntimeHints hints, Class<?> entityClass) {
    logger.info("Hint reflection for JPA entity: {}", entityClass.getName());
    hints
        .reflection()
        .registerType(
            entityClass,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS);
  }

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
    final var allInterfaces = Arrays.asList(ClassUtils.getAllInterfacesForClass(clazz));
    final var matchesTarget =
        allInterfaces.stream()
            .anyMatch(
                implemented ->
                    targetInterfaces.stream()
                        .anyMatch(target -> target.isAssignableFrom(implemented)));

    // Spring's ProxyFactory(bean) proxies ALL interfaces implemented by the bean (e.g.,
    // UserRepository + UserDirectory), not only those extending Repository.
    return matchesTarget ? new ArrayList<>(allInterfaces) : List.of();
  }

  private void registerSpringAopProxy(RuntimeHints hints, List<Class<?>> classes) {
    // Variant 1: JDK proxy wrapping an already CGLIB-proxied Spring bean (@Repository /
    // @Transactional)
    final var cglibWrappedInterfaces = new ArrayList<Class<?>>();
    cglibWrappedInterfaces.add(SpringProxy.class);
    cglibWrappedInterfaces.add(Advised.class);
    cglibWrappedInterfaces.add(Factory.class);
    cglibWrappedInterfaces.addAll(classes);
    cglibWrappedInterfaces.add(DecoratingProxy.class);
    hints.proxies().registerJdkProxy(cglibWrappedInterfaces.toArray(new Class<?>[0]));

    // Variant 2: Direct JDK proxy on a non-CGLIB-wrapped bean
    final var directInterfaces = new ArrayList<Class<?>>(classes);
    directInterfaces.add(SpringProxy.class);
    directInterfaces.add(Advised.class);
    directInterfaces.add(DecoratingProxy.class);
    hints.proxies().registerJdkProxy(directInterfaces.toArray(new Class<?>[0]));
  }

  private static class AotHintException extends RuntimeException {
    AotHintException(Exception e) {
      super(e);
    }
  }
}
