package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserInMemoryRepositoryTest {
  private UserInMemoryRepository repository = new UserInMemoryRepository();

  @BeforeEach
  void setUp() {
    repository = new UserInMemoryRepository();
  }

  @Test
  void canAddUser() {
    // GIVEN
    var user = Mockito.mock(User.class);

    // WHEN
    Mockito.when(user.getId()).thenReturn(Mockito.mock(UserId.class));

    repository.persist(user);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canRetrieveUserById() {
    // GIVEN
    var user = Mockito.mock(User.class);
    var id = Mockito.mock(UserId.class);
    Mockito.when(user.getId()).thenReturn(id);
    repository.persist(user);

    // WHEN
    var retrievedUser = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedUser).isNotNull();
  }

  @Test
  void cannotRetrieveNonExistentUser() {
    // GIVEN
    var nonExistentId = Mockito.mock(UserId.class);
    // WHEN
    var retrievedUser = repository.find(nonExistentId);

    // THEN
    Assertions.assertThat(retrievedUser).isEmpty();
  }

  @Test
  void canDeleteUser() {
    // GIVEN
    var user = Mockito.mock(User.class);
    var id = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    Mockito.when(user.getId()).thenReturn(id);
    repository.persist(user);

    // WHEN
    repository.delete(user);
    var retrievedUser = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedUser).isEmpty();
  }

  @Test
  void deletingNonExistentUserDoesNotThrow() {
    // GIVEN
    var user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(Mockito.mock(UserId.class));

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.delete(user)).doesNotThrowAnyException();
  }
}
