package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ProjectRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ProjectServiceImpl implements ProjectService
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ProjectDTO> findAll(String userId) {
        List<Project> projects = projectRepository.findProjectsForUser(userId);
        return projects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
    public Project findById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Override
    public void delete(Long id) {
        // 현재 인증된 사용자 정보 가져오기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = ((UserDetails)principal).getUsername();

        // 프로젝트 정보 조회
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

        // 소유자 확인 (null 체크 강화)
        if (project.getOwner() == null || !project.getOwner().getId().equals(currentUserId)) {
            throw new SecurityException("프로젝트 소유자만 삭제할 수 있습니다.");
        }

        projectRepository.deleteById(id);
    }

    @Override
    public Project updateProject(Long projectId, ProjectDTO projectDTO) {
        // 현재 인증된 사용자 정보 가져오기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = ((UserDetails)principal).getUsername();

        // DB에서 기존 프로젝트 정보를 온전히 불러오기
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));

        // 소유자 확인 (오타 수정 및 null 체크 강화)
        if (existingProject.getOwner() == null || !existingProject.getOwner().getId().equals(currentUserId)) {
            throw new SecurityException("프로젝트 소유자만 수정할 수 있습니다.");
        }

        // 필요한 필드만 안전하게 업데이트
        existingProject.setProjectTitle(projectDTO.getProjectTitle());
        existingProject.setDescription(projectDTO.getDescription());

        return projectRepository.save(existingProject);
    }

    @Override
    public List<UserDto> getMembersOfProject(Long id) {
        Optional<Project> projectOptional = projectRepository.findById(id);
        if (projectOptional.isPresent()) {
            Project project = projectOptional.get();
            Set<User> allMembers = new HashSet<>(project.getMembers());
            allMembers.add(project.getOwner());
            return allMembers.stream()
                    .map(UserDto::fromEntity)
                    .collect(Collectors.toList());
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
                .collect(Collectors.toSet());
            // 프로젝트 소유자도 멤버 목록에 포함
            if (project.getOwner() != null) {
                memberDtos.add(UserDto.fromEntity(project.getOwner()));
            }
            dto.setMembers(memberDtos);
        }
        return dto;
    }

    public List<ProjectDTO> getUserProjects(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        return user.getProjects().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMemberToProject(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 ID의 프로젝트를 찾을 수 없습니다: " + projectId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("해당 사용자명의 사용자를 찾을 수 없습니다: " + username));

        project.getMembers().add(user);
        user.getProjects().add(project);

        projectRepository.save(project);
    }
}
