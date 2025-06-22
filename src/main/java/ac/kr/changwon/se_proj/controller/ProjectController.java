package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDTO> getAll(@AuthenticationPrincipal UserDetails userDetails){
        return projectService.findAll(userDetails.getUsername());
    }

    @GetMapping("/{id}")
    public ProjectDTO getById(@PathVariable Long id) {
        return projectService.convertToDTO(projectService.findById(id));
    }

    @PostMapping
    public ResponseEntity<String> createProject(@RequestBody ProjectRequestDTO dto) {
        projectService.createProject(dto);
        return ResponseEntity.ok("프로젝트 생성이 완료되었습니다.");
    }

    @GetMapping("/{id}/members")
        public List<UserDto> getMembers(@PathVariable Long id) {
        return projectService.getMembersOfProject(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> update(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        Project updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(projectService.convertToDTO(updatedProject));
    }

    @DeleteMapping("/{id}")
        public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }

    @GetMapping("/user/{userId}")
        public ResponseEntity<List<ProjectDTO>> getUserProjects(@PathVariable("userId") String userId) {
        List<ProjectDTO> projects = projectService.getUserProjects(userId);
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/{projectId}/invite")
    public ResponseEntity<?> inviteUserToProject(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            projectService.addMemberToProject(projectId, username);
            return ResponseEntity.ok().body(Map.of("message", "사용자를 프로젝트에 추가했습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}
