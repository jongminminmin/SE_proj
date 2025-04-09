package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public MainController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String indexPage(){
        return "index";
    }

    @GetMapping("/project")
    public String projectPage(Model model){
        model.addAttribute("project",projectService.findAll());
        return "project";
    }

    @GetMapping("/task")
    public String taskPage(Model model){
        model.addAttribute("task",taskService.findAll());
        return "task";
    }

    @GetMapping("/login")
    public String loginPage(){
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(){
        return "register";
    }

    /**
     * 채팅 페이지 렌더링 시 현재 로그인된 사용자 ID 전달
     */
    @GetMapping("/chat")
    public String chatPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            System.out.println("Principal name: " + authentication.getName());
            model.addAttribute("loginUserId", authentication.getName());
        }
        else {
            model.addAttribute("loginUserId", "anonymous");
        }
        return "chat";
    }


}
