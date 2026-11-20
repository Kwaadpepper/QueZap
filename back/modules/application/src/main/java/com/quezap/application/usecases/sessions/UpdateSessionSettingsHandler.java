package com.quezap.application.usecases.sessions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.UpdateSessionSettings.Input;
import com.quezap.application.ports.sessions.UpdateSessionSettings.Output;
import com.quezap.application.ports.sessions.UpdateSessionSettings.UpdateSessionSettingsUsecase;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class UpdateSessionSettingsHandler implements UpdateSessionSettingsUsecase {

  private final SessionStorePort sessionStore;

  public UpdateSessionSettingsHandler(SessionStorePort sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var session =
        sessionStore
            .findSessionById(input.sessionId())
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));

    session.assertHost(input.requesterId());
    session.updateSettings(
        input.progressionMode(),
        input.explanationDurationSeconds(),
        input.mobileCorrectionEnabled());
    sessionStore.saveAndFlushSession(session);

    return new Output(
        session.getProgressionMode().name(),
        session.getExplanationDurationSeconds(),
        session.isMobileCorrectionEnabled());
  }
}
