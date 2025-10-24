package com.quezap.application.dependencies;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.usecases.questions.ListQuestions;

@Component
public class QuestionDi {
  private final QuestionRepository questionRepository;

  public QuestionDi(QuestionRepository questionRepository) {
    this.questionRepository = questionRepository;
  }

  @Bean
  public ListQuestions.Handler listQuestionsHandler() {
    return new ListQuestions.Handler(questionRepository);
  }
}
