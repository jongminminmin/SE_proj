package ac.kr.changwon.se_proj.service.impl;


import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {


    @Autowired
    private PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public boolean login(String userId, String password) {
        Optional<User> optionalUser = userRepository.findById(userId);
        return optionalUser.map
                (user ->
                        passwordEncoder.matches(password, user.getPassword())).orElse(false);
    }

    @Override
    public boolean register(String userId, String username, String password, String email) {
        // 유효성 검증
        if (userId == null || userId.isBlank() || username == null || username.isBlank() ||
                password == null || password.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("입력 값이 유효하지 않습니다.");
        }

        if (userRepository.existsById(userId)) {
            return false; // 이미 존재하는 사용자
        }


        //규칙 부여
        String role;
        if (userId.equalsIgnoreCase("admin") || userId.equalsIgnoreCase("root") ||
                username.equalsIgnoreCase("admin") || username.equalsIgnoreCase("root")) {
            role = "ADMIN";
        } else {
            role = "USER";
        }


        //비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User(userId, username, encodedPassword, email);
        newUser.setRole(role); // 역할 저장

        try
        {
            userRepository.save(newUser);
        }
        catch (Exception e) {
            // 데이터 저장 실패시 예외 처리
            throw new RuntimeException("사용자 저장 실패", e);
        }
        return true;

    }
}
