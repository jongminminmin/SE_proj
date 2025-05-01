package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    boolean findByEmail(String email);
    boolean existsById(String id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
