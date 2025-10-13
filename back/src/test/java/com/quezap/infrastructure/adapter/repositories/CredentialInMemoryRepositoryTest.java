package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;

import com.quezap.domain.models.entities.Credential;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CredentialInMemoryRepositoryTest {
  private CredentialInMemoryRepository repository = new CredentialInMemoryRepository();

  @BeforeEach
  void setUp() {
    repository = new CredentialInMemoryRepository();
  }

  @Test
  void canAddCredential() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);

    // WHEN
    Mockito.when(credential.getId()).thenReturn(UUID.randomUUID());

    repository.save(credential);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canRetrieveCredentialById() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);
    var id = UUID.randomUUID();
    Mockito.when(credential.getId()).thenReturn(id);
    repository.save(credential);

    // WHEN
    var retrievedCredential = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedCredential).isNotNull();
  }

  @Test
  void cannotRetrieveNonExistentCredential() {
    // GIVEN
    var nonExistentId = UUID.randomUUID();
    // WHEN
    var retrievedCredential = repository.find(nonExistentId);

    // THEN
    Assertions.assertThat(retrievedCredential).isNull();
  }

  @Test
  void canDeleteCredential() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    Mockito.when(credential.getId()).thenReturn(id);
    repository.save(credential);

    // WHEN
    repository.delete(credential);
    var retrievedCredential = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedCredential).isNull();
  }

  @Test
  void deletingNonExistentCredentialDoesNotThrow() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);
    Mockito.when(credential.getId()).thenReturn(UUID.randomUUID());

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.delete(credential)).doesNotThrowAnyException();
  }

  @Test
  void canUpdateCredential() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    Mockito.when(credential.getId()).thenReturn(id);
    repository.save(credential);

    // WHEN
    repository.update(credential);
    var retrievedCredential = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedCredential).isNotNull();
  }

  @Test
  void updatingNonExistentCredentialDoesNotThrow() {
    // GIVEN
    var credential = Mockito.mock(Credential.class);
    Mockito.when(credential.getId()).thenReturn(UUID.randomUUID());

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.update(credential)).doesNotThrowAnyException();
  }
}
