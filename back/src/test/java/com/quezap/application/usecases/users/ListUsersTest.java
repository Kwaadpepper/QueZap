package com.quezap.application.usecases.users;

import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.directories.UserDirectory;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListUsersTest {
  private final ListUsers.Handler handler;
  private final UserDirectory userDirectory;

  public ListUsersTest() {
    userDirectory = MockEntity.mock(UserDirectory.class);
    handler = new ListUsers.Handler(userDirectory);
  }

  @Test
  void canListUsers() {
    // GIVEN
    var pageRequest = Pagination.ofPage(1L, 1L);
    var input = new ListUsers.Input(pageRequest);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);
    var users =
        List.<UserView>of(
            new UserView(
                new UserId(UUID.fromString("017f5a81-7e6d-7e6e-0000-000000000000")),
                "some-name",
                TimelinePoint.now(),
                TimelinePoint.now()));

    // WHEN
    Mockito.when(userDirectory.paginate(pageRequest))
        .thenReturn(new PageOf<UserView>(pageRequest, users, 1L));
    handler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }
}
