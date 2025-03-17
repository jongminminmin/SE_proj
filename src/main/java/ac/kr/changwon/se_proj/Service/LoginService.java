package ac.kr.changwon.se_proj.Service;


import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.UserRepository.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean authenticate(String id, String rawPassword) {
        Optional<User> searchUser = userRepository.findByUsername(id);
        return searchUser.filter(user -> passwordEncoder.matches(rawPassword, user.getPassword())).isPresent();
    }
}
