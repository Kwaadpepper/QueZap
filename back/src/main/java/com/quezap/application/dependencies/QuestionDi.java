package com.quezap.application.dependencies;

import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.domain.models.valueobjects.Picture;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.domain.usecases.questions.ListQuestions;

@Configuration
public class QuestionDi {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;

  public QuestionDi(QuestionRepository questionRepository, ThemeRepository themeRepository) {
    this.questionRepository = questionRepository;
    this.themeRepository = themeRepository;
  }

  @Bean
  ListQuestions.Handler listQuestionsHandler() {
    return new ListQuestions.Handler(questionRepository);
  }

  @Bean
  AddQuestion.Handler addQuestionHandler() {
    return new AddQuestion.Handler(questionRepository, themeRepository, questionPictureManager());
  }

  @Bean
  QuestionPictureManager questionPictureManager() {
    return new QuestionPictureManager() {

      @Override
      public Picture store(Stream<Byte> pictureData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'store'");
      }

      @Override
      public boolean exists(Picture picture) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
      }

      @Override
      public Picture copy(Picture picture) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
      }

      @Override
      public void remove(Picture picture) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
      }
    };
  }
}
