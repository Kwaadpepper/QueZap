package com.quezap.api.v1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AccountEmailServiceTest {

  @Test
  void shouldNotSendEmailWhenMailDisabled() {
    final var mailSender = Mockito.mock(JavaMailSender.class);
    final var service =
        new AccountEmailService(
            mailSender, false, "noreply@quezap.fr", "QueZap", "https://quezap.example.com");

    assertThat(service.isEnabled()).isFalse();
    service.sendActivationEmail("alice@example.com", "Alice", "tok-123");
    service.sendPasswordResetEmail("alice@example.com", "Alice", "tok-456");

    verifyNoInteractions(mailSender);
  }

  @Test
  void shouldSendActivationAndPasswordResetEmailsWhenEnabled() {
    final var mailSender = Mockito.mock(JavaMailSender.class);
    final var service =
        new AccountEmailService(
            mailSender, true, "noreply@quezap.fr", "QueZap", "https://quezap.example.com/");

    service.sendActivationEmail("alice@example.com", "Alice", "act.tok");
    final var activationCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(activationCaptor.capture());

    final var activationMsg = activationCaptor.getValue();
    assertThat(activationMsg.getFrom()).isEqualTo("noreply@quezap.fr");
    assertThat(activationMsg.getTo()).containsExactly("alice@example.com");
    assertThat(activationMsg.getSubject()).contains("QueZap");
    assertThat(activationMsg.getText())
        .contains("https://quezap.example.com/activation?token=act.tok");

    Mockito.reset(mailSender);
    service.sendPasswordResetEmail("alice@example.com", "Alice", "rst.tok");
    final var resetCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(resetCaptor.capture());

    final var resetMsg = resetCaptor.getValue();
    assertThat(resetMsg.getText())
        .contains("https://quezap.example.com/auth/reset-password?token=rst.tok");
  }
}
