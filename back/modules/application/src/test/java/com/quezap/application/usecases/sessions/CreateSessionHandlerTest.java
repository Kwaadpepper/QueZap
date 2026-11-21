package com.quezap.application.usecases.sessions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.quezap.application.ports.sessions.CreateSession.Input;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.SessionStorePort.SessionQuezapSnapshot;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideOption;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreateSessionHandlerTest {

  private SessionStorePort sessionStore;
  private CreateSessionHandler handler;

  @BeforeEach
  void setUp() {
    sessionStore = Mockito.mock(SessionStorePort.class);
    handler = new CreateSessionHandler(sessionStore);
  }

  private static SessionQuezapSnapshot sampleQuezap(
      java.util.UUID authorId, java.util.UUID quezapId) {
    final var slide =
        new SessionSlideSpec(
            1,
            UuidV7.randomUuid(),
            "QUIZZ",
            "Question 1",
            null,
            20,
            1000,
            List.of(
                new SessionSlideOption(0, "Good", null, true),
                new SessionSlideOption(1, "Bad", null, false)));
    return new SessionQuezapSnapshot(quezapId, "Quiz 1", authorId, true, List.of(slide));
  }

  @Test
  void shouldGenerateEightDigitPinInExpectedRange() {
    final var authorId = UuidV7.randomUuid();
    final var quezapId = UuidV7.randomUuid();
    when(sessionStore.findQuezapById(quezapId))
        .thenReturn(Optional.of(sampleQuezap(authorId, quezapId)));
    when(sessionStore.existsSessionByNumber(anyLong())).thenReturn(false);

    for (var i = 0; i < 25; i++) {
      final var out = handler.handle(new Input(authorId, "Session " + i, quezapId), noOpUow());
      assertThat(out.number()).isBetween(10_000_000L, 99_999_999L);
      assertThat(out.code()).hasSize(8);
    }
  }

  @Test
  void shouldFallbackToNineDigitPinAfterTwentyCollisions() {
    final var authorId = UuidV7.randomUuid();
    final var quezapId = UuidV7.randomUuid();
    when(sessionStore.findQuezapById(quezapId))
        .thenReturn(Optional.of(sampleQuezap(authorId, quezapId)));
    when(sessionStore.existsSessionByNumber(anyLong())).thenReturn(true);

    final var out = handler.handle(new Input(authorId, "Collision Session", quezapId), noOpUow());
    assertThat(out.number()).isBetween(100_000_000L, 999_999_999L);
    assertThat(out.code()).hasSize(9);
  }

  private static UnitOfWorkEvents noOpUow() {
    return Mockito.mock(UnitOfWorkEvents.class);
  }
}
