package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ProjectRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public void createProject(ProjectRequestDTO dto) {
        Project project = new Project();
        project.setProjectTitle(dto.getProjectTitle());
        project.setDescription(dto.getDescription());
        // ownerId -> User 변환
        User owner = userRepository.findById(dto.getOwnerId()).orElseThrow(() -> new RuntimeException("해당 ownerId의 사용자가 존재하지 않습니다."));
        project.setOwner(owner);
        // LocalDate -> java.util.Date 변환
        if (dto.getDate() != null) {
            project.setDate(java.sql.Date.valueOf(dto.getDate()));
        }
        project.setProjectMemberTier(dto.getProjectMemberTier());
        projectRepository.save(project);
    }

    @Override
    public Project findById(Integer id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Override
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public void delete(Integer id) {
        projectRepository.deleteById(id);
    }

    @Override
    public List<User> getMembersOfProject(Integer id) {
        //프로젝트 ID를 사용하여 해당 프로젝트의 멤버를 가지고 옴
        Optional<Project> projectOptional = projectRepository.findById(id);
        if(projectOptional.isPresent()) {
            Project project = projectOptional.get();

            //project 엔티티에 List<User> members 필드가 있음.

        }

        return Collections.emptyList();
    }

    // Project -> ProjectDTO 변환 메서드 추가
    public ProjectDTO convertToDTO(Project project) {
        if (project == null) return null;
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(project.getProjectId());
        dto.setProjectTitle(project.getProjectTitle());
        dto.setDescription(project.getDescription());
        dto.setDate(project.getDate());
        dto.setProjectMemberTier(project.getProjectMemberTier());
        // owner 변환
        if (project.getOwner() != null) {
            dto.setOwner(UserDto.fromEntity(project.getOwner()));
        }
        // members 변환
        if (project.getMembers() != null) {
            Set<UserDto> memberDtos = project.getMembers().stream()
                .map(UserDto::fromEntity)
                .collect(java.util.stream.Collectors.toSet());
            dto.setMembers(memberDtos);
        }
        return dto;
    }
}
