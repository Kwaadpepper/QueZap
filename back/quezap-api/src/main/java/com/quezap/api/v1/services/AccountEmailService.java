package com.quezap.api.v1.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountEmailService {
  private static final Logger logger = LoggerFactory.getLogger(AccountEmailService.class);

  private final @Nullable JavaMailSender mailSender;
  private final boolean mailEnabled;
  private final String fromAddress;
  private final String appName;
  private final String publicUrl;

  @Autowired
  public AccountEmailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.mail.enabled:false}") boolean mailEnabled,
      @Value("${app.mail.from:noreply@quezap.fr}") String fromAddress,
      @Value("${app.mail.app-name:QueZap}") String appName,
      @Value("${app.public-url:http://localhost:8000}") String publicUrl) {
    this(mailSenderProvider.getIfAvailable(), mailEnabled, fromAddress, appName, publicUrl);
  }

  public AccountEmailService(
      @Nullable JavaMailSender mailSender,
      boolean mailEnabled,
      String fromAddress,
      String appName,
      String publicUrl) {
    this.mailSender = mailSender;
    this.mailEnabled = mailEnabled;
    this.fromAddress = fromAddress;
    this.appName = appName;
    this.publicUrl = stripTrailingSlash(publicUrl);
  }

  public boolean isEnabled() {
    return mailEnabled;
  }

  public void sendActivationEmail(String recipientEmail, String userName, String activationToken) {
    final var encodedToken = URLEncoder.encode(activationToken, StandardCharsets.UTF_8);
    final var activationUrl = publicUrl + "/activation?token=" + encodedToken;

    if (!mailEnabled) {
      logger.info("[MAIL DISABLED] Activation link for {}: {}", userName, activationUrl);
      return;
    }

    final var subject = "[" + appName + "] Activez votre compte";
    final var body =
        "Bonjour "
            + userName
            + ",\n\n"
            + "Votre compte "
            + appName
            + " vient d'être créé. Cliquez sur le lien ci-dessous pour l'activer "
            + "(valide 24 heures) :\n\n"
            + activationUrl
            + "\n\n"
            + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.\n";

    try {
      sendPlainText(recipientEmail, subject, body);
    } catch (MailException e) {
      logger.error("Failed to send activation email to {}", recipientEmail, e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Failed to send activation email", e);
    }
  }

  public void sendPasswordResetEmail(String recipientEmail, String userName, String resetToken) {
    final var encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
    final var resetUrl = publicUrl + "/auth/reset-password?token=" + encodedToken;

    if (!mailEnabled) {
      logger.info("[MAIL DISABLED] Password reset link for {}: {}", userName, resetUrl);
      return;
    }

    final var subject = "[" + appName + "] Réinitialisation de votre mot de passe";
    final var body =
        "Bonjour "
            + userName
            + ",\n\n"
            + "Une demande de réinitialisation de mot de passe a été effectuée pour votre compte "
            + appName
            + ".\n"
            + "Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe "
            + "(valide 30 minutes, usage unique) :\n\n"
            + resetUrl
            + "\n\n"
            + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.\n";

    try {
      sendPlainText(recipientEmail, subject, body);
    } catch (MailException e) {
      logger.error("Failed to send password reset email to {}", recipientEmail, e);
    }
  }

  private void sendPlainText(String to, String subject, String body) {
    if (mailSender == null) {
      throw new IllegalStateException(
          "APP_MAIL_ENABLED is true but no JavaMailSender bean is configured");
    }
    final var message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    mailSender.send(message);
  }

  private static String stripTrailingSlash(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }
}
