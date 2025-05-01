package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.service.Interface.AuthService;
import ac.kr.changwon.se_proj.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    public String register(
            @ModelAttribute("userDto") @Valid UserDto dto,
            BindingResult br,
            Model model
    ) {
        // 1) 입력 검증 에러 체크
        if (br.hasErrors()) {
            return "register";
        }

        // 2) 서비스 호출 (중복 체크 + insert or skip)
        boolean ok = authService.register(
                dto.getId(), dto.getUsername(), dto.getPassword(), dto.getEmail()
        );

        // 3) 이미 DB에 존재해서 등록 안 된 경우
        if (!ok) {
            model.addAttribute("errorMessage", "이미 존재하는 아이디입니다.");
            return "register";
        }

        // 4) 성공하면 로그인 페이지로 리다이렉트
        return "redirect:/login";
    }
}