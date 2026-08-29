package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.directories.UserDirectory;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.UserJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.CredentialJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Repository
@Primary
public class UserRepositoryJpaAdapter implements UserRepository, UserDirectory {

  private final UserJpaRepository userJpaRepository;
  private final CredentialJpaRepository credentialJpaRepository;
  private final UserJpaMapper userJpaMapper;

  public UserRepositoryJpaAdapter(
      UserJpaRepository userJpaRepository,
      CredentialJpaRepository credentialJpaRepository,
      UserJpaMapper userJpaMapper) {
    this.userJpaRepository = userJpaRepository;
    this.credentialJpaRepository = credentialJpaRepository;
    this.userJpaMapper = userJpaMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> find(UserId id) {
    if (id == null || id.value() == null) {
      return Optional.empty();
    }
    return userJpaRepository.findById(id.value()).map(userJpaMapper::toDomain);
  }

  @Override
  @Transactional
  public void persist(User entity) {
    final var credentialEntity =
        credentialJpaRepository
            .findById(entity.getCredential().value())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Credential not found for user: " + entity.getCredential().value()));

    final var jpaEntity = userJpaMapper.toEntity(entity, credentialEntity);
    userJpaRepository.save(jpaEntity);
  }

  @Override
  @Transactional
  public void delete(User entity) {
    userJpaRepository.deleteById(entity.getId().value());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByName(String name) {
    return userJpaRepository.findByName(name).map(userJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<UserView> paginate(Pagination pagination) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").ascending());

    final var page = userJpaRepository.findAll(pageRequest);
    final var items = page.getContent().stream().map(userJpaMapper::toView).toList();

    return PageOf.of(pagination, items, page.getTotalElements());
  }
}
