package com.quezap.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.quezap")
@EntityScan(basePackages = "com.quezap.infrastructure.adapter.persistence.jpa.entities")
@EnableJpaRepositories(
    basePackages = "com.quezap.infrastructure.adapter.persistence.jpa.repositories")
@ImportRuntimeHints({
  com.quezap.application.aot.QuezapRuntimeHints.class,
  com.quezap.infrastructure.aot.QuezapRuntimeHints.class
})
@CommandScan(basePackages = "com.quezap")
public class CliApplication {
  public static void main(String[] args) {
    SpringApplication.run(CliApplication.class, args);
  }
}
