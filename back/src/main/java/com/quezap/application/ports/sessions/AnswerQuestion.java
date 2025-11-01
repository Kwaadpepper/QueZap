package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface AnswerQuestion {
  record Input(SessionCode code, ParticipationToken token, Integer slideIndex, Integer answerIndex)
      implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record AnswerAdded() implements Output {}
  }

  public interface AnswerQuestionUseCase
      extends UseCaseHandler<AnswerQuestion.Input, AnswerQuestion.Output> {}
}
