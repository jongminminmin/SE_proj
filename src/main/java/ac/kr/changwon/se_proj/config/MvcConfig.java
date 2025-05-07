package ac.kr.changwon.se_proj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        //react라우트 부분 ->
        //        registry.addViewController("/{spring:[\\w-]+}")
//                .setViewName("forward:/index.html");
//        registry.addViewController("/*/{spring:[\\w-]+}")
//                .setViewName("forward:/index.html");

        // 각각의 경로에 대해, 대응하는 HTML로 포워드
        registry.addViewController("/login")
                .setViewName("forward:/login.html");
        registry.addViewController("/register")
                .setViewName("forward:/register.html");
        registry.addViewController("/project")
                .setViewName("forward:/project.html");
        registry.addViewController("/task")
                .setViewName("forward:/task.html");
        registry.addViewController("/chat")
                .setViewName("forward:/chat.html");
        registry.addViewController("/simpleChat")
                .setViewName("forward:/simpleChat.html");

    }
}
