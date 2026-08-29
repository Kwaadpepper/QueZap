package com.quezap.infrastructure.adapter.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.CredentialRepositoryJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.CredentialJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.CredentialJpaRepository;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CredentialRepositoryJpaAdapterTest {

  private CredentialJpaRepository credentialJpaRepository;
  private CredentialJpaMapper credentialJpaMapper;
  private CredentialRepositoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    credentialJpaRepository = Mockito.mock(CredentialJpaRepository.class);
    credentialJpaMapper = new CredentialJpaMapper();
    adapter = new CredentialRepositoryJpaAdapter(credentialJpaRepository, credentialJpaMapper);
  }

  @Test
  void shouldPersistCredential() {
    final var validHash = "$argon2id$v=19$m=16384,t=2,p=1$fakehashedpassword";
    final var credential =
        new Credential(new HashedPassword(validHash), new HashedIdentifier("hashedIdentifier123"));

    adapter.persist(credential);

    verify(credentialJpaRepository).save(any(CredentialJpaEntity.class));
  }

  @Test
  void shouldFindCredentialById() {
    final var id = UuidV7.randomUuid();
    final var validHash = "$argon2id$v=19$m=16384,t=2,p=1$fakehashedpassword";
    final var entity =
        new CredentialJpaEntity(id, "hashedId", validHash, null, Instant.now(), Instant.now());

    when(credentialJpaRepository.findById(id)).thenReturn(Optional.of(entity));

    final var result = adapter.find(new CredentialId(id));

    assertThat(result).isPresent();
    assertThat(result.get().getId().value()).isEqualTo(id);
    assertThat(result.get().getHashedIdentifier().value()).isEqualTo("hashedId");
  }

  @Test
  void shouldFindCredentialByIdentifier() {
    final var id = UuidV7.randomUuid();
    final var validHash = "$argon2id$v=19$m=16384,t=2,p=1$fakehashedpassword";
    final var entity =
        new CredentialJpaEntity(id, "hashedId", validHash, null, Instant.now(), Instant.now());

    when(credentialJpaRepository.findByHashedIdentifier("hashedId"))
        .thenReturn(Optional.of(entity));

    final var result = adapter.findByIdentifier(new HashedIdentifier("hashedId"));

    assertThat(result).isPresent();
    assertThat(result.get().getId().value()).isEqualTo(id);
  }

  @Test
  void shouldDeleteCredential() {
    final var validHash = "$argon2id$v=19$m=16384,t=2,p=1$fakehashedpassword";
    final var credential =
        new Credential(new HashedPassword(validHash), new HashedIdentifier("hashedIdentifier123"));

    adapter.delete(credential);

    verify(credentialJpaRepository).deleteById(credential.getId().value());
  }
}
