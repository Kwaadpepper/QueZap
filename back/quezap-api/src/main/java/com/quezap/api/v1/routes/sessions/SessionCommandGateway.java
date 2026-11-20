package com.quezap.api.v1.routes.sessions;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.ports.sessions.CreateSession;
import com.quezap.application.ports.sessions.CreateSession.CreateSessionUsecase;
import com.quezap.application.ports.sessions.JoinSession;
import com.quezap.application.ports.sessions.JoinSession.JoinSessionUsecase;
import com.quezap.application.ports.sessions.KickSessionParticipant;
import com.quezap.application.ports.sessions.KickSessionParticipant.KickSessionParticipantUsecase;
import com.quezap.application.ports.sessions.LeaveSession;
import com.quezap.application.ports.sessions.LeaveSession.LeaveSessionUsecase;
import com.quezap.application.ports.sessions.NextSessionQuestion;
import com.quezap.application.ports.sessions.NextSessionQuestion.NextSessionQuestionUsecase;
import com.quezap.application.ports.sessions.RevealSessionQuestion;
import com.quezap.application.ports.sessions.RevealSessionQuestion.RevealSessionQuestionUsecase;
import com.quezap.application.ports.sessions.StartSessionLifecycle;
import com.quezap.application.ports.sessions.StartSessionLifecycle.StartSessionLifecycleUsecase;
import com.quezap.application.ports.sessions.SubmitSessionAnswer;
import com.quezap.application.ports.sessions.SubmitSessionAnswer.SubmitSessionAnswerUsecase;
import com.quezap.application.ports.sessions.ToggleSessionPause;
import com.quezap.application.ports.sessions.ToggleSessionPause.ToggleSessionPauseUsecase;
import com.quezap.application.ports.sessions.UpdateSessionSettings;
import com.quezap.application.ports.sessions.UpdateSessionSettings.UpdateSessionSettingsUsecase;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

@Component
public class SessionCommandGateway {

  private final UsecaseExecutor executor;
  private final CreateSessionUsecase createSession;
  private final JoinSessionUsecase joinSession;
  private final StartSessionLifecycleUsecase startSession;
  private final NextSessionQuestionUsecase nextQuestion;
  private final SubmitSessionAnswerUsecase submitAnswer;
  private final RevealSessionQuestionUsecase revealQuestion;
  private final ToggleSessionPauseUsecase togglePause;
  private final UpdateSessionSettingsUsecase updateSettings;
  private final KickSessionParticipantUsecase kickParticipant;
  private final LeaveSessionUsecase leaveSession;

  @SuppressWarnings("java:S107")
  public SessionCommandGateway(
      UsecaseExecutor executor,
      CreateSessionUsecase createSession,
      JoinSessionUsecase joinSession,
      StartSessionLifecycleUsecase startSession,
      NextSessionQuestionUsecase nextQuestion,
      SubmitSessionAnswerUsecase submitAnswer,
      RevealSessionQuestionUsecase revealQuestion,
      ToggleSessionPauseUsecase togglePause,
      UpdateSessionSettingsUsecase updateSettings,
      KickSessionParticipantUsecase kickParticipant,
      LeaveSessionUsecase leaveSession) {
    this.executor = executor;
    this.createSession = createSession;
    this.joinSession = joinSession;
    this.startSession = startSession;
    this.nextQuestion = nextQuestion;
    this.submitAnswer = submitAnswer;
    this.revealQuestion = revealQuestion;
    this.togglePause = togglePause;
    this.updateSettings = updateSettings;
    this.kickParticipant = kickParticipant;
    this.leaveSession = leaveSession;
  }

  public CreateSession.Output create(CreateSession.Input input) {
    return execute(createSession, input);
  }

  public JoinSession.Output join(JoinSession.Input input) {
    return execute(joinSession, input);
  }

  public StartSessionLifecycle.Output start(StartSessionLifecycle.Input input) {
    return execute(startSession, input);
  }

  public NextSessionQuestion.Output nextQuestion(NextSessionQuestion.Input input) {
    return execute(nextQuestion, input);
  }

  public SubmitSessionAnswer.Output submitAnswer(SubmitSessionAnswer.Input input) {
    return execute(submitAnswer, input);
  }

  public void reveal(RevealSessionQuestion.Input input) {
    execute(revealQuestion, input);
  }

  public ToggleSessionPause.Output togglePause(ToggleSessionPause.Input input) {
    return execute(togglePause, input);
  }

  public UpdateSessionSettings.Output updateSettings(UpdateSessionSettings.Input input) {
    return execute(updateSettings, input);
  }

  public void kickParticipant(KickSessionParticipant.Input input) {
    execute(kickParticipant, input);
  }

  public void leaveSession(LeaveSession.Input input) {
    execute(leaveSession, input);
  }

  public <T> T translateDomainError(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (SessionDomainException ex) {
      throw toResponseStatusException(ex);
    }
  }

  private <I extends UsecaseInput, O extends UsecaseOutput> O execute(
      UsecaseHandler<I, O> handler, I input) {
    return translateDomainError(() -> executor.execute(handler, input));
  }

  public static ResponseStatusException toResponseStatusException(SessionDomainException ex) {
    final var status =
        switch (ex.category()) {
          case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
          case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
          case FORBIDDEN -> HttpStatus.FORBIDDEN;
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT -> HttpStatus.CONFLICT;
        };
    return new ResponseStatusException(status, ex.getMessage(), ex);
  }
}
