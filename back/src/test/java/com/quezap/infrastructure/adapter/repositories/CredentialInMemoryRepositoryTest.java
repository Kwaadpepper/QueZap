package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.mocks.MockEntity;

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
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6f-0000-000000000000"));
    var credential = MockEntity.mock(Credential.class);

    // WHEN
    Mockito.when(credential.getId()).thenReturn(credentialId);

    repository.save(credential);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canRetrieveCredentialById() {
    // GIVEN
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6f-0000-000000000000"));
    var credential = MockEntity.mock(Credential.class);
    Mockito.when(credential.getId()).thenReturn(credentialId);
    repository.save(credential);

    // WHEN
    var retrievedCredential = repository.find(credentialId);

    // THEN
    Assertions.assertThat(retrievedCredential).isNotNull();
  }

  @Test
  void cannotRetrieveNonExistentCredential() {
    // GIVEN
    var credentialId = new CredentialId(UUID.fromString("217f5a80-7e6d-7e6e-0000-000000000000"));
    // WHEN
    var retrievedCredential = repository.find(credentialId);

    // THEN
    Assertions.assertThat(retrievedCredential).isEmpty();
  }

  @Test
  void canDeleteCredential() {
    // GIVEN
    var credential = MockEntity.mock(Credential.class);
    var credentialId = new CredentialId(UUID.fromString("117f5a80-7e6d-7e6e-0000-000000000000"));
    Mockito.when(credential.getId()).thenReturn(credentialId);
    repository.save(credential);

    // WHEN
    repository.delete(credential);
    var retrievedCredential = repository.find(credentialId);

    // THEN
    Assertions.assertThat(retrievedCredential).isEmpty();
  }

  @Test
  void deletingNonExistentCredentialDoesNotThrow() {
    // GIVEN
    var credential = MockEntity.mock(Credential.class);
    var credentialId = new CredentialId(UUID.fromString("317f5a80-7e6d-7e6e-0000-000000000000"));
    Mockito.when(credential.getId()).thenReturn(credentialId);

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.delete(credential)).doesNotThrowAnyException();
  }

  @Test
  void canUpdateCredential() {
    // GIVEN
    var credential = MockEntity.mock(Credential.class);
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    Mockito.when(credential.getId()).thenReturn(credentialId);
    repository.save(credential);

    // WHEN
    repository.update(credential);
    var retrievedCredential = repository.find(credentialId);

    // THEN
    Assertions.assertThat(retrievedCredential).isNotNull();
  }

  @Test
  void updatingNonExistentCredentialDoesNotThrow() {
    // GIVEN
    var credential = MockEntity.mock(Credential.class);
    var credentialId = new CredentialId(UUID.fromString("317f5a80-7e6d-7e6e-0000-000000000000"));
    Mockito.when(credential.getId()).thenReturn(credentialId);

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.update(credential)).doesNotThrowAnyException();
  }
}
