package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<Project> getAll(){
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public Project getById(@PathVariable Integer id) {
        return projectService.findById(id);
    }

    @PostMapping
    public Project update(@RequestBody Project project) {
        return projectService.save(project);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        projectService.delete(id);
    }



}
