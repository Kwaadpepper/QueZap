package com.quezap.api.v1.dto.events;

import java.util.List;

public record LobbyStatePayload(
    String sessionName,
    int participantsCount,
    List<ParticipantDto> participants,
    int totalQuestions) {}
