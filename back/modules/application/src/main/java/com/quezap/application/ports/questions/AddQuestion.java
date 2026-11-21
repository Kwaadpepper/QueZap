package com.quezap.application.ports.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface AddQuestion {

  sealed interface Input extends UsecaseInput {
    record Affirmation(
        String question,
        boolean isTrue,
        @Nullable PictureUploadData picture,
        ThemeId theme,
        @Nullable UserId authorId)
        implements Input {
      public Affirmation(
          String question, boolean isTrue, @Nullable PictureUploadData picture, ThemeId theme) {
        this(question, isTrue, picture, theme, null);
      }
    }

    record Binary(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme,
        @Nullable UserId authorId)
        implements Input {
      public Binary(
          String question,
          Set<AnswerData> answers,
          @Nullable PictureUploadData picture,
          ThemeId theme) {
        this(question, answers, picture, theme, null);
      }
    }

    record Quizz(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme,
        @Nullable UserId authorId)
        implements Input {
      public Quizz(
          String question,
          Set<AnswerData> answers,
          @Nullable PictureUploadData picture,
          ThemeId theme) {
        this(question, answers, picture, theme, null);
      }
    }

    public record AnswerData(
        String answerText, @Nullable PictureUploadData picture, boolean isCorrect) {}
  }

  record Output(QuestionId id) implements UsecaseOutput {}

  public interface AddQuestionUsecase
      extends UsecaseHandler<AddQuestion.Input, AddQuestion.Output> {}
}
