package se.taskapp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import se.taskapp.model.Task;
import se.taskapp.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private TaskRepository taskRepository;

  @BeforeEach
  void clearDatabase() {
    taskRepository.deleteAll();
  }

  private Long persistTask(String title, String description, boolean completed) {
    return taskRepository.save(new Task(title, description, completed)).getId();
  }

  @Test
  @DisplayName("POST /api/tasks returns 201 and persists the task")
  void createTaskReturns201() throws Exception {
    String body =
        """
                {
                  "title": "Buy milk",
                  "description": "2 liters",
                  "completed": false
                }
                """;

    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title", is("Buy milk")))
        .andExpect(jsonPath("$.description", is("2 liters")))
        .andExpect(jsonPath("$.completed", is(false)));

    org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("POST /api/tasks returns 400 when title is blank")
  void createTaskWithBlankTitleReturns400() throws Exception {
    String body =
        """
                {
                  "title": "",
                  "description": "No title",
                  "completed": false
                }
                """;

    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)));
  }

  @Test
  @DisplayName("GET /api/tasks returns all tasks")
  void getAllTasksReturnsTasks() throws Exception {
    persistTask("First", "One", false);
    persistTask("Second", "Two", true);

    mockMvc
        .perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("First")))
        .andExpect(jsonPath("$[1].completed", is(true)));
  }

  @Test
  @DisplayName("GET /api/tasks/{id} returns the task")
  void getTaskByIdReturnsTask() throws Exception {
    Long id = persistTask("Buy milk", "2 liters", false);

    mockMvc
        .perform(get("/api/tasks/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id.intValue())))
        .andExpect(jsonPath("$.title", is("Buy milk")));
  }

  @Test
  @DisplayName("GET /api/tasks/{id} returns 404 for an unknown id")
  void getTaskByIdReturns404() throws Exception {
    mockMvc
        .perform(get("/api/tasks/{id}", 9999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @DisplayName("PUT /api/tasks/{id} updates the task")
  void updateTaskReturns200() throws Exception {
    Long id = persistTask("Old title", "Old description", false);

    String body =
        """
                {
                  "title": "New title",
                  "description": "New description",
                  "completed": true
                }
                """;

    mockMvc
        .perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id.intValue())))
        .andExpect(jsonPath("$.title", is("New title")))
        .andExpect(jsonPath("$.completed", is(true)));

    mockMvc
        .perform(get("/api/tasks/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completed", is(true)));
  }

  @Test
  @DisplayName("PUT /api/tasks/{id} returns 404 for an unknown id")
  void updateTaskReturns404() throws Exception {
    String body =
        """
                {
                  "title": "Title",
                  "description": null,
                  "completed": false
                }
                """;

    mockMvc
        .perform(put("/api/tasks/{id}", 9999).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE /api/tasks/{id} returns 204 and removes the task")
  void deleteTaskReturns204() throws Exception {
    Long id = persistTask("Temporary", null, false);

    mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE /api/tasks/{id} returns 404 for an unknown id")
  void deleteTaskReturns404() throws Exception {
    mockMvc.perform(delete("/api/tasks/{id}", 9999)).andExpect(status().isNotFound());
  }
}
