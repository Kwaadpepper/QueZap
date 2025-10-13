package com.quezap.quizz;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.quezap.application.seed.UserSeeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Profile("local")
public class ApplicationSeeder {
  private final Logger logger = LoggerFactory.getLogger(ApplicationSeeder.class);

  private final UserSeeder userSeeder;

  public ApplicationSeeder(UserSeeder userSeeder) {
    this.userSeeder = userSeeder;
  }

  @EventListener
  public void seedDatabaseOnApplicationReady(ApplicationReadyEvent event) {
    logger.info("Starting database seeding...");

    userSeeder.seed();

    logger.info("Database seeding completed.");
  }
}
