package com.quezap.interfaces.api.v1.routes.questions;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.domain.usecases.questions.AddQuestion;
import com.quezap.interfaces.api.v1.dto.internal.AnswerWithStream;
import com.quezap.interfaces.api.v1.dto.request.questions.AddQuestionDto.AffirmationDto;
import com.quezap.interfaces.api.v1.dto.request.questions.AddQuestionDto.AnswerDto;
import com.quezap.interfaces.api.v1.dto.request.questions.AddQuestionDto.BinaryDto;
import com.quezap.interfaces.api.v1.dto.request.questions.AddQuestionDto.QuizzDto;
import com.quezap.interfaces.api.v1.dto.response.questions.QuestionIdDto;
import com.quezap.interfaces.api.v1.exceptions.ServerException;
import com.quezap.interfaces.api.v1.helpers.PictureStreamHelper;
import com.quezap.interfaces.api.v1.mappers.QuestionMapper;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import jakarta.validation.Valid;

@RestController
public class AddQuestionController {
  private static final String IO_ERROR_MESSAGE = "Failed to process uploaded file";

  private final UseCaseExecutor executor;
  private final AddQuestion.Handler handler;
  private final QuestionMapper mapper;

  AddQuestionController(
      UseCaseExecutor executor, AddQuestion.Handler handler, QuestionMapper mapper) {
    this.executor = executor;
    this.handler = handler;
    this.mapper = mapper;
  }

  @PostMapping("apiv1/questions/affirmation")
  public QuestionIdDto addAffirmation(@Valid AffirmationDto request) {

    final var picture = request.picture();
    final var themeId = request.themeId();
    final var isTrue = request.isTrue();
    final var question = request.question();

    try (final var pictureStream = PictureStreamHelper.openStream(picture)) {

      final var pictureData = mapper.toPictureUploadData(pictureStream, picture);

      final var input = new AddQuestion.Input.Affirmation(question, isTrue, pictureData, themeId);
      final var output = executor.execute(handler, input);

      return mapper.toDto(output);

    } catch (IOException e) {
      throw new ServerException(IO_ERROR_MESSAGE, e);
    }
  }

  @PostMapping("apiv1/questions/binary")
  public QuestionIdDto addBinary(@Valid BinaryDto request) {

    final var picture = request.picture();
    final var themeId = request.themeId();
    final var question = request.question();
    final var answers = request.answers();

    try (final var questionStream = PictureStreamHelper.openStream(picture)) {

      final var questionPictureData = mapper.toPictureUploadData(questionStream, picture);

      return executeWithAnswerStreams(
          answers,
          answerDataList -> {
            final var input =
                new AddQuestion.Input.Binary(
                    question, answerDataList, questionPictureData, themeId);
            final var output = executor.execute(handler, input);

            return mapper.toDto(output);
          });

    } catch (IOException e) {
      throw new ServerException(IO_ERROR_MESSAGE, e);
    }
  }

  @PostMapping("apiv1/questions/quizz")
  public QuestionIdDto addQuizz(@Valid QuizzDto request) {

    final var picture = request.picture();
    final var themeId = request.themeId();
    final var question = request.question();
    final var answers = request.answers();

    try (final var questionStream = PictureStreamHelper.openStream(picture)) {

      final var questionPictureData = mapper.toPictureUploadData(questionStream, picture);

      return executeWithAnswerStreams(
          answers,
          answerDataList -> {
            final var input =
                new AddQuestion.Input.Quizz(question, answerDataList, questionPictureData, themeId);
            final var output = executor.execute(handler, input);

            return mapper.toDto(output);
          });

    } catch (IOException e) {
      throw new ServerException(IO_ERROR_MESSAGE, e);
    }
  }

  private <T> T executeWithAnswerStreams(
      List<AnswerDto> answers, Function<Set<AddQuestion.Input.AnswerData>, T> operation) {

    final var answersWithStreams = new HashSet<AnswerWithStream>();

    try {
      for (final var answer : answers) {
        final var answerPicture = answer.picture();
        final var stream = PictureStreamHelper.openStream(answerPicture);

        answersWithStreams.add(new AnswerWithStream(answer, stream));
      }

      return operation.apply(mapper.toAnswerDataSet(answersWithStreams));
    } catch (IOException e) {
      throw new ServerException(IO_ERROR_MESSAGE, e);
    } finally {
      answersWithStreams.stream()
          .forEach(answer -> PictureStreamHelper.closeStream(answer.stream()));
    }
  }
}
