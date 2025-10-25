package com.quezap.application.seed;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.usecases.themes.AddTheme;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@Component
public class ThemeSeeder implements Seeder {
  private static final int NUMBER_OF_THEMES = 10;

  private final UseCaseExecutor executor;
  private final AddTheme.Handler handler;

  public ThemeSeeder(UseCaseExecutor executor, AddTheme.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @Override
  public void seed() {
    for (int i = 1; i <= NUMBER_OF_THEMES; i++) {
      final var themeName = new ThemeName("Theme " + i);
      final var input = new AddTheme.Input(themeName);

      executor.execute(handler, input);
    }
  }
}
