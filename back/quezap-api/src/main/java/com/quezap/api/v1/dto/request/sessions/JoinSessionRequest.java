package com.quezap.api.v1.dto.request.sessions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(
    @NotBlank(message = C_REQ) @Pattern(regexp = C_RE, message = C_MSG) String code,
    // HTML tags are rejected by Pattern below
    @NotBlank(message = N_REQ) @Size(min = 2, max = 50, message = N_LEN)
        // disallow < and >
        @Pattern(regexp = N_RE, message = N_HTML) String participantName) {

  private static final String C_REQ = "Session code is required";
  private static final String C_RE = "^\\s*[A-Za-z0-9\\-]{3,20}\\s*$";
  private static final String C_MSG = "Session code must be valid";
  private static final String N_REQ = "Participant name is required";
  private static final String N_LEN = "Participant name must be between 2 and 50 characters";
  private static final String N_RE = "^[^<>]*$";
  private static final String N_HTML = "Participant name must not contain HTML tags";

  @JsonCreator
  public static JoinSessionRequest of(
      @JsonProperty("code") String code, @JsonProperty("participantName") String participantName) {
    return new JoinSessionRequest(
        code != null ? code.trim() : "", participantName != null ? participantName.trim() : "");
  }
}
