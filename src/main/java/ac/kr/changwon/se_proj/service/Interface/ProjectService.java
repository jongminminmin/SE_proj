package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.Project;
import java.util.List;

public interface ProjectService {
    List<ProjectDTO> findAll(String userId);
    Project findById(Long id);
    void delete(Long id);
    void createProject(ProjectRequestDTO dto);
    ProjectDTO convertToDTO(Project project);
    List<UserDto> getMembersOfProject(Long id);
    List<ProjectDTO> getUserProjects(String userId);
    void addMemberToProject(Long projectId, String username);
    Project updateProject(Long projectId, ProjectDTO projectDTO);
}