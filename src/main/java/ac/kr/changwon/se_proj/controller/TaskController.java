package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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
    public TaskDTO create(@RequestBody Task task) {
        return taskService.convertToDTO(taskService.save(task));
    }

    @PutMapping
    public TaskDTO update(@RequestBody Task task) {
        return taskService.convertToDTO(taskService.save(task));
    }

    @DeleteMapping
    public void delete(@RequestBody Integer id) {
        taskService.deleteById(id);
    }

    @GetMapping("/due-tomorrow")
    public List<TaskDTO> getTasksDueTomorrow() {
        return taskService.getTasksDueTomorrow().stream().map(taskService::convertToDTO).collect(Collectors.toList());
    }

}
