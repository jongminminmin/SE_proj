package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByLoginId(String loginId);

    boolean findByEmail(String email);
}
