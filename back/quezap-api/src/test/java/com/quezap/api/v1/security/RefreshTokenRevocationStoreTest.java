package com.quezap.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.RevokedRefreshTokenJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.RevokedRefreshTokenJpaRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenRevocationStoreTest {

  @Test
  void shouldStoreAndLookupSha256HashInRepository() {
    final var repository = mock(RevokedRefreshTokenJpaRepository.class);
    final var store = new RefreshTokenRevocationStore(repository);
    final var rawToken = "header.payload.signature";
    final var expectedHash = RefreshTokenRevocationStore.hashToken(rawToken);
    final var expiresAt = Instant.now().getEpochSecond() + 3600;

    when(repository.existsById(expectedHash)).thenReturn(false);

    store.revoke(rawToken, expiresAt);

    final var entityCaptor = ArgumentCaptor.forClass(RevokedRefreshTokenJpaEntity.class);
    verify(repository).save(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getTokenHash()).isEqualTo(expectedHash);
    assertThat(entityCaptor.getValue().getTokenHash()).hasSize(64);
    assertThat(entityCaptor.getValue().getTokenHash()).isNotEqualTo(rawToken);
    assertThat(store.isRevoked(rawToken)).isTrue();
  }

  @Test
  void shouldLoadPersistedRevocationAfterCacheMiss() {
    final var repository = mock(RevokedRefreshTokenJpaRepository.class);
    final var storeAfterRestart = new RefreshTokenRevocationStore(repository);
    final var rawToken = "persisted-refresh-token";
    final var expectedHash = RefreshTokenRevocationStore.hashToken(rawToken);
    final var entity =
        new RevokedRefreshTokenJpaEntity(expectedHash, Instant.now().plusSeconds(3600));

    when(repository.findById(expectedHash)).thenReturn(Optional.of(entity));

    assertThat(storeAfterRestart.isRevoked(rawToken)).isTrue();
    assertThat(storeAfterRestart.isRevoked(rawToken)).isTrue();
    verify(repository).findById(expectedHash);
  }

  @Test
  void shouldCleanupExpiredTokensInRepository() {
    final var repository = mock(RevokedRefreshTokenJpaRepository.class);
    final var store = new RefreshTokenRevocationStore(repository);

    store.cleanupExpired();

    verify(repository).deleteByExpiresAtBefore(any(Instant.class));
  }
}
