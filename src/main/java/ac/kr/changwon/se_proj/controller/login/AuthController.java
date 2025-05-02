package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.service.Interface.AuthService;
import ac.kr.changwon.se_proj.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
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
    public ResponseEntity<String> register(@RequestBody
                                               UserDto
                                                       dto) {
        boolean ok = authService.register(
                dto.getId(),
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail()
        );

        if(!ok){
            //http 400 + 메시지 바디
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body("이미 존재하는 아이디입니다.");
        }

        return ResponseEntity.ok("가입성공");
    }


    @ExceptionHandler
    public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String,String> error = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> error.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error);

    }
}