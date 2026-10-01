package se.taskapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import se.taskapp.model.Task;
import se.taskapp.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerEdgeCaseIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private TaskRepository taskRepository;

  @BeforeEach
  void clearDatabase() {
    taskRepository.deleteAll();
  }

  private ResultActions postJson(String body) throws Exception {
    return mockMvc.perform(
        post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private Task onlyStoredTask() {
    assertThat(taskRepository.count()).isEqualTo(1);
    return taskRepository.findAll().getFirst();
  }

  @Test
  @DisplayName("malformed JSON is rejected with 400 and nothing is stored")
  void malformedJsonIsRejected() throws Exception {
    postJson("{ \"title\": \"Buy milk\", ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", is("request body is missing or malformed")));

    assertThat(taskRepository.count()).isZero();
  }

  @Test
  @DisplayName("an empty body is rejected with 400")
  void emptyBodyIsRejected() throws Exception {
    postJson("")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("request body is missing or malformed")));
  }

  @Test
  @DisplayName("a JSON null body is rejected with 400")
  void nullBodyIsRejected() throws Exception {
    postJson("null").andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("a JSON array instead of an object is rejected with 400")
  void arrayBodyIsRejected() throws Exception {
    postJson("[]").andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("a non-boolean completed value is rejected with 400")
  void nonBooleanCompletedIsRejected() throws Exception {
    postJson("{ \"title\": \"Buy milk\", \"completed\": \"yes\" }")
        .andExpect(status().isBadRequest());

    assertThat(taskRepository.count()).isZero();
  }

  @Test
  @DisplayName("a missing title is rejected with a message naming the field")
  void missingTitleIsRejected() throws Exception {
    postJson("{ \"description\": \"No title here\", \"completed\": false }")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("title: title is required")));
  }

  @Test
  @DisplayName("a whitespace-only title is rejected with 400")
  void whitespaceOnlyTitleIsRejected() throws Exception {
    postJson("{ \"title\": \"   \", \"completed\": false }")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("title")));
  }

  @Test
  @DisplayName("a title longer than 255 characters is rejected with 400")
  void tooLongTitleIsRejected() throws Exception {
    postJson("{ \"title\": \"" + "a".repeat(256) + "\", \"completed\": false }")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("title: title must be at most 255 characters")));
  }

  @Test
  @DisplayName("plain text instead of JSON is rejected with 415")
  void plainTextIsRejected() throws Exception {
    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.TEXT_PLAIN).content("Buy milk"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status", is(415)));
  }

  @Test
  @DisplayName("a non-numeric id is rejected with 400 instead of a server error")
  void nonNumericIdIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/tasks/{id}", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("id has an invalid value")));
  }

  @Test
  @DisplayName("an id too large for a number is rejected with 400")
  void overflowingIdIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/tasks/{id}", "99999999999999999999"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("a negative id is treated as not found")
  void negativeIdIsNotFound() throws Exception {
    mockMvc.perform(get("/api/tasks/{id}", -1)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("surrounding whitespace is removed and a blank description is stored as none")
  void inputIsNormalisedBeforeSaving() throws Exception {
    postJson("{ \"title\": \"   Buy milk   \", \"description\": \"   \", \"completed\": false }")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("Buy milk")))
        .andExpect(jsonPath("$.description", nullValue()));

    Task stored = onlyStoredTask();
    assertThat(stored.getTitle()).isEqualTo("Buy milk");
    assertThat(stored.getDescription()).isNull();
  }

  @Test
  @DisplayName("HTML in a title is stored as plain text, never interpreted")
  void htmlIsStoredVerbatim() throws Exception {
    postJson("{ \"title\": \"<script>alert(1)</script>\", \"completed\": false }")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("<script>alert(1)</script>")));

    assertThat(onlyStoredTask().getTitle()).isEqualTo("<script>alert(1)</script>");
  }

  @Test
  @DisplayName("SQL-like input is stored as plain text and the table survives")
  void sqlLikeInputIsStoredVerbatim() throws Exception {
    String title = "Robert'); DROP TABLE tasks;--";

    postJson("{ \"title\": \"" + title + "\", \"completed\": false }")
        .andExpect(status().isCreated());

    assertThat(onlyStoredTask().getTitle()).isEqualTo(title);

    mockMvc.perform(get("/api/tasks")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Swedish characters and emoji survive the round trip to the database")
  void unicodeIsStoredVerbatim() throws Exception {
    String title = "Köp mjölk 🥛";

    postJson("{ \"title\": \"" + title + "\", \"completed\": false }")
        .andExpect(status().isCreated());

    assertThat(onlyStoredTask().getTitle()).isEqualTo(title);
  }

  @Test
  @DisplayName("an invalid update is rejected and leaves the stored task unchanged")
  void invalidUpdateLeavesTaskUnchanged() throws Exception {
    Long id = taskRepository.save(new Task("Original", "Keep me", false)).getId();

    mockMvc
        .perform(
            put("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"\", \"description\": \"Changed\", \"completed\": true }"))
        .andExpect(status().isBadRequest());

    Task stored = taskRepository.findById(id).orElseThrow();
    assertThat(stored.getTitle()).isEqualTo("Original");
    assertThat(stored.getDescription()).isEqualTo("Keep me");
    assertThat(stored.isCompleted()).isFalse();
  }

  @Test
  @DisplayName("deleting the same task twice gives 204 and then 404")
  void deletingTwiceGivesNotFound() throws Exception {
    Long id = taskRepository.save(new Task("Temporary", null, false)).getId();

    mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNotFound());
  }
}
