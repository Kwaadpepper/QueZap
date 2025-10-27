package com.quezap.application.config.jackson;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

@Configuration
public class JacksonConfig {

  @Bean
  Module domainValueObjectModule() {
    return new DomainValueObjectModule();
  }
}
