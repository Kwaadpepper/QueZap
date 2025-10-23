package com.quezap.application.seed;

import java.util.Arrays;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StartUpSeedRunner {
  private static Logger logger = LoggerFactory.getLogger(StartUpSeedRunner.class);
  private UserSeeder userSeeder;

  public StartUpSeedRunner(UserSeeder userSeeder) {
    this.userSeeder = userSeeder;
  }

  @EventListener
  public void onStateChange(ApplicationStartedEvent event) {
    final var appCtx = event.getApplicationContext();
    final var appEnv = appCtx.getEnvironment();

    if (Arrays.asList(appEnv.getActiveProfiles()).contains("local")) {
      runSeeders();
    }
  }

  private void runSeeders() {
    logger.info("Running seeders.");

    userSeeder.seed();

    logger.info("Seeders Done.");
  }
}
