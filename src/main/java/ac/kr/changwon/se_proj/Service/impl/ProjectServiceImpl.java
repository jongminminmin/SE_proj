package ac.kr.changwon.se_proj.Service.impl;

import ac.kr.changwon.se_proj.Model.Project;
import ac.kr.changwon.se_proj.Repository.ProjectRepository;
import ac.kr.changwon.se_proj.Service.Interface.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
