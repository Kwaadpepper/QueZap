package com.quezap.api.v1.aot;

import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import com.quezap.api.v1.dto.events.AnswerCountPayload;
import com.quezap.api.v1.dto.events.AnswerStatDto;
import com.quezap.api.v1.dto.events.FinalLeaderboardEntryDto;
import com.quezap.api.v1.dto.events.IntermediateLeaderboardPayload;
import com.quezap.api.v1.dto.events.LobbyStatePayload;
import com.quezap.api.v1.dto.events.ParticipantDto;
import com.quezap.api.v1.dto.events.ParticipantKickedPayload;
import com.quezap.api.v1.dto.events.PodiumDto;
import com.quezap.api.v1.dto.events.QuestionClosedPayload;
import com.quezap.api.v1.dto.events.QuestionRevealPayload;
import com.quezap.api.v1.dto.events.QuestionStartPayload;
import com.quezap.api.v1.dto.events.QuestionTimeWarningPayload;
import com.quezap.api.v1.dto.events.SafeAnswerDto;
import com.quezap.api.v1.dto.events.SafeQuestionDto;
import com.quezap.api.v1.dto.events.SessionEndedPayload;
import com.quezap.api.v1.dto.events.SessionEventEnvelope;
import com.quezap.api.v1.dto.events.SessionStartedPayload;
import com.quezap.api.v1.dto.response.sessions.JoinSessionResponseDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.QuestionAnswerStatDto;
import com.quezap.api.v1.dto.response.sessions.QuestionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionCreatedDto;
import com.quezap.api.v1.dto.response.sessions.SessionDetailDto;
import com.quezap.api.v1.dto.response.sessions.SessionParticipantReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.api.v1.services.SessionSseService;

import org.jspecify.annotations.Nullable;

public class QuezapApiRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(@Nullable RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (hints == null) {
      return;
    }

    final var classes =
        List.of(
            SessionEventEnvelope.class,
            LobbyStatePayload.class,
            SessionStartedPayload.class,
            QuestionStartPayload.class,
            QuestionTimeWarningPayload.class,
            QuestionClosedPayload.class,
            QuestionRevealPayload.class,
            IntermediateLeaderboardPayload.class,
            SessionEndedPayload.class,
            ParticipantKickedPayload.class,
            AnswerCountPayload.class,
            ParticipantDto.class,
            SafeQuestionDto.class,
            SafeAnswerDto.class,
            AnswerStatDto.class,
            FinalLeaderboardEntryDto.class,
            PodiumDto.class,
            SessionDetailDto.class,
            SessionCreatedDto.class,
            JoinSessionResponseDto.class,
            LeaderboardResponseDto.class,
            LeaderboardEntryDto.class,
            SessionReportDto.class,
            SessionParticipantReportDto.class,
            QuestionReportDto.class,
            QuestionAnswerStatDto.class,
            SessionSseService.StoredEvent.class);

    for (final var clazz : classes) {
      hints
          .reflection()
          .registerType(
              clazz,
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.INVOKE_DECLARED_METHODS,
              MemberCategory.DECLARED_FIELDS);
    }
  }
}
