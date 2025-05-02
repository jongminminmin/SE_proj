package ac.kr.changwon.se_proj.service.impl;


import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @PersistenceContext
    private EntityManager em;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public boolean login(String userId, String password) {
        User user = userRepository.findById(userId).
                orElseThrow(() ->
                        new UsernameNotFoundException("존재하지 않는 사용자" + userId)
                );

        //패스워드 인코더 매치
        if(!passwordEncoder.
                matches(password, user.getPassword())) {
            throw new BadCredentialsException("비밀번호 불일치");
        }
        return true;
    }


    @Transactional
    @Override
    public boolean register(String userId, String username, String rawPassword, String email) {
        if(userRepository.existsById(userId)
                || userRepository.existsByEmail(email)){
            return false;
        }
        String role = (userId.equalsIgnoreCase("admin")
                || userId.equalsIgnoreCase("root"))
                ? "ROLE_ADMIN" : "ROLE_USER";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        User u = new User(userId, username, encodedPassword, role);
        userRepository.save(u);
        return true;
    }
}
