package com.quezap.api.v1.routes.questions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.ports.questions.RemoveQuestion;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionForbiddenException;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionInUseException;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import org.jspecify.annotations.Nullable;

@RestController
public class DeleteQuestionController {
  private final UsecaseExecutor executor;
  private final RemoveQuestionUsecase usecase;
  private final @Nullable UserRepository userRepository;

  DeleteQuestionController(UsecaseExecutor executor, RemoveQuestionUsecase usecase) {
    this(executor, usecase, null);
  }

  @Autowired
  DeleteQuestionController(
      UsecaseExecutor executor,
      RemoveQuestionUsecase usecase,
      @Nullable UserRepository userRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.userRepository = userRepository;
  }

  @DeleteMapping("apiv1/questions/{id}")
  void delete(@PathVariable("id") QuestionId id, @Nullable Authentication authentication) {
    final var effectiveAuth = QuestionAuthHelper.requireAuthentication(authentication);
    final var requesterId = QuestionAuthHelper.requireUserId(effectiveAuth, userRepository);
    final var isAdmin = QuestionAuthHelper.isAdmin(effectiveAuth);

    final var input = new RemoveQuestion.Input(id, requesterId, isAdmin);

    try {
      executor.execute(usecase, input);
    } catch (QuestionForbiddenException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (QuestionInUseException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
    }
  }
}
