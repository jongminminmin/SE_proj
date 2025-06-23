package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.TaskRequestDTO;

import java.util.List;

public interface TaskService {
    List<Task> findAll();
    Task findById(Integer id);
    Task save(Task task);
    TaskDTO createTask(TaskRequestDTO taskRequestDTO);
    TaskDTO updateTaskStatus(Integer taskId, String status);
    void deleteById(Integer id);

    List<Task> findByProjectId(Long projectId);

    List<Task> getTasksDueTomorrow();

    TaskDTO convertToDTO(Task task);

    List<TaskDTO> findAllTasksByProjectId(Long projectId);
    void deleteTask(Integer taskId);
}
