package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.Project;

import java.util.List;

public interface ProjectService {
    List<Project> findAll();
    Project findById(Integer id);
    Project save(Project project);
    void delete(Integer id);
}
