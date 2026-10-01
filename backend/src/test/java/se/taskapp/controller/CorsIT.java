package se.taskapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = "ALLOWED_ORIGINS=https://frontend.example.test, https://second.example.test")
@AutoConfigureMockMvc
class CorsIT {

  private static final String ALLOWED_ORIGIN = "https://frontend.example.test";
  private static final String UNKNOWN_ORIGIN = "https://evil.example.test";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("a preflight request from the frontend origin is allowed")
  void preflightFromAllowedOriginIsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/tasks")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
  }

  @Test
  @DisplayName("every origin in the comma-separated list is allowed")
  void secondOriginInListIsAllowed() throws Exception {
    mockMvc
        .perform(get("/api/tasks").header(HttpHeaders.ORIGIN, "https://second.example.test"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://second.example.test"));
  }

  @Test
  @DisplayName("a preflight request from an unknown origin is refused")
  void preflightFromUnknownOriginIsRefused() throws Exception {
    mockMvc
        .perform(
            options("/api/tasks")
                .header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("a request from the frontend origin gets the CORS header")
  void requestFromAllowedOriginGetsHeader() throws Exception {
    mockMvc
        .perform(get("/api/tasks").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
  }

  @Test
  @DisplayName("a request from an unknown origin is refused")
  void requestFromUnknownOriginIsRefused() throws Exception {
    mockMvc
        .perform(get("/api/tasks").header(HttpHeaders.ORIGIN, UNKNOWN_ORIGIN))
        .andExpect(status().isForbidden());
  }
}
