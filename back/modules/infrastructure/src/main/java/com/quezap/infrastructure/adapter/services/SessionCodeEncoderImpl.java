package com.quezap.infrastructure.adapter.services;

import org.springframework.stereotype.Service;

import com.quezap.application.config.SessionCodeConfig;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.ports.services.SessionCodeEncoder;

import io.github.kwaadpepper.serialintcaster.SerialCaster;
import io.github.kwaadpepper.serialintcaster.SerialCasterException;

@Service
public class SessionCodeEncoderImpl implements SessionCodeEncoder {
  private final int length;
  private final long seed;
  private final char[] dict;

  public SessionCodeEncoderImpl(SessionCodeConfig config) {
    this.length = config.getLength();
    this.seed = config.getSeed();
    this.dict = config.getDictionary();
  }

  @Override
  public SessionCode encode(SessionNumber number) {
    final var codeString = convertLongToCode(number.value());
    return new SessionCode(codeString);
  }

  @Override
  public SessionNumber decode(SessionCode code) {
    final var numberLong = convertCodeToLong(code.value());
    return new SessionNumber(numberLong);
  }

  private String convertLongToCode(long number) {
    try {
      return SerialCaster.Companion.encode(number, seed, length, dict);
    } catch (SerialCasterException e) {
      throw new CodeGeneratorException("Failed to encode session number", e);
    }
  }

  private long convertCodeToLong(String code) {
    try {
      return SerialCaster.Companion.decode(code, seed, dict);
    } catch (SerialCasterException e) {
      throw new CodeGeneratorException("Failed to decode session number", e);
    }
  }

  private static class CodeGeneratorException extends RuntimeException {
    CodeGeneratorException(String message, Exception cause) {
      super(message, cause);
    }
  }
}
