package com.quezap.interfaces.api.v1.routes.questions;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.usecases.questions.RemoveQuestion;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@RestController
public class DeleteQuestionController {
  private final UseCaseExecutor executor;
  private final RemoveQuestion.Handler handler;

  DeleteQuestionController(UseCaseExecutor executor, RemoveQuestion.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @DeleteMapping("apiv1/questions/{id}")
  void delete(@PathVariable("id") QuestionId id) {
    final var input = new RemoveQuestion.Input(id);

    executor.execute(handler, input);
  }
}
