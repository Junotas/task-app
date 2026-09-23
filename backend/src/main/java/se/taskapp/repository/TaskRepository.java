package se.taskapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.taskapp.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {}
