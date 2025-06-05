package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.model.User;

import java.util.List;

public interface ProjectService {
    List<Project> findAll();
    Project findById(Integer id);
    Project save(Project project);
    void delete(Integer id);

    //CRUD 이외의 작업
    List<User> getMembersOfProject(Integer id);
}
