package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import ac.kr.changwon.se_proj.service.impl.ProjectServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<ProjectDTO> getAll(){
        if (projectService instanceof ProjectServiceImpl) {
            ProjectServiceImpl impl = (ProjectServiceImpl) projectService;
            return projectService.findAll().stream()
                .map(impl::convertToDTO)
                .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    @GetMapping("/{id}")
    public ProjectDTO getById(@PathVariable Integer id) {
        if (projectService instanceof ProjectServiceImpl) {
            ProjectServiceImpl impl = (ProjectServiceImpl) projectService;
            return impl.convertToDTO(projectService.findById(id));
        } else {
            return null;
        }
    }

    @PostMapping
    public ResponseEntity<String> createProject(@RequestBody ProjectRequestDTO dto) {
        projectServiceImpl.createProject(dto);
        return ResponseEntity.ok("프로젝트 생성이 완료되었습니다.");
    }

    @PutMapping
    public ProjectDTO update(@RequestBody Project project) {
        Project updated = projectService.save(project);
        if (projectService instanceof ProjectServiceImpl) {
            ProjectServiceImpl impl = (ProjectServiceImpl) projectService;
            return impl.convertToDTO(updated);
        } else {
            return null;
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        projectService.delete(id);
    }

    @GetMapping("/{projectId}/users")
    public ResponseEntity<List<UserDto>> getProjectMembers(@PathVariable Integer projectId) {
        //projectId에 속한 사용자목록을 가지고 옴
        List<User> members = projectService.getMembersOfProject(projectId);
        
        List<UserDto> membersDto = members.stream()
                .map(m -> new UserDto(m.getId(), m.getUsername(), null))
        .collect(Collectors.toList());
        
        return ResponseEntity.ok(membersDto);

    }
}
