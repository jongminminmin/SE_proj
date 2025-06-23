package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.TaskRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ProjectRepository;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import ac.kr.changwon.se_proj.service.SseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final NotificationServiceImpl notificationService;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           UserRepository userRepository,
                           SseService sseService,
                           NotificationServiceImpl notificationService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.sseService = sseService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public TaskDTO createTask(TaskRequestDTO taskRequestDTO) {
        Project project = projectRepository.findById(taskRequestDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User assignee = null;
        if (taskRequestDTO.getAssigneeId() != null) {
            assignee = userRepository.findById(taskRequestDTO.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
        }

        Task task = new Task();
        task.setTaskTitle(taskRequestDTO.getTaskTitle());
        task.setDescription(taskRequestDTO.getDescription());
        task.setStatus(taskRequestDTO.getStatus());
        task.setProject(project);
        task.setAssignee(assignee);
        if (taskRequestDTO.getDueEnd() != null && !taskRequestDTO.getDueEnd().isEmpty()) {
            LocalDateTime ldt = LocalDateTime.parse(taskRequestDTO.getDueEnd() + "T23:59:59");
            task.setDueEnd(Timestamp.valueOf(ldt));
        }

        Task savedTask = taskRepository.save(task);
        
        if (savedTask.getAssignee() != null) {
            String message = "'" + savedTask.getProject().getProjectTitle() + "' 프로젝트의 '" + savedTask.getTaskTitle() + "' 업무가 할당되었습니다.";
            String url = "/task?projectId=" + savedTask.getProject().getProjectId();
            notificationService.createNotification(savedTask.getAssignee(), message, url);
        }

        TaskDTO taskDTO = convertToDTO(savedTask);
        sseService.broadcastTaskUpdate(taskDTO);
        return taskDTO;
    }

    @Override
    public List<TaskDTO> findAllTasksByProjectId(Long projectId) {
        return taskRepository.findByProject_ProjectId(projectId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskDTO updateTaskStatus(Integer taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        
        TaskDTO taskDTO = convertToDTO(updatedTask);
        sseService.broadcastTaskUpdate(taskDTO);
        return taskDTO;
    }

    @Override
    @Transactional
    public void deleteTask(Integer taskId) {
        taskRepository.deleteById(taskId);
        sseService.broadcastTaskUpdate(new TaskDTO(taskId));
    }

    @Override
    public TaskDTO convertToDTO(Task task) {
        if (task == null) return null;
        TaskDTO dto = new TaskDTO();
        dto.setTaskNo(task.getTaskNo());
        dto.setTaskTitle(task.getTaskTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        if (task.getProject() != null) {
            dto.setProjectId(task.getProject().getProjectId());
        }
        if (task.getDueEnd() != null) {
            dto.setDueEnd(new Timestamp(task.getDueEnd().getTime()).toLocalDateTime().toLocalDate().toString());
        }
        if (task.getAssignee() != null) {
            dto.setAssignee(UserDto.fromEntity(task.getAssignee()));
        }
        return dto;
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

    @Override
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProject_ProjectId(projectId);
    }

    @Override
    public List<Task> getTasksDueTomorrow() {
        LocalDateTime startOfTomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfTomorrow = startOfTomorrow.withHour(23).withMinute(59).withSecond(59);
        return taskRepository.findByDueEndBetween(startOfTomorrow, endOfTomorrow);
    }
}
