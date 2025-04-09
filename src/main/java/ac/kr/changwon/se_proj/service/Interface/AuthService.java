package ac.kr.changwon.se_proj.service.Interface;

public interface AuthService {
    boolean login(String userId, String password);
    boolean register(String userId, String username, String password, String email);
}
