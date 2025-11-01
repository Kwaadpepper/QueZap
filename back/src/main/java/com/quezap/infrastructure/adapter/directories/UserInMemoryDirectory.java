package com.quezap.infrastructure.adapter.directories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.quezap.domain.port.directories.UserDirectory;
import com.quezap.domain.port.directories.views.UserView;
import com.quezap.infrastructure.adapter.repositories.UserInMemoryRepository;
import com.quezap.infrastructure.anotations.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Directory
public class UserInMemoryDirectory implements UserDirectory {
  private final UserInMemoryRepository userRepository;

  public UserInMemoryDirectory(UserInMemoryRepository userRepository) {
    this.userRepository = userRepository;
  }

  private List<UserView> getAllUsers() {
    return userRepository.<UserView>mapWith(
        user ->
            new UserView(user.getId(), user.getName(), user.getCreatedAt(), user.getUpdatedAt()));
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
