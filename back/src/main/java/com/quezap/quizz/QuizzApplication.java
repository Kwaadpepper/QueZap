package com.quezap.quizz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
@ComponentScan(basePackages = "com.quezap")
public class QuizzApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(QuizzApplication.class, args);
  }
}
