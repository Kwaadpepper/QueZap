package com.quezap.application.usecases.sessions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quezap.application.ports.sessions.JoinSession.Input;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideOption;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JoinSessionHandlerTest {

  private SessionStorePort sessionStore;
  private JoinSessionHandler handler;

  @BeforeEach
  void setUp() {
    sessionStore = Mockito.mock(SessionStorePort.class);
    final var sanitizer = Mockito.mock(UserNameSanitizer.class);
    when(sanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
    final var publisher = Mockito.mock(SessionRealtimePublisher.class);
    handler = new JoinSessionHandler(sessionStore, sanitizer, publisher);
  }

  private static Session sessionWithParticipants(List<SessionParticipant> participants) {
    final var now = Instant.parse("2026-11-18T10:00:00Z");
    final var slide =
        new SessionSlideSpec(
            1,
            UuidV7.randomUuid(),
            "QUIZZ",
            "Q1",
            null,
            20,
            1000,
            List.of(new SessionSlideOption(0, "Yes", null, true)));
    return Session.hydrateLive(
        new SessionId(UuidV7.randomUuid()),
        "Cap Test",
        new SessionNumber(12345678L),
        0,
        new UserId(UuidV7.randomUuid()),
        UuidV7.randomUuid(),
        "Quiz",
        ProgressionMode.MANUAL,
        false,
        15,
        false,
        null,
        null,
        null,
        null,
        0,
        TimelinePoint.at(now),
        List.of(slide),
        participants,
        List.of());
  }

  @Test
  void shouldRejectNewParticipantWhenActiveCountReachesCap() {
    final var now = Instant.now();
    final var participants = new ArrayList<SessionParticipant>();
    for (var i = 0; i < JoinSessionHandler.MAX_ACTIVE_PARTICIPANTS; i++) {
      participants.add(
          new SessionParticipant(
              UUID.randomUUID(), "Student" + i, 0, "ptk_" + i, false, now, 0L, 0, 0L));
    }
    final var session = sessionWithParticipants(participants);
    when(sessionStore.findSessionByNumber(12345678L)).thenReturn(Optional.of(session));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new Input("12345678", "NewStudent", null),
                    Mockito.mock(UnitOfWorkEvents.class)))
        .isInstanceOf(SessionDomainException.class)
        .satisfies(
            ex ->
                assertThat(((SessionDomainException) ex).category())
                    .isEqualTo(SessionDomainException.ErrorCategory.CONFLICT));
  }

  @Test
  void shouldAllowReconnectionAndIgnoreKickedParticipantsAgainstCap() {
    final var now = Instant.now();
    final var participants = new ArrayList<SessionParticipant>();
    for (var i = 0; i < JoinSessionHandler.MAX_ACTIVE_PARTICIPANTS - 1; i++) {
      participants.add(
          new SessionParticipant(
              UUID.randomUUID(), "Student" + i, 0, "ptk_" + i, false, now, 0L, 0, 0L));
    }
    participants.add(
        new SessionParticipant(
            UUID.randomUUID(), "KickedStudent", 0, "ptk_kicked", true, now, 0L, 0, 0L));

    final var session = sessionWithParticipants(participants);
    when(sessionStore.findSessionByNumber(12345678L)).thenReturn(Optional.of(session));

    // 199 active + 1 kicked -> 200th active participant is allowed
    final var joined =
        handler.handle(
            new Input("12345678", "Student200", null), Mockito.mock(UnitOfWorkEvents.class));
    assertThat(joined.participantName()).isEqualTo("Student200");

    // Reconnection with existing token still succeeds even at 200 active participants
    final var reconnected =
        handler.handle(
            new Input("12345678", "Student0", "ptk_0"), Mockito.mock(UnitOfWorkEvents.class));
    assertThat(reconnected.participationToken()).isEqualTo("ptk_0");
  }
}
