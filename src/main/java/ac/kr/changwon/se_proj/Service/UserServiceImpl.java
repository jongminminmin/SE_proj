package ac.kr.changwon.se_proj.Service;

import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.UserRepository.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void createUser(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Override
    public boolean findById(String userId, String password) {
        Optional<User> loginUser = userRepository.findById(userId);

        if(loginUser.isEmpty()) {
            return false;
        }

        if(!passwordEncoder.matches(password, loginUser.get().getPassword())) {
            System.out.println("Password unmatched");
            return false;
        }

        return true;
    }
}
