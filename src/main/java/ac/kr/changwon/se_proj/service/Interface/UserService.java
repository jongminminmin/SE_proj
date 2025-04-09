package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    User findByIdAndEmail(String id, String email);
    User saveUser(User user);
    User updateUser(User user);
    User findById(String id);
    void deleteById(String id);
}
