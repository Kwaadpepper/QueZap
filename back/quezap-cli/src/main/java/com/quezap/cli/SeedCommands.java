package com.quezap.cli;

import org.springframework.context.annotation.Profile;
import org.springframework.shell.command.annotation.Command;

import com.quezap.application.seed.UserSeeder;

@Profile("!prod")
@Command(command = "seed")
public class SeedCommands {
  private final UserSeeder userSeeder;

  public SeedCommands(UserSeeder userSeeder) {
    this.userSeeder = userSeeder;
  }

  @Command(command = "users", description = "génère des utilisateurs de test")
  public String seedUsers() {
    userSeeder.seed();

    return "Done";
  }
}
