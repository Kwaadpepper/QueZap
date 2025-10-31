package com.quezap.domain.usecases.users;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListUsersTest {
  private final ListUsers.Handler handler;
  private final UserRepository userRepository;

  public ListUsersTest() {
    userRepository = MockEntity.mock(UserRepository.class);

    handler = new ListUsers.Handler(userRepository);
  }

  @Test
  void canListUsers() {
    // GIVEN
    var pageRequest = Pagination.ofPage(1L, 1L);
    var input = new ListUsers.Input(pageRequest);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);
    var users =
        List.of(
            new User(
                "some-name",
                new CredentialId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000")),
                ZonedDateTime.now(ZoneId.of("UTC"))));

    // WHEN
    Mockito.when(userRepository.findAll(pageRequest))
        .thenReturn(new PageOf<User>(pageRequest, users, 1L));
    handler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }
}
