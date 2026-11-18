package com.quezap.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;

import com.quezap.application.StartUpSeedRunner;
import com.quezap.application.seed.QuestionSeeder;
import com.quezap.application.seed.ThemeSeeder;
import com.quezap.application.seed.UserSeeder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SeedCommandsTest {

  @Test
  void seedUsersDelegatesToUserSeeder() {
    final var userSeeder = Mockito.mock(UserSeeder.class);
    final var seedCommands = new SeedCommands(userSeeder);

    final var result = seedCommands.seedUsers();

    assertThat(result).isEqualTo("Done");
    verify(userSeeder).seed();
  }

  @Test
  void seedComponentsAreDisabledInProdProfile() {
    assertThat(AnnotationUtils.findAnnotation(SeedCommands.class, Profile.class))
        .isNotNull()
        .satisfies(p -> assertThat(p.value()).containsExactly("!prod"));
    assertThat(AnnotationUtils.findAnnotation(UserSeeder.class, Profile.class))
        .isNotNull()
        .satisfies(p -> assertThat(p.value()).containsExactly("!prod"));
    assertThat(AnnotationUtils.findAnnotation(ThemeSeeder.class, Profile.class))
        .isNotNull()
        .satisfies(p -> assertThat(p.value()).containsExactly("!prod"));
    assertThat(AnnotationUtils.findAnnotation(QuestionSeeder.class, Profile.class))
        .isNotNull()
        .satisfies(p -> assertThat(p.value()).containsExactly("!prod"));
    assertThat(AnnotationUtils.findAnnotation(StartUpSeedRunner.class, Profile.class))
        .isNotNull()
        .satisfies(p -> assertThat(p.value()).containsExactly("!prod"));
  }
}
