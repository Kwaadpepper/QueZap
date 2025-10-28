package com.quezap.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.command.annotation.CommandScan;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
@ComponentScan(basePackages = "com.quezap")
@CommandScan(basePackages = "com.quezap")
public class QuizzApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(QuizzApplication.class, args);
  }
}
