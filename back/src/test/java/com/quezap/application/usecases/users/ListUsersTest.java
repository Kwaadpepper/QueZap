package com.quezap.application.usecases.users;

import java.util.List;
import java.util.UUID;

import com.quezap.application.ports.users.ListUsers.Input;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
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
  private final ListUsersUsecase usecase;
  private final UserDirectory userDirectory;

  public ListUsersTest() {
    userDirectory = MockEntity.mock(UserDirectory.class);
    usecase = new ListUsersHandler(userDirectory);
  }

  @Test
  void canListUsers() {
    // GIVEN
    var pageRequest = Pagination.ofPage(1L, 1L);
    var input = new Input(pageRequest);
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
    usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }
}
