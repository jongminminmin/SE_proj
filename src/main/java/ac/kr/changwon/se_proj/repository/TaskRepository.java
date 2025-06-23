package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    List<Task> findByDueEnd(LocalDate dueDate);

    List<Task> findByProject_ProjectId(Long projectId);

    List<Task> findByDueEndBetween(LocalDateTime start, LocalDateTime end);
}
