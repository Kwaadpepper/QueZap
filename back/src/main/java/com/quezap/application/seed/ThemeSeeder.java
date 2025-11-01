package com.quezap.application.seed;

import org.springframework.stereotype.Component;

import com.quezap.application.ports.themes.AddTheme;
import com.quezap.application.ports.themes.AddTheme.AddThemeUsecase;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

@Component
public class ThemeSeeder implements Seeder {
  private static final int NUMBER_OF_THEMES = 10;

  private final UsecaseExecutor executor;
  private final AddThemeUsecase usecase;

  public ThemeSeeder(UsecaseExecutor executor, AddThemeUsecase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @Override
  public void seed() {
    for (int i = 1; i <= NUMBER_OF_THEMES; i++) {
      final var themeName = new ThemeName("Theme " + i);
      final var input = new AddTheme.Input(themeName);

      executor.execute(usecase, input);
    }
  }
}
