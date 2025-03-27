package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Service.Interface.ProjectService;
import ac.kr.changwon.se_proj.Service.Interface.TaskService;
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

    @GetMapping("/chat")
    public String chatPage(){
        return "chat";
    }


}
