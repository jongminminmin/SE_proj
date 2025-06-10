package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import org.springframework.stereotype.Service;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.UserDto;

import java.time.LocalDate;
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

    public Task updateTask(Integer id, Task updated) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("업무를 찾을 수 없습니다."));

        task.setProjectId(updated.getProjectId());
        task.setTaskTitle(updated.getTaskTitle());
        task.setDescription(updated.getDescription());
        task.setDueStart(updated.getDueStart());
        task.setDueEnd(updated.getDueEnd());
        task.setTaskContent(updated.getTaskContent());
        task.setAssignee(updated.getAssignee());

        return taskRepository.save(task);
    }

    @Override
    public void deleteById(Integer id) {
        taskRepository.deleteById(id);
    }

    public List<Task> getTasksDueTomorrow() {
        return taskRepository.findByDueEnd(LocalDate.now().plusDays(1));
    }

    public TaskDTO convertToDTO(Task task) {
        if (task == null) return null;
        TaskDTO dto = new TaskDTO();
        dto.setTaskNo(task.getTaskNo());
        dto.setProjectId(task.getProjectId());
        dto.setTaskTitle(task.getTaskTitle());
        dto.setDescription(task.getDescription());
        dto.setDueStart(task.getDueStart());
        dto.setDueEnd(task.getDueEnd());
        dto.setTaskContent(task.getTaskContent());
        if (task.getAssignee() != null) {
            dto.setAssignee(UserDto.fromEntity(task.getAssignee()));
        }
        return dto;
    }

}
