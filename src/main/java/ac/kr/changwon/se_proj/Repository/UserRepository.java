package ac.kr.changwon.se_proj.Repository;

import ac.kr.changwon.se_proj.UserRepository.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface UserRepository extends JpaRepository<User, String> {
}
