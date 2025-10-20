package com.quezap.domain.models.entities.builders;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.entities.builders.SessionBuilder.Builder.WithoutOptional;
import com.quezap.domain.models.valueobjects.QuestionSlide;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;

import org.eclipse.jdt.annotation.Nullable;

public sealed interface SessionBuilder {
  public static non-sealed class Builder implements SessionBuilder {
    public static WithoutOptional with(SessionName name, SessionCode code, UserId author) {
      return new BuilderImpl(name, code, author);
    }

    public interface WithoutOptional {
      WithoutOptional questionSlides(Set<QuestionSlide> questionSlides);

      WithoutOptional addQuestionSlide(QuestionSlide slide);

      WithoutOptional participants(Set<Participant> participants);

      WithoutOptional addParticipant(Participant participant);

      WithoutOptional startedAt(ZonedDateTime startedAt);

      WithoutOptional endedAt(ZonedDateTime endedAt);

      Session build();
    }
  }

  class BuilderImpl implements WithoutOptional {
    private SessionName name;
    private SessionCode code;
    private Set<QuestionSlide> questionSlides = new HashSet<>();
    private Set<Participant> participants = new HashSet<>();
    private UserId author;
    private @Nullable ZonedDateTime startedAt = null;
    private @Nullable ZonedDateTime endedAt = null;

    public BuilderImpl(SessionName name, SessionCode code, UserId author) {
      this.name = name;
      this.code = code;
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
    public WithoutOptional questionSlides(Set<QuestionSlide> questionSlides) {
      this.questionSlides = questionSlides;
      return this;
    }

    @Override
    public WithoutOptional addQuestionSlide(QuestionSlide slide) {
      this.questionSlides.add(slide);
      return this;
    }

    @Override
    public WithoutOptional participants(Set<Participant> participants) {
      this.participants = participants;
      return this;
    }

    @Override
    public WithoutOptional addParticipant(Participant participant) {
      this.participants.add(participant);
      return this;
    }

    @Override
    public Session build() {
      return new Session(name, code, questionSlides, participants, author, startedAt, endedAt);
    }
  }
}
