package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    @Override
    public Task findById(Integer id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Override
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public void deleteById(Integer id) {
        taskRepository.deleteById(id);
    }
}
