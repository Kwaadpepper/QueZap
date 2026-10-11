package com.quezap.api.v1;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.junit.jupiter.api.Test;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
class GenerateOpenApiJsonTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void exportOpenApiJson() throws Exception {
    final var result =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    final var json = result.getResponse().getContentAsString();
    final var outputPath = Paths.get("../../front/openapi.json").toAbsolutePath().normalize();
    Files.createDirectories(outputPath.getParent());
    Files.writeString(outputPath, json);
    System.out.println("OpenAPI specification exported to: " + outputPath);
  }
}
