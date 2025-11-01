package com.quezap.application.ports.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.jspecify.annotations.Nullable;

public interface AddQuestion {

  sealed interface Input extends UseCaseInput {
    record Affirmation(
        String question, boolean isTrue, @Nullable PictureUploadData picture, ThemeId theme)
        implements Input {}

    record Binary(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme)
        implements Input {}

    record Quizz(
        String question,
        Set<AnswerData> answers,
        @Nullable PictureUploadData picture,
        ThemeId theme)
        implements Input {}

    public record AnswerData(
        String answerText, @Nullable PictureUploadData picture, boolean isCorrect) {}
  }

  record Output(QuestionId id) implements UseCaseOutput {}

  public interface AddQuestionUseCase
      extends UseCaseHandler<AddQuestion.Input, AddQuestion.Output> {}
}
