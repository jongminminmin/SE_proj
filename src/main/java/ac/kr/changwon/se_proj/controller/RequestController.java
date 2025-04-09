package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.service.Interface.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth/login/{api}")
public class RequestController {

    private final UserService userService;

    public RequestController(UserService userService) {
        this.userService = userService;
    }


}
