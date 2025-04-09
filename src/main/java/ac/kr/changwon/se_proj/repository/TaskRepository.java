package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

}
