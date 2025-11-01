package com.quezap.application;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.command.annotation.CommandScan;

import com.quezap.aot.QuezapRuntimeHints;
import com.quezap.interfaces.api.v1.validation.PaginationValidator;

import org.jspecify.annotations.Nullable;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.quezap")
@CommandScan(basePackages = "com.quezap")
@ImportRuntimeHints({QuezapRuntimeHints.class, QuizzApplication.ValidationRuntimeHints.class})
public class QuizzApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(QuizzApplication.class, args);
  }

  static class ValidationRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.reflection().registerType(PaginationValidator.class);
    }
  }
}
