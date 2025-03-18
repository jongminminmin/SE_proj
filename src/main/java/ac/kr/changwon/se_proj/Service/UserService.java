package ac.kr.changwon.se_proj.Service;

import ac.kr.changwon.se_proj.UserRepository.User;

public interface UserService {
    void createUser(User user);
    boolean findById(String userId, String password);
    void searchUser(User user);
    void updateUser(User user);
    void deleteUser(String userId, String password);
}
