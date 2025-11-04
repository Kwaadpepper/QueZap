package com.quezap.application.usecases.sessions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.sessions.ParticipateSessionError;
import com.quezap.application.ports.sessions.ParticipateSession.Input;
import com.quezap.application.ports.sessions.ParticipateSession.Output;
import com.quezap.application.ports.sessions.ParticipateSession.ParticipateSessionUsecase;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.domain.ports.services.ParticipationTokenService;
import com.quezap.domain.ports.services.SessionCodeEncoder;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class ParticipateSessionHandler implements ParticipateSessionUsecase {
  private final SessionRepository sessionRepository;
  private final ParticipationTokenService participationTokenGenerator;
  private final UserNameSanitizer userNameSanitizer;
  private final SessionCodeEncoder sessionCodeEncoder;

  public ParticipateSessionHandler(
      SessionRepository sessionRepository,
      ParticipationTokenService sessionTokenGenerator,
      UserNameSanitizer userNameSanitizer,
      SessionCodeEncoder sessionCodeEncoder) {
    this.sessionRepository = sessionRepository;
    this.participationTokenGenerator = sessionTokenGenerator;
    this.userNameSanitizer = userNameSanitizer;
    this.sessionCodeEncoder = sessionCodeEncoder;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {

    final var sessionCode = usecaseInput.code();
    final var participantName = usecaseInput.name();
    final var sessionNumber = sessionCodeEncoder.decode(sessionCode);

    return sessionRepository
        .findByNumber(sessionNumber)
        .<Output>map(session -> addParticipantTo(session, participantName))
        .orElseThrow(ApplicationConstraintException.with(ParticipateSessionError.INVALID_CODE));
  }

  private Output addParticipantTo(Session session, ParticipantName participantName) {

    final var sessionId = session.getId();
    final var participationToken = participationTokenGenerator.generate(sessionId);
    final var sanitizedUserName = sanitize(participantName);
    final var participant = new Participant(sanitizedUserName, 0, participationToken);

    session.addParticipant(participant);
    sessionRepository.persist(session);

    return new Output.Participation(participationToken);
  }

  private ParticipantName sanitize(ParticipantName name) {
    try {
      return new ParticipantName(userNameSanitizer.sanitize(name.value()));
    } catch (IllegalDomainStateException _) {
      throw new ApplicationConstraintException(ParticipateSessionError.NAME_REFUSED);
    }
  }
}
