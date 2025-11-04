package com.quezap.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication(scanBasePackages = "com.quezap")
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
