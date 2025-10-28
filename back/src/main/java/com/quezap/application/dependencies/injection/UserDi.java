package com.quezap.application.dependencies.injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.IdentifierHasher;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.domain.usecases.users.AddUser;
import com.quezap.domain.usecases.users.DeleteUser;
import com.quezap.domain.usecases.users.ListUsers;
import com.quezap.domain.usecases.users.UpdateUserPassword;

@Configuration
public class UserDi {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;

  public UserDi(
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      IdentifierHasher identifierHasher,
      PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
    this.identifierHasher = identifierHasher;
    this.passwordHasher = passwordHasher;
  }

  @Bean
  AddUser.Handler addUserHandler() {
    return new AddUser.Handler(
        userRepository, credentialRepository, identifierHasher, passwordHasher);
  }

  @Bean
  ListUsers.Handler listUsersHandler() {
    return new ListUsers.Handler(userRepository);
  }

  @Bean
  DeleteUser.Handler deleteUserHandler() {
    return new DeleteUser.Handler(userRepository, credentialRepository);
  }

  @Bean
  UpdateUserPassword.Handler updateUserPasswordHandler() {
    return new UpdateUserPassword.Handler(userRepository, credentialRepository, passwordHasher);
  }
}
