package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.Task;

import java.util.List;

public interface TaskService {
    List<Task> findAll();
    Task findById(Integer id);
    Task save(Task task);
    void deleteById(Integer id);

    List<Task> getTasksDueTomorrow();
}
