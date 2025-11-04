package com.quezap.api;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

import com.quezap.api.v1.validation.PaginationValidator;

import org.jspecify.annotations.Nullable;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.quezap")
@ImportRuntimeHints({
  com.quezap.application.aot.QuezapRuntimeHints.class,
  com.quezap.infrastructure.aot.QuezapRuntimeHints.class
})
public class ApiApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }

  static class ValidationRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.reflection().registerType(PaginationValidator.class);
    }
  }
}
