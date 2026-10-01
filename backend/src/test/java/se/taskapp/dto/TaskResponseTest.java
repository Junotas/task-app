package se.taskapp.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import se.taskapp.model.Task;

class TaskResponseTest {

  @Test
  @DisplayName("from copies every field of the task")
  void fromCopiesEveryField() {
    Task task = new Task("Buy milk", "2 liters", true);
    task.setId(7L);

    TaskResponse response = TaskResponse.from(task);

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.title()).isEqualTo("Buy milk");
    assertThat(response.description()).isEqualTo("2 liters");
    assertThat(response.completed()).isTrue();
  }

  @Test
  @DisplayName("from keeps a missing description as null")
  void fromKeepsMissingDescription() {
    Task task = new Task("Buy milk", null, false);
    task.setId(1L);

    assertThat(TaskResponse.from(task).description()).isNull();
  }

  @Test
  @DisplayName("from rejects a null task with a clear message")
  void fromRejectsNull() {
    assertThatThrownBy(() -> TaskResponse.from(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("task must not be null");
  }
}
