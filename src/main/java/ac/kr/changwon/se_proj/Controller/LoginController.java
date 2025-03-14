package ac.kr.changwon.se_proj.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/* 로그인 컨트롤러
* 요청 시 로그인 페이지로 이동 */
@RestController
public class LoginController {
    
    /* 로그인 화면 관련 컨트롤러*/
    @Autowired
    private LoginService loginService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if(loginService.authenticate(loginRequest.getId(),loginRequest.getPassword())) {
            return ResponseEntity.ok(loginRequest);
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID/Password");
        }
    }

}
