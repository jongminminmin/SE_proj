package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Service.Interface.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String userId,
                                     @RequestParam String password) {
        boolean result = authService.login(userId, password);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "Login successful" : "Invalid credentials");
        return response;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String userId,
                                        @RequestParam String username,
                                        @RequestParam String password,
                                        @RequestParam(required = false) String email) {
        boolean result = authService.register(userId, username, password, email);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "Registration successful" : "User already exists");
        return response;
    }
}