package ac.kr.changwon.se_proj.Repository;

import ac.kr.changwon.se_proj.Model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
}
