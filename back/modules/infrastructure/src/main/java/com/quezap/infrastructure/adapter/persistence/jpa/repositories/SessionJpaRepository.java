package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionAnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

  Page<SessionJpaEntity> findByAuthorId(UUID authorId, Pageable pageable);

  Optional<SessionJpaEntity> findByNumber(Long number);

  boolean existsByNumber(Long number);

  Optional<SessionJpaEntity> findFirstByOrderByNumberDesc();

  @Query("SELECT s FROM SessionJpaEntity s LEFT JOIN FETCH s.quezap WHERE s.id = :sessionId")
  Optional<SessionJpaEntity> findByIdWithQuezap(@Param("sessionId") UUID sessionId);

  @Query(
      "SELECT p FROM SessionParticipantJpaEntity p "
          + "WHERE p.session.id = :sessionId AND p.participationToken = :token")
  Optional<SessionParticipantJpaEntity> findParticipantBySessionIdAndToken(
      @Param("sessionId") UUID sessionId, @Param("token") String token);

  @Query(
      "SELECT a FROM SessionAnswerJpaEntity a "
          + "WHERE a.session.id = :sessionId "
          + "AND a.participantName = :participantName AND a.slideIndex = :slideIndex")
  Optional<SessionAnswerJpaEntity> findAnswerBySessionIdAndParticipantAndSlide(
      @Param("sessionId") UUID sessionId,
      @Param("participantName") String participantName,
      @Param("slideIndex") int slideIndex);

  @Query(
      "SELECT COUNT(p) FROM SessionParticipantJpaEntity p "
          + "WHERE p.session.id = :sessionId AND p.isKicked = false")
  long countActiveParticipants(@Param("sessionId") UUID sessionId);

  @Query(
      "SELECT COUNT(a) FROM SessionAnswerJpaEntity a "
          + "WHERE a.session.id = :sessionId AND a.slideIndex = :slideIndex "
          + "AND a.participantName IN ("
          + "SELECT p.name FROM SessionParticipantJpaEntity p "
          + "WHERE p.session.id = :sessionId AND p.isKicked = false)")
  long countActiveAnswersForSlide(
      @Param("sessionId") UUID sessionId, @Param("slideIndex") int slideIndex);

  @Query(
      "SELECT p.session.id, COUNT(p) FROM SessionParticipantJpaEntity p "
          + "WHERE p.session.id IN :sessionIds AND p.isKicked = false GROUP BY p.session.id")
  List<Object[]> countActiveParticipantsBySessionIds(
      @Param("sessionIds") Collection<UUID> sessionIds);

  @Query(
      "SELECT qq.quezap.id, COUNT(qq) FROM QuezapQuestionJpaEntity qq "
          + "WHERE qq.quezap.id IN :quezapIds GROUP BY qq.quezap.id")
  List<Object[]> countSlidesByQuezapIds(@Param("quezapIds") Collection<UUID> quezapIds);
}
