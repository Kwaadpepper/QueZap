package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;

@Repository
public interface StagedPictureJpaRepository extends JpaRepository<StagedPictureJpaEntity, UUID> {

  List<StagedPictureJpaEntity> findByExpiresAtBefore(Instant now);

  long countByUploaderIdAndExpiresAtAfter(UUID uploaderId, Instant now);

  Optional<StagedPictureJpaEntity> findByObjectKey(String objectKey);

  Optional<StagedPictureJpaEntity> findByPictureUrl(String pictureUrl);

  List<StagedPictureJpaEntity> findByPictureUrlIn(Collection<String> pictureUrls);

  @Modifying
  @Query(
      "DELETE FROM StagedPictureJpaEntity sp "
          + "WHERE sp.pictureUrl IN :pictureUrls AND sp.uploaderId = :uploaderId")
  void deleteByPictureUrlInAndUploaderId(
      @Param("pictureUrls") Collection<String> pictureUrls, @Param("uploaderId") UUID uploaderId);

  @Modifying
  @Query("DELETE FROM StagedPictureJpaEntity sp WHERE sp.pictureUrl IN :pictureUrls")
  void deleteByPictureUrlIn(@Param("pictureUrls") Collection<String> pictureUrls);

  @Modifying
  @Query("DELETE FROM StagedPictureJpaEntity sp WHERE sp.objectKey = :objectKey")
  void deleteByObjectKey(@Param("objectKey") String objectKey);

  @Modifying
  @Query("DELETE FROM StagedPictureJpaEntity sp WHERE sp.pictureUrl = :pictureUrl")
  void deleteByPictureUrl(@Param("pictureUrl") String pictureUrl);
}
