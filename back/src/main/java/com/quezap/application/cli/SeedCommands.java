package com.quezap.application.cli;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.quezap.application.seed.UserSeeder;

@ShellComponent
public class SeedCommands {
  private final UserSeeder userSeeder;

  public SeedCommands(UserSeeder userSeeder) {
    this.userSeeder = userSeeder;
  }

  @ShellMethod(key = "seed:users", value = "génère des utilisateurs de test")
  public String seedUsers() {
    userSeeder.seed();

    return "Done";
  }
}
