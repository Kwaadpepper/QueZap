package com.quezap.infrastructure.adapter.directories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.ports.directories.UserDirectory;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.infrastructure.adapter.spi.DataSource;
import com.quezap.infrastructure.annotations.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Directory
public class UserInMemoryDirectory implements UserDirectory {
  private final DataSource<User> userDataSource;

  public UserInMemoryDirectory(
      @Qualifier("userInMemoryRepository") DataSource<User> userDataSource) {
    this.userDataSource = userDataSource;
  }

  private List<UserView> getAllUsers() {
    return userDataSource.getAll().stream()
        .<UserView>map(
            user ->
                new UserView(
                    user.getId(), user.getName(), user.getCreatedAt(), user.getUpdatedAt()))
        .toList();
  }

  @Override
  public PageOf<UserView> paginate(Pagination pagination) {
    final var users = new ArrayList<>(getAllUsers());
    final var fromIndex = ((pagination.pageNumber() - 1) * pagination.pageSize());
    final var totalItems = users.size();

    if (fromIndex >= totalItems) {
      return PageOf.empty(pagination);
    }

    users.sort(createdAtComparator());

    final var toIndex = Math.min(fromIndex + pagination.pageSize(), totalItems);
    final var pageItems = users.subList((int) fromIndex, (int) toIndex);

    return PageOf.of(pagination, pageItems, (long) totalItems);
  }

  private Comparator<UserView> createdAtComparator() {
    return Comparator.comparing(UserView::createdAt);
  }
}
