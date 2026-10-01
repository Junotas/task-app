package se.taskapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskRequest(
    @NotNull(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        @Pattern(
            regexp = ValidationPatterns.VISIBLE_TEXT,
            message = "title must contain visible text and no control characters")
        String title,
    @Size(max = 1000, message = "description must be at most 1000 characters")
        @Pattern(
            regexp = ValidationPatterns.NO_CONTROL_CHARACTERS,
            message = "description must not contain control characters")
        String description,
    boolean completed) {

  public TaskRequest {
    title = title == null ? null : title.strip();

    if (description != null) {
      description = description.strip();

      if (description.isEmpty()) {
        description = null;
      }
    }
  }
}
