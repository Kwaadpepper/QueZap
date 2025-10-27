package com.quezap.application.api.v1.routes.questions;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.usecases.questions.RemoveQuestion;

@RestController
public class DeleteQuestionController {
  private final RemoveQuestion.Handler handler;

  DeleteQuestionController(RemoveQuestion.Handler handler) {
    this.handler = handler;
  }

  @DeleteMapping("apiv1/questions/{id}")
  void delete(@PathVariable("id") QuestionId id) {
    final var input = new RemoveQuestion.Input(id);

    handler.handle(input);
  }
}
