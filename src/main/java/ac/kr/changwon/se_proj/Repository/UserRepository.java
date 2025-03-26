package ac.kr.changwon.se_proj.Repository;

import ac.kr.changwon.se_proj.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
}
