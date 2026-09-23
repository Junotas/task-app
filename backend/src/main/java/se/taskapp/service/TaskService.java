package se.taskapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.taskapp.exception.TaskNotFoundException;
import se.taskapp.model.Task;
import se.taskapp.repository.TaskRepository;

@Service
@Transactional
public class TaskService {

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional(readOnly = true)
  public List<Task> getAllTasks() {
    return taskRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Task getTaskById(Long id) {
    return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }

  public Task createTask(Task task) {
    return taskRepository.save(task);
  }

  public Task updateTask(Long id, Task updatedTask) {
    Task existingTask =
        taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

    existingTask.setTitle(updatedTask.getTitle());
    existingTask.setDescription(updatedTask.getDescription());
    existingTask.setCompleted(updatedTask.isCompleted());

    return taskRepository.save(existingTask);
  }

  public void deleteTask(Long id) {
    if (!taskRepository.existsById(id)) {
      throw new TaskNotFoundException(id);
    }
    taskRepository.deleteById(id);
  }
}
