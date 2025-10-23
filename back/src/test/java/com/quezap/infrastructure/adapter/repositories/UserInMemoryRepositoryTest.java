package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.entities.User;
import com.quezap.lib.pagination.Pagination;

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
  void canListUsers() {
    // GIVEN
    var pageRequest = Pagination.ofPage(1L, 10L);
    var userList = List.of(Mockito.mock(User.class), Mockito.mock(User.class));

    userList.forEach(
        user -> {
          Mockito.when(user.getId()).thenReturn(UUID.randomUUID());
          repository.save(user);
        });

    // WHEN
    var users = repository.findAll(pageRequest);

    // THEN
    Assertions.assertThat(users.items().size()).isEqualTo(userList.size());
  }

  @Test
  void canAddUser() {
    // GIVEN
    var user = Mockito.mock(User.class);

    // WHEN
    Mockito.when(user.getId()).thenReturn(UUID.randomUUID());

    repository.save(user);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canRetrieveUserById() {
    // GIVEN
    var user = Mockito.mock(User.class);
    var id = UUID.randomUUID();
    Mockito.when(user.getId()).thenReturn(id);
    repository.save(user);

    // WHEN
    var retrievedUser = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedUser).isNotNull();
  }

  @Test
  void cannotRetrieveNonExistentUser() {
    // GIVEN
    var nonExistentId = UUID.randomUUID();
    // WHEN
    var retrievedUser = repository.find(nonExistentId);

    // THEN
    Assertions.assertThat(retrievedUser).isNull();
  }

  @Test
  void canDeleteUser() {
    // GIVEN
    var user = Mockito.mock(User.class);
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    Mockito.when(user.getId()).thenReturn(id);
    repository.save(user);

    // WHEN
    repository.delete(user);
    var retrievedUser = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedUser).isNull();
  }

  @Test
  void deletingNonExistentUserDoesNotThrow() {
    // GIVEN
    var user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(UUID.randomUUID());

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.delete(user)).doesNotThrowAnyException();
  }

  @Test
  void canUpdateUser() {
    // GIVEN
    var user = Mockito.mock(User.class);
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    Mockito.when(user.getId()).thenReturn(id);
    repository.save(user);

    // WHEN
    repository.update(user);
    var retrievedUser = repository.find(id);

    // THEN
    Assertions.assertThat(retrievedUser).isNotNull();
  }

  @Test
  void updatingNonExistentUserDoesNotThrow() {
    // GIVEN
    var user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(UUID.randomUUID());

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.update(user)).doesNotThrowAnyException();
  }
}
