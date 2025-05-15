package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@Controller
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
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
    public String register(UserDto dto, RedirectAttributes redirectAttributes, Model model) {
        boolean ok = authService.register(
                dto.getId(),
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail()
        );

        if(!ok){
            // 회원가입 실패 시, 오류 메시지를 모델에 담아 다시 회원가입 페이지로 이동
            // RedirectAttributes를 사용하면 리다이렉트 후에도 값을 사용할 수 있습니다 (Flash Attribute).
            redirectAttributes.addFlashAttribute("registrationError", "이미 존재하는 아이디이거나 회원가입에 실패했습니다. 다시 시도해주세요.");
            return "redirect:/register"; // GET /api/auth/register 로 리다이렉트 (위의 showRegisterPage 메소드 호출)
            // 또는 직접 뷰 이름 "register" 반환 (이 경우 URL은 POST /api/auth/register 유지)
            // "redirect:/register.html" 또는 "redirect:/register" (SecurityConfig의 permitAll 경로에 따라)
        }
        // 회원가입 성공 시, 성공 메시지와 함께 로그인 페이지로 리다이렉션
        redirectAttributes.addFlashAttribute("registrationSuccess", "회원가입이 성공적으로 완료되었습니다. 로그인해주세요.");
        return "redirect:/login"; // 로그인 페이지 경로 (예: /login.html 또는 SecurityConfig에 설정된 /login)
    }


    @ExceptionHandler(MethodArgumentNotValidException.class) // @RequestBody + @Valid 사용 시 유효성 검사 실패 처리
    @ResponseBody
    public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String,String> error = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> error.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error);
    }
}