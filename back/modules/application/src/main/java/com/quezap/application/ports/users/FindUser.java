package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public interface FindUser {
  record Input(UserId id) implements UsecaseInput {}

  record Output(UserId id, String name, TimelinePoint createdAt, TimelinePoint updatedAt)
      implements UsecaseOutput {}

  public interface FindUserUsecase extends UsecaseHandler<FindUser.Input, FindUser.Output> {}
}
