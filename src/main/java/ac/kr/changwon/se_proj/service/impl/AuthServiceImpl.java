package ac.kr.changwon.se_proj.service.impl;


import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public boolean login(String userId, String password) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new UsernameNotFoundException("회원 없음"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("비밀번호 불일치");
        }
        // 로그인 성공 로직
        return true;
    }

    @Override
    public boolean register(String userId, String username, String rawPassword, String email) {
        // 1) 입력검증, 2) 중복검사
        if (userRepository.existsById(userId) || userRepository.findByEmail(email)) {
            return false;
        }
        String role = (userId.equalsIgnoreCase("admin") || userId.equalsIgnoreCase("root"))
                ? "ROLE_ADMIN" : "ROLE_USER";
        String pw = passwordEncoder.encode(rawPassword);

        User u = new User(userId, username, pw, email, role);
        em.persist(u);  // 무조건 INSERT
        return true;
    }


}
