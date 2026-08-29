package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.directories.ThemeDirectory;
import com.quezap.domain.ports.directories.views.ThemeView;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.ThemeJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Repository
@Primary
public class ThemeRepositoryJpaAdapter implements ThemeRepository, ThemeDirectory {

  private final ThemeJpaRepository themeJpaRepository;
  private final ThemeJpaMapper themeJpaMapper;

  public ThemeRepositoryJpaAdapter(
      ThemeJpaRepository themeJpaRepository, ThemeJpaMapper themeJpaMapper) {
    this.themeJpaRepository = themeJpaRepository;
    this.themeJpaMapper = themeJpaMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Theme> find(ThemeId id) {
    if (id == null || id.value() == null) {
      return Optional.empty();
    }
    return themeJpaRepository.findById(id.value()).map(themeJpaMapper::toDomain);
  }

  @Override
  @Transactional
  public void persist(Theme entity) {
    final var jpaEntity = themeJpaMapper.toEntity(entity);
    themeJpaRepository.save(jpaEntity);
  }

  @Override
  @Transactional
  public void delete(Theme entity) {
    themeJpaRepository.deleteById(entity.getId().value());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Theme> findByName(ThemeName name) {
    return themeJpaRepository.findByName(name.value()).map(themeJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<ThemeView> paginate(Pagination pagination) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").ascending());

    final var page = themeJpaRepository.findAll(pageRequest);
    final var items = page.getContent().stream().map(themeJpaMapper::toView).toList();

    return PageOf.of(pagination, items, page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<ThemeView> paginateSearching(Pagination pagination, SearchQuery search) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").ascending());

    final var page = themeJpaRepository.findByNameContainingIgnoreCase(search.value(), pageRequest);
    final var items = page.getContent().stream().map(themeJpaMapper::toView).toList();

    return PageOf.of(pagination, items, page.getTotalElements());
  }
}
