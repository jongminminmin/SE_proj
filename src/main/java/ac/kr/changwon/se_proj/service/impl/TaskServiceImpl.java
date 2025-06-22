package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ProjectRepository;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.dto.TaskRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
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
    @Transactional
    public Task save(Task task) {
        if (task.getProject() != null && task.getProject().getProjectId() != null) {
            Project project = projectRepository.findById(task.getProject().getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            task.setProject(project);
        }

        if (task.getAssignee() != null && task.getAssignee().getId() != null) {
            User assignee = userRepository.findById(task.getAssignee().getId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }

        if (task.getTaskNo() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task createTask(TaskRequestDTO request) {
        Task task = new Task();
        task.setTaskTitle(request.getTaskTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        if (request.getDueEnd() != null && !request.getDueEnd().isEmpty()) {
            task.setDueEnd(java.sql.Date.valueOf(request.getDueEnd()));
        }

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + request.getProjectId()));
            task.setProject(project);
        }

        if (request.getAssigneeId() != null && !request.getAssigneeId().isEmpty()) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found with id: " + request.getAssigneeId()));
            task.setAssignee(assignee);
        }
        
        task.setCreatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTaskStatus(Integer taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        task.setStatus(status);
        return taskRepository.save(task);
    }

    @Override
    public void deleteById(Integer id) {
        taskRepository.deleteById(id);
    }

    @Override
    public List<Task> getTasksDueTomorrow() {
        return taskRepository.findByDueEnd(LocalDate.now().plusDays(1));
    }

    @Override
    public TaskDTO convertToDTO(Task task) {
        if (task == null) return null;
        TaskDTO dto = new TaskDTO();
        dto.setTaskNo(task.getTaskNo());
        if (task.getProject() != null) {
            dto.setProjectId(task.getProject().getProjectId().intValue());
        }
        dto.setTaskTitle(task.getTaskTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setDueStart(task.getDueStart());
        dto.setDueEnd(task.getDueEnd());
        dto.setTaskContent(task.getTaskContent());
        if (task.getAssignee() != null) {
            dto.setAssignee(UserDto.fromEntity(task.getAssignee()));
        }
        return dto;
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProject_ProjectId(projectId);
    }
}
