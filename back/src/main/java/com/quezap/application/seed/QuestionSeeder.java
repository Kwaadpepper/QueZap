package com.quezap.application.seed;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;
import com.quezap.lib.pagination.Pagination;

@Component
public class QuestionSeeder implements Seeder {
  private static final int NUMBER_OF_QUESTIONS_OF_ANY_TYPE = 10;

  private final UseCaseExecutor executor;
  private final AddQuestion.Handler handler;

  private final ThemeRepository themeRepository;

  public QuestionSeeder(
      UseCaseExecutor executor, AddQuestion.Handler handler, ThemeRepository themeRepository) {
    this.executor = executor;
    this.handler = handler;
    this.themeRepository = themeRepository;
  }

  @Override
  public void seed() {
    seedSimpleBooleanQuestions();
  }

  private void seedSimpleBooleanQuestions() {
    final var randomGen = RandomGenerator.getDefault();
    final var themes =
        themeRepository.paginate(Pagination.firstPage(50L)).items().stream()
            .map(Theme::getId)
            .toList();

    for (int i = 1; i <= NUMBER_OF_QUESTIONS_OF_ANY_TYPE; i++) {
      final var randomThemeId = themes.get(Random.from(randomGen).nextInt(themes.size()));
      final var question = "Question : can you answer the question " + i + "?";
      final var isTrue = randomGen.nextBoolean();
      final var input = new AddQuestion.Input.Affirmation(question, isTrue, null, randomThemeId);

      executor.execute(handler, input);
    }
  }
}
