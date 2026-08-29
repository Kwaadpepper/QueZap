package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.directories.QuestionDirectory;
import com.quezap.domain.ports.directories.views.QuestionView;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.QuestionJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Repository
@Primary
public class QuestionRepositoryJpaAdapter implements QuestionRepository, QuestionDirectory {

  private final QuestionJpaRepository questionJpaRepository;
  private final ThemeJpaRepository themeJpaRepository;
  private final QuestionJpaMapper questionJpaMapper;

  public QuestionRepositoryJpaAdapter(
      QuestionJpaRepository questionJpaRepository,
      ThemeJpaRepository themeJpaRepository,
      QuestionJpaMapper questionJpaMapper) {
    this.questionJpaRepository = questionJpaRepository;
    this.themeJpaRepository = themeJpaRepository;
    this.questionJpaMapper = questionJpaMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public long countWithThemes(Set<ThemeId> themeIds) {
    if (themeIds.isEmpty()) {
      return 0;
    }
    final var ids = themeIds.stream().map(ThemeId::value).toList();
    return questionJpaRepository.countByThemeIdIn(ids);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Question> find(QuestionId id) {
    if (id == null || id.value() == null) {
      return Optional.empty();
    }
    return questionJpaRepository.findById(id.value()).map(questionJpaMapper::toDomain);
  }

  @Override
  @Transactional
  public void persist(Question entity) {
    final var themeEntity =
        themeJpaRepository
            .findById(entity.getTheme().value())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Theme not found for question: " + entity.getTheme().value()));

    final var jpaEntity = questionJpaMapper.toEntity(entity, themeEntity);
    questionJpaRepository.save(jpaEntity);
  }

  @Override
  @Transactional
  public void delete(Question entity) {
    questionJpaRepository.deleteById(entity.getId().value());
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<QuestionView> paginate(Pagination pagination) {
    final var pageRequest = createPageRequest(pagination);
    final var page = questionJpaRepository.findAll(pageRequest);

    return toPageOf(page, pagination);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<QuestionView> paginateWithThemes(Pagination pagination, Set<ThemeId> themes) {
    if (themes.isEmpty()) {
      return PageOf.empty(pagination);
    }
    final var ids = themes.stream().map(ThemeId::value).toList();
    final var pageRequest = createPageRequest(pagination);
    final var page = questionJpaRepository.findByThemeIdIn(ids, pageRequest);

    return toPageOf(page, pagination);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<QuestionView> paginateSearching(Pagination pagination, SearchQuery search) {
    final var pageRequest = createPageRequest(pagination);
    final var page =
        questionJpaRepository.findByLabelContainingIgnoreCase(search.value(), pageRequest);

    return toPageOf(page, pagination);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<QuestionView> paginateSearchingWithThemes(
      Pagination pagination, SearchQuery search, Set<ThemeId> themes) {
    if (themes.isEmpty()) {
      return PageOf.empty(pagination);
    }
    final var ids = themes.stream().map(ThemeId::value).toList();
    final var pageRequest = createPageRequest(pagination);
    final var page =
        questionJpaRepository.findByThemeIdInAndLabelContainingIgnoreCase(
            ids, search.value(), pageRequest);

    return toPageOf(page, pagination);
  }

  private PageRequest createPageRequest(Pagination pagination) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    return PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").ascending());
  }

  private PageOf<QuestionView> toPageOf(Page<QuestionJpaEntity> page, Pagination pagination) {
    final var items = page.getContent().stream().map(questionJpaMapper::toView).toList();
    return PageOf.of(pagination, items, page.getTotalElements());
  }
}
