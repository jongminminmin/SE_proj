package ac.kr.changwon.se_proj.Controller;


import ac.kr.changwon.se_proj.Model.Project;
import ac.kr.changwon.se_proj.Service.Interface.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/project")
public class ProjectAdminController {

    private final ProjectService projectService;

    public ProjectAdminController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/isAdmin")
    public ResponseEntity<Map<String, Object>> isAdmin(@PathVariable int projectId,
                                                       @RequestParam(required = false) String projectMemberTier) {
        Map<String, Object> response = new HashMap<>();

        Project project=projectService.findById(projectId);
        if(project == null) {
            response.put("error", "Project not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        boolean isAdmin = project.getOwner().getId().equals(projectMemberTier);
        response.put("isAdmin", isAdmin);
        return ResponseEntity.ok(response);
    }
}
