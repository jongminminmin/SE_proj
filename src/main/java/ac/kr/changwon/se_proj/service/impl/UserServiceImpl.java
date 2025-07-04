package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findByIdAndEmail(String id, String email) {
        return userRepository.findById(id).orElse(null);
    }


    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }



    @Override
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}
