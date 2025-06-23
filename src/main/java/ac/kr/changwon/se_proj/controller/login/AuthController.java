package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.ResetPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = authentication.getName();
        UserDto userDto = authService.getUserById(userId);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody LoginRequestDTO reqDto, HttpServletRequest request) { // HttpServletRequest 파라미터 추가
        Map<String, Object> response = new HashMap<>();
        String userId = reqDto.getUserId();
        String password = reqDto.getPassword();

        // 디버깅 로그 추가
        logger.debug("Login attempt for userId: {}", userId);

        if (userId == null || userId.isBlank() ||
                password == null || password.isBlank()) {
            logger.warn("Login attempt with missing credentials for userId: {}", userId);
            response.put("success", false);
            response.put("message", "userId와 password는 필수 입력입니다.");
            return ResponseEntity.badRequest().body(response);
        }

        logger.info("/api/auth/login 호출됨 요청 DTO: {}", reqDto);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            reqDto.getUserId(),
                            reqDto.getPassword()
                    )
            );
            logger.info("AuthenticationManager.authenticate() 성공. 인증 객체: {}", authentication);

            // SecurityContext에 인증 정보 설정
            SecurityContext context = SecurityContextHolder.createEmptyContext(); // 새 SecurityContext 생성
            context.setAuthentication(authentication); // 인증 정보 설정
            SecurityContextHolder.setContext(context); // SecurityContextHolder에 설정

            // HttpSession에 SecurityContext 명시적으로 저장 (HttpSessionSecurityContextRepository가 처리하도록 위임 가능)
            // SecurityConfig에 HttpSessionSecurityContextRepository가 이미 설정되어 있으므로,
            // SecurityContextHolder에 컨텍스트를 설정하는 것만으로도 세션에 저장되어야 함.
            // 다만, 명시적으로 세션을 가져와서 저장하는 것도 한 방법입니다.
            HttpSession session = request.getSession(true); // true: 세션이 없으면 새로 생성
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            logger.info("SecurityContext가 HttpSession에 저장됨. Session ID: {}", session.getId());


            // 로그인 성공 응답
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Login successful");
            // 필요하다면 여기에 사용자 정보 일부를 포함할 수 있으나,
            // 보통은 /api/users/me 와 같은 별도 엔드포인트에서 가져옵니다.
            return ResponseEntity.ok(responseBody);

        } catch (BadCredentialsException e) {
            logger.warn("로그인 실패 (잘못된 자격 증명): {}", reqDto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "아이디 또는 비밀번호가 잘못되었습니다."));
        } catch (AuthenticationException e) {
            logger.error("로그인 중 인증 오류 발생: {}", reqDto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "인증 중 오류가 발생했습니다."));
        } catch (Exception e) {
            logger.error("로그인 중 알 수 없는 오류 발생: {}", reqDto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "로그인 중 알 수 없는 오류가 발생했습니다."));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody UserDto dto) {
        try{
            boolean ok = authService.register(
                    dto.getId(),
                    dto.getUsername(),
                    dto.getPassword(),
                    dto.getEmail()
            );

            if(ok){
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "회원가입이 성공적으로 완료되었습니다. 로그인해주세요."
                ));
            }
            else{
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "success", false,
                                "message", "이미 존재하는 아이디 또는 이메일 입니다."
                        ));
            }
        }
        catch (Exception e){
            // 기타 예상치 못한 예외 발생 시 (로깅 권장)
            // logger.error("Registration failed for user DTO: {}", dto, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "서버 오류로 회원가입에 실패했습니다: " + e.getMessage()
                    ));
        }
    }


    // 아이디 찾기 엔드포인트
    @PostMapping("/find/id")
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

    // 비밀번호 찾기(사용자 확인) 엔드포인트
    @PostMapping("/find/password")
    public ResponseEntity<Map<String, Object>> verifyUserForPasswordReset(@RequestBody FindPasswordRequestDto findPasswordRequestDto) {
        logger.info("Find password (user verification) attempt for userId: {}", findPasswordRequestDto.getUserId());
        if (findPasswordRequestDto.getUserId() == null || findPasswordRequestDto.getUserId().isBlank() ||
                findPasswordRequestDto.getEmail() == null || findPasswordRequestDto.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "아이디와 이메일을 모두 입력해주세요."));
        }

        boolean userExists = authService.verifyUserForPasswordReset(findPasswordRequestDto);

        if (userExists) {
            logger.info("User verified for password reset: {}", findPasswordRequestDto.getUserId());
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            logger.warn("User verification for password reset failed: userId={}, email={}",
                    findPasswordRequestDto.getUserId(), findPasswordRequestDto.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "입력하신 정보와 일치하는 사용자를 찾을 수 없습니다."));
        }
    }

    // 비밀번호 재설정 엔드포인트
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequestDto resetPasswordRequestDto) {
        logger.info("Password reset attempt for userId: {}", resetPasswordRequestDto.getUserId());
        // 입력 유효성 검사
        if (resetPasswordRequestDto.getNewPassword() == null || resetPasswordRequestDto.getNewPassword().length() < 6) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "비밀번호는 6자 이상이어야 합니다."));
        }
        
        boolean isSuccess = authService.resetPassword(resetPasswordRequestDto);

        if (isSuccess) {
            logger.info("Password has been reset successfully for userId: {}", resetPasswordRequestDto.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 성공적으로 변경되었습니다."));
        } else {
            logger.warn("Password reset failed for userId: {}. User not found with provided details.", resetPasswordRequestDto.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "사용자 정보를 찾을 수 없어 비밀번호를 변경할 수 없습니다."));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // @RequestBody + @Valid 사용 시 유효성 검사 실패 처리
    public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String,String> error = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> error.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error);
    }
}