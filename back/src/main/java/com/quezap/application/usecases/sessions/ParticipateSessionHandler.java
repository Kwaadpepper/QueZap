package com.quezap.application.usecases.sessions;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.ParticipateSession.Input;
import com.quezap.application.ports.sessions.ParticipateSession.Output;
import com.quezap.application.ports.sessions.ParticipateSession.ParticipateSessionUseCase;
import com.quezap.domain.errors.sessions.ParticipateSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.domain.ports.services.ParticipationTokenGenerator;
import com.quezap.domain.ports.services.SessionCodeEncoder;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class ParticipateSessionHandler implements ParticipateSessionUseCase {
  private final SessionRepository sessionRepository;
  private final ParticipationTokenGenerator participationTokenGenerator;
  private final UserNameSanitizer userNameSanitizer;
  private final SessionCodeEncoder sessionCodeEncoder;

  public ParticipateSessionHandler(
      SessionRepository sessionRepository,
      ParticipationTokenGenerator sessionTokenGenerator,
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
        .orElseThrow(DomainConstraintException.with(ParticipateSessionError.INVALID_CODE));
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
      throw new DomainConstraintException(ParticipateSessionError.NAME_REFUSED);
    }
  }
}
