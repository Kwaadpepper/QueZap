package com.quezap.application.dependencies;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.domain.usecases.questions.ListQuestions;
import com.quezap.lib.ddd.usecases.TransactionRegistrar;

@Configuration
public class QuestionDi {
  private final TransactionRegistrar transactionRegistrar;
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager questionPictureManager;

  public QuestionDi(
      QuestionRepository questionRepository,
      ThemeRepository themeRepository,
      QuestionPictureManager questionPictureManager,
      TransactionRegistrar transactionRegistrar) {
    this.questionRepository = questionRepository;
    this.themeRepository = themeRepository;
    this.questionPictureManager = questionPictureManager;
    this.transactionRegistrar = transactionRegistrar;
  }

  @Bean
  ListQuestions.Handler listQuestionsHandler() {
    return new ListQuestions.Handler(questionRepository);
  }

  @Bean
  AddQuestion.Handler addQuestionHandler() {
    return new AddQuestion.Handler(
        questionRepository, themeRepository, questionPictureManager, transactionRegistrar);
  }
}
