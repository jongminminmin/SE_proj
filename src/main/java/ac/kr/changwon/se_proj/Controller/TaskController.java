package ac.kr.changwon.se_proj.Controller;


import ac.kr.changwon.se_proj.Model.Task;
import ac.kr.changwon.se_proj.Service.Interface.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public Task getById(@PathVariable Integer id) {
        return taskService.findById(id);
    }

    @PostMapping
    public Task create(@RequestBody Task task) {
        return taskService.save(task);
    }

    @PutMapping
    public Task update(@RequestBody Task task) {
        return taskService.save(task);
    }

    @DeleteMapping
    public void delete(@RequestBody Integer id) {
        taskService.deleteById(id);
    }
}
