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
        String id = user.getId();
        String username = user.getUsername();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        User newUser = new User();
        newUser.setId(id);
        newUser.setUsername(username);
        newUser.setPassword(encodedPassword);
        userRepository.save(newUser);
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

    @Override
    public void searchUser(User user) {
        Optional<User> loginUser = userRepository.findById(user.getId());
        if(loginUser.isEmpty()) {
            System.out.println("User not found");
        }
        else {

        }
    }

    @Override
    public void updateUser(User user) {

    }

    @Override
    public void deleteUser(String userId, String password) {

    }
}
