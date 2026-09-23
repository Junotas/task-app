package se.taskapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import se.taskapp.dto.TaskRequest;
import se.taskapp.dto.TaskResponse;
import se.taskapp.model.Task;
import se.taskapp.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public ResponseEntity<List<TaskResponse>> getAllTasks() {
    List<TaskResponse> tasks = taskService.getAllTasks().stream().map(TaskResponse::from).toList();

    return ResponseEntity.ok(tasks);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
    return ResponseEntity.ok(TaskResponse.from(taskService.getTaskById(id)));
  }

  @PostMapping
  public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
    Task created = taskService.createTask(toEntity(request));

    return ResponseEntity.created(URI.create("/api/tasks/" + created.getId()))
        .body(TaskResponse.from(created));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TaskResponse> updateTask(
      @PathVariable Long id, @Valid @RequestBody TaskRequest request) {
    Task updated = taskService.updateTask(id, toEntity(request));

    return ResponseEntity.ok(TaskResponse.from(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);

    return ResponseEntity.noContent().build();
  }

  private Task toEntity(TaskRequest request) {
    return new Task(request.title(), request.description(), request.completed());
  }
}
