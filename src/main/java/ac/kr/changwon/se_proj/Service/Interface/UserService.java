package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(String id);
    User saveUser(User user);
    void deleteById(String id);
}
