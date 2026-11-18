package com.quezap.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.quezap")
@EntityScan(basePackages = "com.quezap.infrastructure.adapter.persistence.jpa.entities")
@EnableJpaRepositories(
    basePackages = "com.quezap.infrastructure.adapter.persistence.jpa.repositories")
@ImportRuntimeHints({
  com.quezap.application.aot.QuezapRuntimeHints.class,
  com.quezap.infrastructure.aot.QuezapRuntimeHints.class,
  com.quezap.api.v1.aot.QuezapApiRuntimeHints.class
})
public class ApiApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }
}
