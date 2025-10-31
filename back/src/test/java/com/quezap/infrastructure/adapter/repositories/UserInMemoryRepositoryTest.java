package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

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
    var userMocked = MockEntity.mock(User.class, Mockito.RETURNS_DEEP_STUBS);
    var userList = List.of(userMocked, userMocked);

    userList.forEach(
        user -> {
          Mockito.when(user.getId()).thenReturn(MockEntity.mock(UserId.class));
          repository.persist(user);
        });

    // WHEN
    var users = repository.findAll(pageRequest);

    // THEN
    Assertions.assertThat(users.items()).hasSameSizeAs(userList);
  }

  @Test
  void canAddUser() {
    // GIVEN
    var user = MockEntity.mock(User.class);

    // WHEN
    Mockito.when(user.getId()).thenReturn(MockEntity.mock(UserId.class));

    repository.persist(user);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canRetrieveUserById() {
    // GIVEN
    var user = MockEntity.mock(User.class);
    var id = MockEntity.mock(UserId.class);
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
    var nonExistentId = MockEntity.mock(UserId.class);
    // WHEN
    var retrievedUser = repository.find(nonExistentId);

    // THEN
    Assertions.assertThat(retrievedUser).isEmpty();
  }

  @Test
  void canDeleteUser() {
    // GIVEN
    var user = MockEntity.mock(User.class);
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
    var user = MockEntity.mock(User.class);
    Mockito.when(user.getId()).thenReturn(MockEntity.mock(UserId.class));

    // WHEN & THEN
    Assertions.assertThatCode(() -> repository.delete(user)).doesNotThrowAnyException();
  }
}
