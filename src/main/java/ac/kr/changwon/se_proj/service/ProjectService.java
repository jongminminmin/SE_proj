package ac.kr.changwon.se_proj.service;

import ac.kr.changwon.se_proj.dto.ProjectDTO;
import java.util.List;

public interface ProjectService {
    ProjectDTO createProject(ProjectDTO projectDTO);
    List<ProjectDTO> getAllProjects();
    ProjectDTO getProjectById(Long id);
    List<ProjectDTO> getUserProjects(Long userId);
    void addMemberToProject(Long projectId, String username);
} 