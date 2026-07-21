package com.gestudio.crm.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=test-owner",
      "security.bootstrap.password=test-password"
    })
@AutoConfigureMockMvc
@Testcontainers
class SecurityAuthorizationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void businessApiRejectsAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/v1/prospects")).andExpect(status().isUnauthorized());
  }

  @Test
  void explicitlyConfiguredBootstrapOwnerCanReadTheApi() throws Exception {
    mockMvc
        .perform(get("/api/v1/prospects").with(httpBasic("test-owner", "test-password")))
        .andExpect(status().isOk());
  }
}
