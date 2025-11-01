package com.quezap.application.dependencies.injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.application.usecases.themes.AddTheme;
import com.quezap.application.usecases.themes.ListThemes;
import com.quezap.application.usecases.themes.RemoveTheme;
import com.quezap.application.usecases.themes.RenameTheme;
import com.quezap.domain.ports.directories.ThemeDirectory;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;

@Configuration
public class ThemeDi {
  private final ThemeDirectory themeDirectory;
  private final ThemeRepository themeRepository;
  private final QuestionRepository questionRepository;

  public ThemeDi(
      ThemeDirectory themeDirectory,
      ThemeRepository themeRepository,
      QuestionRepository questionRepository) {
    this.themeDirectory = themeDirectory;
    this.themeRepository = themeRepository;
    this.questionRepository = questionRepository;
  }

  @Bean
  ListThemes.Handler listThemesHandler() {
    return new ListThemes.Handler(themeDirectory);
  }

  @Bean
  AddTheme.Handler addThemeHandler() {
    return new AddTheme.Handler(themeRepository);
  }

  @Bean
  RenameTheme.Handler renameThemeHandler() {
    return new RenameTheme.Handler(themeRepository);
  }

  @Bean
  RemoveTheme.Handler removeThemeHandler() {
    return new RemoveTheme.Handler(themeRepository, questionRepository);
  }
}
