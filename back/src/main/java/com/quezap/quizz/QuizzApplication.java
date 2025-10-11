package com.quezap.quizz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Fichier principal de l'application Spring Boot. */
@SpringBootApplication
public class QuizzApplication {

  /** Point d'entrée de l'application. */
  public static void main(String[] args) {
    SpringApplication.run(QuizzApplication.class, args);
  }
}
