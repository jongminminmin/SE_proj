package ac.kr.changwon.se_proj.Service.impl;


import ac.kr.changwon.se_proj.Model.User;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.Service.Interface.AuthService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public boolean login(String userId, String password) {
        Optional<User> optionalUser = userRepository.findById(userId);
        return optionalUser.map(user -> user.getPassword().equals(password)).orElse(false);
    }

    @Override
    public boolean register(String userId, String username, String password, String email) {
        if (userRepository.existsById(userId)) {
            return false; // 이미 존재하는 사용자
        }
        User newUser = new User(userId, username, password, email);
        userRepository.save(newUser);
        return true;
    }
}
