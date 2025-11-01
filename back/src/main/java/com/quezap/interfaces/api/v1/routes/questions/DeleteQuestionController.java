package com.quezap.interfaces.api.v1.routes.questions;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.ports.questions.RemoveQuestion;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

@RestController
public class DeleteQuestionController {
  private final UsecaseExecutor executor;
  private final RemoveQuestionUsecase usecase;

  DeleteQuestionController(UsecaseExecutor executor, RemoveQuestionUsecase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @DeleteMapping("apiv1/questions/{id}")
  void delete(@PathVariable("id") QuestionId id) {
    final var input = new RemoveQuestion.Input(id);

    executor.execute(usecase, input);
  }
}
