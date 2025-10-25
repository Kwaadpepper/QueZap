package com.quezap.application.seed;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.IdentifierHasher;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.domain.usecases.users.AddUser;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@Component
public class UserSeeder implements Seeder {
  private static final int NUMBER_OF_USERS = 10;

  private final UseCaseExecutor executor;
  private final AddUser.Handler handler;

  public UserSeeder(
      UseCaseExecutor executor,
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      IdentifierHasher identifierHasher,
      PasswordHasher passwordHashery) {
    this.executor = executor;
    handler =
        new AddUser.Handler(
            userRepository, credentialRepository, identifierHasher, passwordHashery);
  }

  @Override
  public void seed() {
    for (int i = 1; i <= NUMBER_OF_USERS; i++) {
      final var username = "user" + i;
      final var identifier = new RawIdentifier("user" + i);
      final var password = new RawPassword("123Password." + i);
      final var input = new AddUser.Input(username, identifier, password);

      executor.execute(handler, input);
    }
  }
}
