package com.quezap.application;

import java.util.Arrays;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.quezap.application.seed.QuestionSeeder;
import com.quezap.application.seed.ThemeSeeder;
import com.quezap.application.seed.UserSeeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StartUpSeedRunner {
  private static Logger logger = LoggerFactory.getLogger(StartUpSeedRunner.class);
  private static final String SEED_ON_PROFILE = "seed";

  private UserSeeder userSeeder;
  private ThemeSeeder themeSeeder;
  private QuestionSeeder questionSeeder;

  public StartUpSeedRunner(
      UserSeeder userSeeder, ThemeSeeder themeSeeder, QuestionSeeder questionSeeder) {
    this.userSeeder = userSeeder;
    this.themeSeeder = themeSeeder;
    this.questionSeeder = questionSeeder;
  }

  @EventListener
  public void onStateChange(ApplicationStartedEvent event) {
    final var appCtx = event.getApplicationContext();
    final var appEnv = appCtx.getEnvironment();

    if (Arrays.asList(appEnv.getActiveProfiles()).contains(SEED_ON_PROFILE)) {
      runSeeders();
    }
  }

  private void runSeeders() {
    logger.info("Running seeders.");

    userSeeder.seed();
    themeSeeder.seed();
    questionSeeder.seed();

    logger.info("Seeders Done.");
  }
}
