package ac.kr.changwon.se_proj.Controller;


import ac.kr.changwon.se_proj.Model.RegisterRequest;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.Service.UserService;
import ac.kr.changwon.se_proj.UserRepository.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    /*회원가입 컨트롤러*/
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterPage() {
        logger.info("Accessing register page");
        return "register";
    }



    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@RequestBody RegisterRequest registerRequest){
        if(userRepository.findByUsername(registerRequest.getUserId()).isPresent()){
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequest.getUserId());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userService.createUser(user);

        return "Registered Successfully";
    }
}
