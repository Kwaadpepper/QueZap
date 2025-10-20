package com.quezap.domain.usecases.sessions;

public sealed interface AddParticipant {
  record Input(SessionName name, UserId user) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record SessionAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AddSession {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionCodeGenerator sessionCodeGenerator;

    public Handler(
        SessionRepository sessionRepository,
        UserRepository userRepository,
        SessionCodeGenerator sessionCodeGenerator) {
      this.sessionRepository = sessionRepository;
      this.userRepository = userRepository;
      this.sessionCodeGenerator = sessionCodeGenerator;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var sessionName = usecaseInput.name();
      final var userId = usecaseInput.user();
      final var sessionCode = sessionCodeGenerator.generateUniqueCode();
      final var sessionBuilder = SessionBuilder.Builder.with(sessionName, sessionCode, userId);
      final var session = sessionBuilder.build();

      if (userRepository.find(userId.value()) == null) {
        throw new DomainConstraintException(AddSessionError.NO_SUCH_USER);
      }

      if (sessionRepository.findByCode(sessionCode) != null) {
        throw new IllegalDomainStateException("Generated session code is not unique");
      }

      sessionRepository.save(session);

      return new Output.SessionAdded();
    }
  }
}
