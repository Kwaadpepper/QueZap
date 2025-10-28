package com.quezap.application;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

@SpringBootTest
@ActiveProfiles("test")
class QuizzApplicationTests {

  @Test
  void contextLoads() {
    // Test de démarrage du contexte Spring - pas d'implémentation nécessaire
  }
}
