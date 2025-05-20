package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserRepository extends JpaRepository<User, String> {
    //로그인 아이디 중복체크
    boolean existsById(String id);

    //이메일 중복 체크
    boolean existsByEmail(String email);

    //닉네임 중복 체크
    boolean existsByUsername(String username);

    //사용자 존재 체크
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // 이메일로 사용자 찾기
    Optional<User> findByIdAndEmail(String userId, String email); // 아이디와 이메일로 사용자 찾기
    Optional<User> findByid(String id);

}
