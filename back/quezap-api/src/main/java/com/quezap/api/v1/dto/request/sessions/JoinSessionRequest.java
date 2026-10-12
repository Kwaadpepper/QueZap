package com.quezap.api.v1.dto.request.sessions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(
    @NotBlank(message = "Session code is required") @Pattern(regexp = "^\\s*[A-Za-z0-9\\-]{3,20}\\s*$", message = "Session code must be valid") String code,
    @NotBlank(message = "Participant name is required") @Size(min = 2, max = 50, message = "Participant name must be between 2 and 50 characters") @Pattern(regexp = "^[^<>]*$", message = "Participant name must not contain HTML tags") String participantName) {

  @JsonCreator
  public static JoinSessionRequest of(
      @JsonProperty("code") String code, @JsonProperty("participantName") String participantName) {
    return new JoinSessionRequest(
        code != null ? code.trim() : "", participantName != null ? participantName.trim() : "");
  }
}
