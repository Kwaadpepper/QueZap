package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AnswerQuestion {
  record Input(SessionCode code, ParticipationToken token, Integer slideIndex, Integer answerIndex)
      implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record AnswerAdded() implements Output {}
  }

  public interface AnswerQuestionUsecase
      extends UsecaseHandler<AnswerQuestion.Input, AnswerQuestion.Output> {}
}
