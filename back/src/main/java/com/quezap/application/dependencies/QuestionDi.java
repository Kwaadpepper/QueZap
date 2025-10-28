package com.quezap.application.dependencies;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.domain.usecases.questions.ListQuestions;
import com.quezap.domain.usecases.questions.RemoveQuestion;

@Configuration
public class QuestionDi {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager questionPictureManager;

  public QuestionDi(
      QuestionRepository questionRepository,
      ThemeRepository themeRepository,
      QuestionPictureManager questionPictureManager) {
    this.questionRepository = questionRepository;
    this.themeRepository = themeRepository;
    this.questionPictureManager = questionPictureManager;
  }

  @Bean
  ListQuestions.Handler listQuestionsHandler() {
    return new ListQuestions.Handler(questionRepository);
  }

  @Bean
  AddQuestion.Handler addQuestionHandler() {
    return new AddQuestion.Handler(questionRepository, themeRepository, questionPictureManager);
  }

  @Bean
  RemoveQuestion.Handler removeQuestionHandler() {
    return new RemoveQuestion.Handler(questionRepository);
  }
}
