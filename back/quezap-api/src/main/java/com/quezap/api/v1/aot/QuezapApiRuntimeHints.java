package com.quezap.api.v1.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import com.quezap.api.v1.services.SessionSseService;
import com.quezap.api.v1.services.TurnstileService;

import org.jspecify.annotations.Nullable;

public class QuezapApiRuntimeHints implements RuntimeHintsRegistrar {
  private static final String DTO_PACKAGE = "com.quezap.api.v1.dto";
  private static final String ERRORS_PACKAGE = "com.quezap.api.errors";

  private static final String[] MAIL_RESOURCES = {
    "META-INF/javamail.default.providers",
    "META-INF/javamail.default.address.map",
    "META-INF/javamail.providers",
    "META-INF/javamail.address.map",
    "META-INF/javamail.charset.map",
    "META-INF/mailcap",
    "META-INF/mailcap.default",
    "META-INF/services/jakarta.mail.Provider",
    "META-INF/services/jakarta.mail.util.StreamProvider",
    "META-INF/services/jakarta.activation.spi.MailcapRegistryProvider",
    "META-INF/services/jakarta.activation.spi.MimeTypeRegistryProvider",
  };

  private static final String[] MAIL_REFLECTION_CLASSES = {
    "org.eclipse.angus.mail.smtp.SMTPTransport",
    "org.eclipse.angus.mail.smtp.SMTPSSLTransport",
    "org.eclipse.angus.mail.smtp.SMTPProvider",
    "org.eclipse.angus.mail.smtp.SMTPSSLProvider",
    "org.eclipse.angus.mail.handlers.text_plain",
    "org.eclipse.angus.mail.handlers.text_html",
    "org.eclipse.angus.mail.handlers.text_xml",
    "org.eclipse.angus.mail.handlers.multipart_mixed",
    "org.eclipse.angus.mail.handlers.message_rfc822",
    "org.eclipse.angus.mail.util.AngusStreamProvider",
    "org.eclipse.angus.activation.MailcapRegistryProviderImpl",
    "org.eclipse.angus.activation.MimeTypeRegistryProviderImpl",
  };

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (hints == null) {
      return;
    }

    registerPackageTypes(hints, DTO_PACKAGE, classLoader);
    registerPackageTypes(hints, ERRORS_PACKAGE, classLoader);
    registerReflection(hints, SessionSseService.StoredEvent.class);
    registerReflection(hints, TurnstileService.TurnstileVerifyResponse.class);
    registerMailHints(hints, classLoader);
  }

  private void registerMailHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    for (final var resource : MAIL_RESOURCES) {
      hints.resources().registerPattern(resource);
    }

    final var effectiveClassLoader =
        classLoader != null ? classLoader : QuezapApiRuntimeHints.class.getClassLoader();
    for (final var className : MAIL_REFLECTION_CLASSES) {
      try {
        final var clazz = Class.forName(className, false, effectiveClassLoader);
        registerReflection(hints, clazz);
      } catch (ClassNotFoundException ignored) {
        // Optional provider class not present on classpath
      }
    }
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
