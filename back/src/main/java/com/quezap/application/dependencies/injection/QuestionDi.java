package com.quezap.application.dependencies.injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.application.usecases.questions.AddQuestion;
import com.quezap.application.usecases.questions.ListQuestions;
import com.quezap.application.usecases.questions.RemoveQuestion;
import com.quezap.domain.ports.directories.QuestionDirectory;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;

@Configuration
public class QuestionDi {
  private final QuestionDirectory questionDirectory;
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager questionPictureManager;

  public QuestionDi(
      QuestionDirectory questionDirectory,
      QuestionRepository questionRepository,
      ThemeRepository themeRepository,
      QuestionPictureManager questionPictureManager) {
    this.questionDirectory = questionDirectory;
    this.questionRepository = questionRepository;
    this.themeRepository = themeRepository;
    this.questionPictureManager = questionPictureManager;
  }

  @Bean
  ListQuestions.Handler listQuestionsHandler() {
    return new ListQuestions.Handler(questionDirectory);
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
