package se.taskapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.taskapp.exception.TaskNotFoundException;
import se.taskapp.model.Task;
import se.taskapp.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task taskWithId(Long id, String title, String description, boolean completed) {
        Task task = new Task(title, description, completed);
        task.setId(id);
        return task;
    }

    @Test
    @DisplayName("createTask saves the task and returns it")
    void createTaskSavesAndReturnsTask() {
        Task input = new Task("Buy milk", "2 liters", false);
        Task saved = taskWithId(1L, "Buy milk", "2 liters", false);
        when(taskRepository.save(input)).thenReturn(saved);

        Task result = taskService.createTask(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Buy milk");
        verify(taskRepository).save(input);
    }

    @Test
    @DisplayName("getAllTasks returns every task from the repository")
    void getAllTasksReturnsAllTasks() {
        List<Task> tasks = List.of(
                taskWithId(1L, "First", null, false),
                taskWithId(2L, "Second", null, true));
        when(taskRepository.findAll()).thenReturn(tasks);

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Task::getTitle).containsExactly("First", "Second");
    }

    @Test
    @DisplayName("getTaskById returns the task when it exists")
    void getTaskByIdReturnsTask() {
        Task task = taskWithId(1L, "Buy milk", "2 liters", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Buy milk");
    }

    @Test
    @DisplayName("getTaskById throws TaskNotFoundException for an unknown id")
    void getTaskByIdThrowsWhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("updateTask overwrites the fields of an existing task")
    void updateTaskUpdatesExistingTask() {
        Task existing = taskWithId(1L, "Old title", "Old description", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.updateTask(1L, new Task("New title", "New description", true));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateTask throws TaskNotFoundException for an unknown id")
    void updateTaskThrowsWhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, new Task("Title", null, false)))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask deletes an existing task")
    void deleteTaskDeletesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTask throws TaskNotFoundException for an unknown id")
    void deleteTaskThrowsWhenNotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).deleteById(99L);
    }
}