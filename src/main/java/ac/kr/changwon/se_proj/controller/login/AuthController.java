package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String,Object>> login(@RequestBody LoginRequestDTO req) {
        String userId   = req.getUserId();
        String password = req.getPassword();

        if (userId == null || userId.isBlank() ||
                password == null || password.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "userId와 password는 필수 입력입니다."
                    ));
        }

        boolean ok = authService.login(userId, password);
        return ResponseEntity.ok(Map.of(
                "success", ok,
                "message", ok ? "Login successful" : "Invalid credentials"
        ));
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