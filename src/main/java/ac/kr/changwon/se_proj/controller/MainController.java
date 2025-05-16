package ac.kr.changwon.se_proj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {


    /*
    *   해당 컨트롤러는 SPA의 React 컴포넌트로
    *   리디렉션 하는 코드로
    *   변경
    * */

    // 생성자를 기본으로 변경 (서비스 의존성 제거 시)
    public MainController() {
        // 필요한 경우 초기화 로직
    }


    @GetMapping("/")
    public String indexPage(){
        // 루트 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }

    @GetMapping("/main")
    public String mainPage(){
        // /main 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }

    @GetMapping("/project")
    public String projectPage(/*Model model*/) { // 모델 주입 불필요
        // model.addAttribute("project",projectService.findAll()); // React에서 API로 데이터 로드
        // /project 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }


    @GetMapping("/task")
    public String taskPage(/*Model model*/){ // 모델 주입 불필요
        // model.addAttribute("task",taskService.findAll()); // React에서 API로 데이터 로드
        // /task 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String loginPage(){
        // /login 경로도 React 앱의 index.html로 포워딩
        // Spring Security의 .loginPage("/login") 설정과 연동되어,
        // 인증되지 않은 사용자가 보호된 리소스 접근 시 이 경로로 리다이렉트되고,
        // 결과적으로 React 앱의 로그인 컴포넌트가 로드됩니다.
        return "forward:/index.html";
    }

    @GetMapping("/register")
    public String registerPage(){
        // /register 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }


    @GetMapping("/chat")
    public String chatPage(/*Authentication authentication, Model model*/) { // 모델 및 인증 주입 불필요
        // if (authentication != null && authentication.isAuthenticated()) {
        //     System.out.println("Principal name: " + authentication.getName());
        //     model.addAttribute("loginUserId", authentication.getName()); // React에서 API로 사용자 정보 로드
        // }
        // else {
        //     model.addAttribute("loginUserId", "anonymous");
        // }
        // /chat 경로도 React 앱의 index.html로 포워딩
        return "forward:/index.html";
    }
}
