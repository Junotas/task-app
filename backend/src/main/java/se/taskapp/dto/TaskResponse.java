package se.taskapp.dto;

import java.util.Objects;

import se.taskapp.model.Task;

public record TaskResponse(Long id, String title, String description, boolean completed) {

  public static TaskResponse from(Task task) {
    Objects.requireNonNull(task, "task must not be null");

    return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.isCompleted());
  }
}