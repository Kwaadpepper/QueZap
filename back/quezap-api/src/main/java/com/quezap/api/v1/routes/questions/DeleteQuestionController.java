package com.quezap.api.v1.routes.questions;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.ports.questions.RemoveQuestion;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import org.jspecify.annotations.Nullable;

@RestController
public class DeleteQuestionController {
  private final UsecaseExecutor executor;
  private final RemoveQuestionUsecase usecase;

  DeleteQuestionController(UsecaseExecutor executor, RemoveQuestionUsecase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @DeleteMapping("apiv1/questions/{id}")
  void delete(@PathVariable("id") QuestionId id, @Nullable Authentication authentication) {
    final var effectiveAuth =
        authentication != null
            ? authentication
            : SecurityContextHolder.getContext().getAuthentication();
    if (effectiveAuth == null || !effectiveAuth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
    }

    final var input = new RemoveQuestion.Input(id);

    executor.execute(usecase, input);
  }
}
