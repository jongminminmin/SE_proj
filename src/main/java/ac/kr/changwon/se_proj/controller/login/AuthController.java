package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@Controller
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
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


    // 아이디 찾기 엔드포인트
    @PostMapping("/find/id")
    @ResponseBody // JSON 응답을 위해 추가
    public ResponseEntity<Map<String, Object>> findUserId(@RequestBody FindIdRequestDto findIdRequestDto) {
        logger.info("Find ID attempt for email: {}", findIdRequestDto.getEmail());
        if (findIdRequestDto.getEmail() == null || findIdRequestDto.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이메일을 입력해주세요."));
        }
        String userId = authService.findUserIdByEmail(findIdRequestDto);
        if (userId != null) {
            // 보안 참고: 실제 운영 환경에서는 아이디 전체를 그대로 노출하는 것보다
            // 부분 마스킹 처리(예: test***) 또는 "가입된 아이디가 있습니다" 정도의 안내를 고려해야 합니다.
            logger.info("User ID found for email {}: {}", findIdRequestDto.getEmail(), userId);
            return ResponseEntity.ok(Map.of("success", true, "userId", userId));
        } else {
            logger.warn("User ID not found for email: {}", findIdRequestDto.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "해당 이메일로 가입된 아이디를 찾을 수 없습니다."));
        }
    }

    // 비밀번호 찾기(재설정 전 사용자 확인 및 토큰 발급) 엔드포인트
    @PostMapping("/find/password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> findPasswordAndGenerateToken(@RequestBody FindPasswordRequestDto findPasswordRequestDto) {
        logger.info("Find password (user check and token generation) attempt for userId: {}", findPasswordRequestDto.getUserId());
        if (findPasswordRequestDto.getUserId() == null || findPasswordRequestDto.getUserId().isBlank() ||
                findPasswordRequestDto.getEmail() == null || findPasswordRequestDto.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "아이디와 이메일을 모두 입력해주세요."));
        }

        // AuthService의 generatePasswordResetToken 메서드는 String 타입의 토큰을 반환합니다.
        String resetToken = authService.generatePasswordResetToken(findPasswordRequestDto);

        if (resetToken != null) {
            // 토큰이 성공적으로 생성됨 (사용자 정보 일치)
            // 중요: 실제 운영 환경에서는 생성된 토큰을 사용자에게 직접 반환하는 대신,
            // 이 토큰을 포함한 비밀번호 재설정 링크를 이메일로 발송해야 합니다.
            // 클라이언트에게는 이메일 발송 안내 메시지만 전달하는 것이 일반적입니다.
            logger.info("Password reset token generated for user: userId={}, email={}. Token (for dev only): {}",
                    findPasswordRequestDto.getUserId(), findPasswordRequestDto.getEmail(), resetToken);
            // 클라이언트에게는 토큰 자체를 반환하지 않고, 안내 메시지만 전달합니다.
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호 재설정 절차가 시작되었습니다. 이메일을 확인해주세요. (실제 이메일 발송 기능은 구현 필요)"
                    // "token", resetToken // 개발/테스트 목적으로만 토큰을 반환하고, 실제 운영에서는 제거
            ));
        }
        else {
            // 사용자를 찾을 수 없거나 토큰 생성 실패
            logger.warn("User not found for password reset or token generation failed: userId={}, email={}",
                    findPasswordRequestDto.getUserId(), findPasswordRequestDto.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "입력하신 정보와 일치하는 사용자를 찾을 수 없습니다."));
        }
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