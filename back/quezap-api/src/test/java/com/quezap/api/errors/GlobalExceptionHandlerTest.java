package com.quezap.api.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import com.quezap.api.v1.exceptions.BadPaginationException;
import com.quezap.api.v1.exceptions.InvalidFileException;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

  private GlobalExceptionHandler handler;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler(FIXED_CLOCK);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController()).setControllerAdvice(handler).build();
  }

  @Test
  void shouldReturnBadRequestWhenBadPaginationExceptionIsThrown() throws Exception {
    mockMvc
        .perform(get("/test/bad-pagination"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("Page size must be less than or equal to 50"));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidFileExceptionIsThrown() throws Exception {
    mockMvc
        .perform(get("/test/invalid-file"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("Extension mismatch"));
  }

  @Test
  void shouldReturnNoContentWithoutJsonSerializationOnAsyncRequestNotUsable() throws Exception {
    mockMvc
        .perform(get("/test/sse-async-not-usable").accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNullWhenAsyncRequestNotUsableOnCommittedResponse() {
    final var response = new MockHttpServletResponse();
    response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    response.setCommitted(true);

    final var result =
        handler.handleAsyncRequestNotUsableException(
            new AsyncRequestNotUsableException("Broken pipe"), response);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNoContentWhenIoExceptionBrokenPipeOnSseStream() throws Exception {
    mockMvc
        .perform(get("/test/sse-broken-pipe").accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnInternalServerErrorForNonDisconnectIoException() {
    final var request = new MockHttpServletRequest();
    final var response = new MockHttpServletResponse();

    final var result =
        handler.handleIoException(new IOException("Disk read failure"), request, response);

    assertThat(result).isNotNull();
    final var nonNullResult = Objects.requireNonNull(result);
    assertThat(nonNullResult.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(nonNullResult.getBody()).isInstanceOf(ErrorResonseDto.class);
  }

  @Test
  void shouldNotSerializeJsonOnUnexpectedExceptionForSseRequest() {
    final var request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
    final var response = new MockHttpServletResponse();
    response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

    final var result =
        handler.handleUnexpectedException(
            new IllegalStateException("Stream already closed"), request, response);

    assertThat(result).isNotNull();
    final var nonNullResult = Objects.requireNonNull(result);
    assertThat(nonNullResult.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(nonNullResult.getBody()).isNull();
  }

  @RestController
  @SuppressWarnings("DoNotCallSuggester")
  static class TestController {

    @GetMapping("/test/invalid-file")
    void invalidFile() {
      throw new InvalidFileException("Extension mismatch");
    }

    @GetMapping("/test/bad-pagination")
    void badPagination() {
      throw new BadPaginationException("Page size must be less than or equal to 50");
    }

    @GetMapping(value = "/test/sse-async-not-usable", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    void sseAsyncNotUsable(HttpServletResponse response) throws AsyncRequestNotUsableException {
      response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
      throw new AsyncRequestNotUsableException(
          "ClientAbortException: java.io.IOException: Broken pipe");
    }

    @GetMapping(value = "/test/sse-broken-pipe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    void sseBrokenPipe(HttpServletResponse response) throws IOException {
      response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
      throw new IOException("Broken pipe");
    }
  }
}
