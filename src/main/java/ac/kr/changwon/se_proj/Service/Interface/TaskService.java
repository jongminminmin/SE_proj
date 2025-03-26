package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.Task;

import java.util.List;

public interface TaskService {
    List<Task> findAll();
    Task findById(Integer id);
    Task save(Task task);
    void deleteById(Integer id);
}
