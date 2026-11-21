package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public interface FindUserCredential {
  sealed interface Input extends UsecaseInput {
    record ById(UserId id) implements Input {}

    record ByName(String name) implements Input {}

    record ByIdentifier(RawIdentifier identifier) implements Input {}
  }

  record Output(
      UserId id,
      String name,
      boolean activated,
      String passwordFingerprint,
      TimelinePoint createdAt,
      TimelinePoint updatedAt)
      implements UsecaseOutput {}

  public interface FindUserCredentialUsecase
      extends UsecaseHandler<FindUserCredential.Input, FindUserCredential.Output> {}
}
