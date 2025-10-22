package com.quezap.domain.models.entities.builders;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.entities.builders.SessionBuilder.Builder.WithoutOptional;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.questions.QuestionAnswer;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;

import org.eclipse.jdt.annotation.Nullable;

public sealed interface SessionBuilder {
  public static non-sealed class Builder implements SessionBuilder {
    public static WithoutOptional with(SessionName name, SessionNumber number, UserId author) {
      return new BuilderImpl(name, number, author);
    }

    public interface WithoutOptional {
      WithoutOptional questionSlides(Set<QuestionSlide> questionSlides, Integer currentSlideIndex);

      WithoutOptional addQuestionSlide(QuestionSlide slide);

      WithoutOptional currentSlideIndex(Integer currentSlideIndex);

      WithoutOptional participants(Set<Participant> participants);

      WithoutOptional addParticipant(Participant participant);

      WithoutOptional answers(Set<QuestionAnswer> answers);

      WithoutOptional addAnswer(QuestionAnswer answer);

      WithoutOptional startedAt(ZonedDateTime startedAt);

      WithoutOptional endedAt(ZonedDateTime endedAt);

      Session build();
    }
  }

  class BuilderImpl implements WithoutOptional {
    private SessionName name;
    private SessionNumber number;
    private Integer currentSlideIndex = 0;
    private Set<QuestionSlide> questionSlides = new HashSet<>();
    private Set<Participant> participants = new HashSet<>();
    private Set<QuestionAnswer> questionAnswers = new HashSet<>();
    private UserId author;
    private @Nullable ZonedDateTime startedAt = null;
    private @Nullable ZonedDateTime endedAt = null;

    public BuilderImpl(SessionName name, SessionNumber number, UserId author) {
      this.name = name;
      this.number = number;
      this.author = author;
    }

    @Override
    public WithoutOptional startedAt(ZonedDateTime startedAt) {
      this.startedAt = startedAt;
      return this;
    }

    @Override
    public WithoutOptional endedAt(ZonedDateTime endedAt) {
      this.endedAt = endedAt;
      return this;
    }

    @Override
    public WithoutOptional questionSlides(
        Set<QuestionSlide> questionSlides, Integer currentSlideIndex) {
      this.questionSlides = new HashSet<>(questionSlides);
      this.currentSlideIndex = currentSlideIndex;
      return this;
    }

    @Override
    public WithoutOptional addQuestionSlide(QuestionSlide slide) {
      this.questionSlides.add(slide);
      return this;
    }

    @Override
    public WithoutOptional participants(Set<Participant> participants) {
      this.participants = new HashSet<>(participants);
      return this;
    }

    @Override
    public WithoutOptional addParticipant(Participant participant) {
      this.participants.add(participant);
      return this;
    }

    @Override
    public WithoutOptional answers(Set<QuestionAnswer> answers) {
      this.questionAnswers = new HashSet<>(answers);
      return this;
    }

    @Override
    public WithoutOptional addAnswer(QuestionAnswer answer) {
      this.questionAnswers.add(answer);
      return this;
    }

    @Override
    public WithoutOptional currentSlideIndex(Integer currentSlideIndex) {
      this.currentSlideIndex = currentSlideIndex;
      return this;
    }

    @Override
    public Session build() {
      return new Session(
          name,
          number,
          currentSlideIndex,
          questionSlides,
          participants,
          questionAnswers,
          author,
          startedAt,
          endedAt);
    }
  }
}
