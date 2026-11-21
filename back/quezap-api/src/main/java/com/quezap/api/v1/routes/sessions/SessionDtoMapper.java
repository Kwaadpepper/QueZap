package com.quezap.api.v1.routes.sessions;

import java.util.List;

import com.quezap.api.v1.dto.events.ParticipantDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.application.ports.sessions.SessionStorePort.SessionSummary;
import com.quezap.application.usecases.sessions.SessionQueryService.SessionDetailView;

final class SessionDtoMapper {

  private SessionDtoMapper() {}

  static SessionDetailDto toDetailDto(SessionSummary s) {
    return new SessionDetailDto(
        s.id(),
        s.number(),
        String.valueOf(s.number()),
        s.name(),
        s.status(),
        s.currentSlideIndex(),
        s.totalSlides(),
        s.activeParticipantCount(),
        List.of(),
        s.authorId(),
        s.startedAt(),
        s.endedAt(),
        s.progressionMode(),
        s.isPaused(),
        s.explanationDurationSeconds(),
        s.mobileCorrectionEnabled());
  }

  static SessionDetailDto toDetailDto(SessionDetailView view) {
    final var participants =
        view.participants().stream().map(p -> new ParticipantDto(p.name(), p.joinedAt())).toList();
    return new SessionDetailDto(
        view.id(),
        view.number(),
        view.code(),
        view.name(),
        view.status(),
        view.currentSlideIndex(),
        view.totalSlides(),
        view.participantsCount(),
        participants,
        view.authorId(),
        view.startedAt(),
        view.endedAt(),
        view.progressionMode(),
        view.isPaused(),
        view.explanationDurationSeconds(),
        view.mobileCorrectionEnabled());
  }

  static SessionDetailDto toPublicDetailDto(SessionDetailView view) {
    return new SessionDetailDto(
        view.id(),
        view.number(),
        view.code(),
        view.name(),
        view.status(),
        view.currentSlideIndex(),
        view.totalSlides(),
        view.participantsCount(),
        List.of(),
        view.authorId(),
        view.startedAt(),
        view.endedAt(),
        view.progressionMode(),
        view.isPaused(),
        view.explanationDurationSeconds(),
        view.mobileCorrectionEnabled());
  }
}
