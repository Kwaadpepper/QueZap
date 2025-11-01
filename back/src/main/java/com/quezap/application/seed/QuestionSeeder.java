package com.quezap.application.seed;

import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.quezap.domain.port.directories.ThemeDirectory;
import com.quezap.domain.port.directories.views.ThemeView;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;
import com.quezap.lib.pagination.Pagination;

@Component
public class QuestionSeeder implements Seeder {
  private static final int NUMBER_OF_QUESTIONS_OF_ANY_TYPE = 10;

  private final UseCaseExecutor executor;
  private final AddQuestion.Handler handler;

  private final ThemeDirectory themeDirectory;

  public QuestionSeeder(
      UseCaseExecutor executor, AddQuestion.Handler handler, ThemeDirectory themeDirectory) {
    this.executor = executor;
    this.handler = handler;
    this.themeDirectory = themeDirectory;
  }

  @Override
  public void seed() {
    seedAffirmationQuestions();
    seedBinaryQuestions();
    seedQuizzQuestions();
  }

  private void seedAffirmationQuestions() {
    final var randomGen = RandomGenerator.getDefault();
    final var themes =
        themeDirectory.paginate(Pagination.firstPage(50L)).items().stream()
            .map(ThemeView::id)
            .toList();

    for (int i = 1; i <= NUMBER_OF_QUESTIONS_OF_ANY_TYPE; i++) {
      final var randomThemeId = themes.get(Random.from(randomGen).nextInt(themes.size()));
      final var question = "Question : can you answer the question " + i + "?";
      final var isTrue = randomGen.nextBoolean();
      final var input = new AddQuestion.Input.Affirmation(question, isTrue, null, randomThemeId);

      executor.execute(handler, input);
    }
  }

  private void seedBinaryQuestions() {
    final var randomGen = RandomGenerator.getDefault();
    final var themes =
        themeDirectory.paginate(Pagination.firstPage(50L)).items().stream()
            .map(ThemeView::id)
            .toList();

    for (int i = 1; i <= NUMBER_OF_QUESTIONS_OF_ANY_TYPE; i++) {
      final var randomThemeId = themes.get(Random.from(randomGen).nextInt(themes.size()));
      final var question = "Question : can you answer the binary question " + i + "?";
      final var responseTruth = randomGen.nextBoolean();
      final var answers =
          Set.of(
              new AddQuestion.Input.AnswerData("Yes", null, responseTruth),
              new AddQuestion.Input.AnswerData("No", null, !responseTruth));

      final var input = new AddQuestion.Input.Binary(question, answers, null, randomThemeId);
      executor.execute(handler, input);
    }
  }

  private void seedQuizzQuestions() {
    final var randomGen = RandomGenerator.getDefault();
    final var themes =
        themeDirectory.paginate(Pagination.firstPage(50L)).items().stream()
            .map(ThemeView::id)
            .toList();
    for (int i = 1; i <= NUMBER_OF_QUESTIONS_OF_ANY_TYPE; i++) {
      final var randomThemeId = themes.get(Random.from(randomGen).nextInt(themes.size()));
      final var question = "Question : can you answer the quizz question " + i + "?";
      final var responseTruth = randomGen.nextInt(4);
      final var answers =
          Set.of(
              new AddQuestion.Input.AnswerData("Answer 1", null, responseTruth == 0),
              new AddQuestion.Input.AnswerData("Answer 2", null, responseTruth == 1),
              new AddQuestion.Input.AnswerData("Answer 3", null, responseTruth == 2),
              new AddQuestion.Input.AnswerData("Answer 4", null, responseTruth == 3));
      final var input = new AddQuestion.Input.Quizz(question, answers, null, randomThemeId);
      executor.execute(handler, input);
    }
  }
}
