package com.quezap.infrastructure.adapter.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.UserRepositoryJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.UserJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.CredentialJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserRepositoryJpaAdapterTest {

  private UserJpaRepository userJpaRepository;
  private CredentialJpaRepository credentialJpaRepository;
  private UserJpaMapper userJpaMapper;
  private UserRepositoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    credentialJpaRepository = Mockito.mock(CredentialJpaRepository.class);
    userJpaMapper = new UserJpaMapper();
    adapter =
        new UserRepositoryJpaAdapter(userJpaRepository, credentialJpaRepository, userJpaMapper);
  }

  @Test
  void shouldPersistUser() {
    final var credentialId = UuidV7.randomUuid();
    final var credEntity =
        new CredentialJpaEntity(credentialId, "hid", "hpass", null, Instant.now(), Instant.now());
    when(credentialJpaRepository.findById(credentialId)).thenReturn(Optional.of(credEntity));

    final var user = new User("Alice", new CredentialId(credentialId));

    adapter.persist(user);

    verify(userJpaRepository).save(any(UserJpaEntity.class));
  }

  @Test
  void shouldFindUserById() {
    final var userId = UuidV7.randomUuid();
    final var credEntity =
        new CredentialJpaEntity(
            UuidV7.randomUuid(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var userEntity =
        new UserJpaEntity(userId, "Alice", credEntity, Instant.now(), Instant.now());

    when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

    final var result = adapter.find(new UserId(userId));

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Alice");
  }

  @Test
  void shouldFindUserByName() {
    final var userId = UuidV7.randomUuid();
    final var credEntity =
        new CredentialJpaEntity(
            UuidV7.randomUuid(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var userEntity =
        new UserJpaEntity(userId, "Alice", credEntity, Instant.now(), Instant.now());

    when(userJpaRepository.findByName("Alice")).thenReturn(Optional.of(userEntity));

    final var result = adapter.findByName("Alice");

    assertThat(result).isPresent();
    assertThat(result.get().getId().value()).isEqualTo(userId);
  }

  @Test
  void shouldPaginateUsers() {
    final var credEntity =
        new CredentialJpaEntity(
            UuidV7.randomUuid(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var userEntity =
        new UserJpaEntity(UuidV7.randomUuid(), "Alice", credEntity, Instant.now(), Instant.now());
    final var page = new PageImpl<>(List.of(userEntity));

    when(userJpaRepository.findAll(any(Pageable.class))).thenReturn(page);

    final var result = adapter.paginate(Pagination.firstPage(10));

    assertThat(result.items()).hasSize(1);
    assertThat(result.totalItems()).isEqualTo(1);
  }
}
