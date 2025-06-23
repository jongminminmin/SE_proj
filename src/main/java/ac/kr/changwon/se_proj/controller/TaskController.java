package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import ac.kr.changwon.se_proj.service.SseService;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.TaskRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final SseService sseService;

    public TaskController(TaskService taskService, SseService sseService) {
        this.taskService = taskService;
        this.sseService = sseService;
    }

    @GetMapping("/")
    public List<TaskDTO> getAll() {
        return taskService.findAll().stream().map(taskService::convertToDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TaskDTO getById(@PathVariable Integer id) {
        return taskService.convertToDTO(taskService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskDTO> create(@RequestBody TaskRequestDTO requestDTO) {
        TaskDTO createdTaskDTO = taskService.createTask(requestDTO);
        return ResponseEntity.ok(createdTaskDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> update(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        TaskDTO updatedTaskDTO = taskService.updateTaskStatus(id, status);
        return ResponseEntity.ok(updatedTaskDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/due-tomorrow")
    public List<TaskDTO> getTasksDueTomorrow() {
        return taskService.getTasksDueTomorrow().stream().map(taskService::convertToDTO).collect(Collectors.toList());
    }

    @GetMapping("/project/{projectId}")
    public List<TaskDTO> getTasksByProjectId(@PathVariable Long projectId) {
        return taskService.findAllTasksByProjectId(projectId);
    }

}
