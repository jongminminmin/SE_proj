package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
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


@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public void createProject(ProjectRequestDTO dto) {
        Project project = new Project(
            dto.getProjectId(), dto.getProjectTitle(),
                dto.getDescription(), dto.getOwnerId(),
                dto.getDate(), dto.getProjectMemberTier()
        );
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
}
