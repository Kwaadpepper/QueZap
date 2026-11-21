package com.quezap.application.usecases.users;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.FindUserError;
import com.quezap.application.ports.users.FindUserCredential.FindUserCredentialUsecase;
import com.quezap.application.ports.users.FindUserCredential.Input;
import com.quezap.application.ports.users.FindUserCredential.Output;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class FindUserCredentialHandler implements FindUserCredentialUsecase {
  private static final int FINGERPRINT_HEX_LENGTH = 16;

  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final IdentifierHasher identifierHasher;

  public FindUserCredentialHandler(
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      IdentifierHasher identifierHasher) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
    this.identifierHasher = identifierHasher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    return switch (input) {
      case Input.ById(var id) -> {
        final var user =
            userRepository
                .find(id)
                .orElseThrow(ApplicationConstraintException.with(FindUserError.NO_SUCH_USER));
        final var credential = loadCredential(user);
        yield toOutput(user, credential);
      }
      case Input.ByName(var name) -> {
        final var user =
            userRepository
                .findByName(name)
                .orElseThrow(ApplicationConstraintException.with(FindUserError.NO_SUCH_USER));
        final var credential = loadCredential(user);
        yield toOutput(user, credential);
      }
      case Input.ByIdentifier(var rawIdentifier) -> {
        final var hashedIdentifier = identifierHasher.hash(rawIdentifier);
        final var credential =
            credentialRepository
                .findByIdentifier(hashedIdentifier)
                .orElseThrow(ApplicationConstraintException.with(FindUserError.NO_SUCH_USER));
        final var user =
            userRepository
                .findByCredential(credential.getId())
                .orElseThrow(ApplicationConstraintException.with(FindUserError.NO_SUCH_USER));
        yield toOutput(user, credential);
      }
    };
  }

  private Credential loadCredential(User user) {
    return credentialRepository
        .find(user.getCredential())
        .orElseThrow(() -> new IllegalDomainStateException("A user is missing credential"));
  }

  private Output toOutput(User user, Credential credential) {
    final var fingerprint = sha256Prefix(credential.getHashedPassword().value());
    return new Output(
        user.getId(),
        user.getName(),
        credential.isActivated(),
        fingerprint,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private static String sha256Prefix(String value) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, FINGERPRINT_HEX_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
