package ac.kr.changwon.se_proj.Service.impl;


import ac.kr.changwon.se_proj.Model.User;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.Service.Interface.AuthService;
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
        if (userRepository.existsById(userId)) {
            return false; // 이미 존재하는 사용자
        }
        //규칙 부여
        String role = (userId.equalsIgnoreCase("admin") ||
                userId.equalsIgnoreCase("root") ||
                username.equalsIgnoreCase("admin") ||
                username.equalsIgnoreCase("root"))
                ? "ADMIN" : "USER";

        //비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);


        User newUser = new User(userId, username, encodedPassword, email);
        userRepository.save(newUser);
        return true;
    }
}
