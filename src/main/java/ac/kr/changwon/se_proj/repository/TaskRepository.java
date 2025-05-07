package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    List<Task> findByProjectId(Integer projectId); //하나의 프로젝트의 모든 업무

    List<Task> findByDueEnd(LocalDate dueEnd);
}
