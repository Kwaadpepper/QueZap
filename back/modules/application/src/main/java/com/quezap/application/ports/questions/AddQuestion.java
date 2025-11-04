package com.quezap.application.ports.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface AddQuestion {

  sealed interface Input extends UsecaseInput {
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

  record Output(QuestionId id) implements UsecaseOutput {}

  public interface AddQuestionUsecase
      extends UsecaseHandler<AddQuestion.Input, AddQuestion.Output> {}
}
