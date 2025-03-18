package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Model.LoginRequest;
import ac.kr.changwon.se_proj.Service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@Controller
public class LoginController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    /* 로그인 화면 관련 컨트롤러*/
    @Autowired
    private LoginService loginService;


    /*로그인 시 ID와 비밀번호를 userRepository에서 검색 후 일치하는 사용자만 welcome page로 리디렉션*/
    @GetMapping("/login")
    public String login(){

        return "login";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginService.authenticate(loginRequest.getId(), loginRequest.getPassword())) {
            // 인증 성공 시 토큰 생성 및 반환
            String token = loginService.generateToken(loginRequest.getId());
            logger.info("Login successful for user: {}", loginRequest.getId());

            return ResponseEntity.ok(token);
        }
        else {
            logger.warn("Failed login attempt for user: {}", loginRequest.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID/Password");
        }
    }

}
