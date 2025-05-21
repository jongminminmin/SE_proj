package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import ac.kr.changwon.se_proj.service.impl.ProjectServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectServiceImpl projectServiceImpl;

    public ProjectController(ProjectService projectService, ProjectServiceImpl projectServiceImpl) {
        this.projectService = projectService;
        this.projectServiceImpl = projectServiceImpl;
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

    public ResponseEntity<String> createProject(@RequestBody ProjectRequestDTO dto) {
        projectServiceImpl.createProject(dto);
        return ResponseEntity.ok("프로젝트 생성이 완료되었습니다.");
    }

}
