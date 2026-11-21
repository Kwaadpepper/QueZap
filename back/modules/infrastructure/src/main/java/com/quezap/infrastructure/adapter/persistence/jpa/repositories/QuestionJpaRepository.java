package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

  long countByThemeIdIn(Collection<UUID> themeIds);

  boolean existsByPictureFilename(String pictureFilename);

  boolean existsByPictureFilenameIn(Collection<String> pictureFilenames);

  @Query(
      "SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END "
          + "FROM AnswerJpaEntity a WHERE a.pictureFilename IN :pictureFilenames")
  boolean existsAnswerByPictureFilenameIn(
      @Param("pictureFilenames") Collection<String> pictureFilenames);

  Page<QuestionJpaEntity> findByThemeIdIn(Collection<UUID> themeIds, Pageable pageable);

  Page<QuestionJpaEntity> findByLabelContainingIgnoreCase(String label, Pageable pageable);

  Page<QuestionJpaEntity> findByThemeIdInAndLabelContainingIgnoreCase(
      Collection<UUID> themeIds, String label, Pageable pageable);

  Page<QuestionJpaEntity> findByAuthorId(UUID authorId, Pageable pageable);

  Page<QuestionJpaEntity> findByAuthorIdAndThemeIdIn(
      UUID authorId, Collection<UUID> themeIds, Pageable pageable);

  Page<QuestionJpaEntity> findByAuthorIdAndLabelContainingIgnoreCase(
      UUID authorId, String label, Pageable pageable);

  Page<QuestionJpaEntity> findByAuthorIdAndThemeIdInAndLabelContainingIgnoreCase(
      UUID authorId, Collection<UUID> themeIds, String label, Pageable pageable);

  @Query(
      "SELECT COUNT(DISTINCT qq.quezap.id) FROM QuezapQuestionJpaEntity qq "
          + "WHERE qq.question.id = :questionId")
  long countQuezapsReferencingQuestion(@Param("questionId") UUID questionId);

  @Query(
      "SELECT COUNT(DISTINCT qq.quezap.id) FROM QuezapQuestionJpaEntity qq "
          + "WHERE qq.question.id = :questionId AND qq.quezap.id <> :quezapId")
  long countOtherQuezapsReferencingQuestion(
      @Param("questionId") UUID questionId, @Param("quezapId") UUID quezapId);

  @Query(
      "SELECT CASE WHEN COUNT(qq) > 0 THEN true ELSE false END "
          + "FROM QuezapQuestionJpaEntity qq "
          + "WHERE qq.question.id = :questionId AND qq.quezap.isPublic = true")
  boolean isQuestionInPublicQuezap(@Param("questionId") UUID questionId);
}
