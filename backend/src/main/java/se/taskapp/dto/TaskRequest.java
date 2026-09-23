package se.taskapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
    @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
    @Size(max = 1000, message = "description must be at most 1000 characters") String description,
    boolean completed) {}
